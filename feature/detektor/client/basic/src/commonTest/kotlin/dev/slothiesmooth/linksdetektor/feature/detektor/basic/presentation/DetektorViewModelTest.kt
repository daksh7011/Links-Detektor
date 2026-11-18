package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import app.cash.turbine.test
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import com.slothiesmooth.linksdetektor.LinksDetektorOptions.QUOTE_MATCH
import com.slothiesmooth.linksdetektor.LinksDetektorOptions.SINGLE_QUOTE_MATCH
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectLinksUseCase
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.LibraryLinkEngine
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.LinkEngine
import dev.slothiesmooth.linksdetektor.shared.presentation.state.UiState
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Holds every dispatched block until [release], so a detection can be kept in flight. */
private class GatedDispatcher : CoroutineDispatcher() {
    var queued: PersistentList<Runnable> = persistentListOf()
        private set

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queued = queued.adding(block)
    }

    fun release() {
        val batch = queued
        queued = persistentListOf()
        batch.forEach { it.run() }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DetektorViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(
        dispatcher: CoroutineDispatcher = StandardTestDispatcher(testScheduler),
        engine: LinkEngine = LibraryLinkEngine(),
    ) = DetektorViewModel(DetectLinksUseCase(engine, dispatcher))

    @Test
    fun `typing detects after the debounce, not before`() = runTest {
        val vm = vm()
        vm.onInputChange("go to https://example.com now")
        advanceTimeBy(INPUT_DEBOUNCE_MS - 1)
        runCurrent()
        assertTrue(vm.state.value.detecting)
        assertEquals(DetectionUi.Empty, (vm.state.value.result as UiState.Ready).value)
        advanceUntilIdle()
        val ready = (vm.state.value.result as UiState.Ready).value
        assertEquals("go to https://example.com now", ready.analyzedText)
        assertEquals(1, ready.links.size)
        assertFalse(vm.state.value.detecting)
    }

    @Test
    fun `a newer input discards the older result`() = runTest {
        var calls = 0
        val vm = vm { content, options ->
            calls++
            LibraryLinkEngine().detect(content, options)
        }
        vm.onInputChange("a.com")
        advanceTimeBy(100)
        vm.onInputChange("b.com and c.com")
        advanceUntilIdle()
        assertEquals(1, calls)
        val ready = (vm.state.value.result as UiState.Ready).value
        assertEquals("b.com and c.com", ready.analyzedText)
        ready.links.forEach { assertTrue(it.endExclusive <= ready.analyzedText.length) }
    }

    @Test
    fun `a newer input cancels the detection already in flight`() = runTest {
        val gate = GatedDispatcher()
        var ran: PersistentList<String> = persistentListOf()
        val vm = vm(
            engine = { content, options ->
                ran = ran.adding(content)
                LibraryLinkEngine().detect(content, options)
            },
            dispatcher = gate,
        )
        vm.onInputChange("a.com")
        advanceTimeBy(INPUT_DEBOUNCE_MS)
        runCurrent()
        assertEquals(1, gate.queued.size) // detection 1 is in flight, held at the gate
        vm.onInputChange("b.com and c.com")
        advanceTimeBy(INPUT_DEBOUNCE_MS)
        runCurrent()
        gate.release() // detection 1 was cancelled: it unwinds without running the engine, and detection 2 starts
        runCurrent()
        assertEquals(1, gate.queued.size)
        gate.release()
        advanceUntilIdle()
        assertEquals(persistentListOf("b.com and c.com"), ran)
        val ready = (vm.state.value.result as UiState.Ready).value
        assertEquals("b.com and c.com", ready.analyzedText)
        assertEquals(2, ready.links.size)
        assertFalse(vm.state.value.detecting)
    }

    @Test
    fun `late result while debouncing does not keep a stale selection`() = runTest {
        val gate = GatedDispatcher()
        val vm = vm(dispatcher = gate)
        vm.onInputChange("a.com b.com")
        advanceTimeBy(INPUT_DEBOUNCE_MS)
        runCurrent()
        vm.onInputChange("x.com y.com z.com") // newer input is now waiting out its debounce
        gate.release() // the older detection lands meanwhile
        runCurrent()
        vm.onSelectLink(1)
        assertEquals(1, vm.state.value.selectedId)
        advanceTimeBy(INPUT_DEBOUNCE_MS)
        runCurrent()
        gate.release()
        advanceUntilIdle()
        assertEquals("x.com y.com z.com", (vm.state.value.result as UiState.Ready).value.analyzedText)
        assertNull(vm.state.value.selectedId)
    }

    @Test
    fun `typing past the cap again changes nothing but the flag`() = runTest {
        val vm = vm()
        val base = "a.com " + "b".repeat(MAX_INPUT_CHARS)
        vm.onInputChange(base)
        advanceUntilIdle()
        vm.onSelectLink(0)
        vm.onInputChange(base + "c")
        runCurrent()
        assertFalse(vm.state.value.detecting)
        assertEquals(0, vm.state.value.selectedId)
        assertTrue(vm.state.value.truncated)
    }

    @Test
    fun `input over the cap is cut and flagged`() = runTest {
        val vm = vm()
        vm.onInputChange("a".repeat(MAX_INPUT_CHARS + 5))
        assertEquals(MAX_INPUT_CHARS, vm.state.value.input.length)
        assertTrue(vm.state.value.truncated)
        vm.onInputChange("short")
        assertFalse(vm.state.value.truncated)
    }

    @Test
    fun `the cut never splits a surrogate pair`() = runTest {
        val vm = vm()
        vm.onInputChange("a".repeat(MAX_INPUT_CHARS - 1) + "😀")
        assertEquals(MAX_INPUT_CHARS - 1, vm.state.value.input.length)
        assertFalse(vm.state.value.input.last().isHighSurrogate())
        assertTrue(vm.state.value.truncated)
    }

    @Test
    fun `toggling options flips the set and any combination detects`() = runTest {
        var seen: PersistentList<ImmutableSet<LinksDetektorOptions>> = persistentListOf()
        val vm = vm { content, options ->
            seen = seen.adding(options)
            LibraryLinkEngine().detect(content, options)
        }
        vm.onInputChange("\"http://a.com\" 'http://b.com'")
        advanceUntilIdle()
        vm.onToggleOption(QUOTE_MATCH)
        vm.onToggleOption(SINGLE_QUOTE_MATCH)
        advanceUntilIdle()
        assertEquals(persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH), vm.state.value.options)
        assertEquals(persistentSetOf(QUOTE_MATCH, SINGLE_QUOTE_MATCH), seen.last())
        val ready = (vm.state.value.result as UiState.Ready).value
        assertEquals(
            persistentListOf("http://a.com", "http://b.com"),
            ready.links.map { it.original.full }.toImmutableList(),
        )
        vm.onToggleOption(QUOTE_MATCH)
        vm.onToggleOption(SINGLE_QUOTE_MATCH)
        assertEquals(persistentSetOf(), vm.state.value.options)
    }

    @Test
    fun `a failure shows the error and retry recovers`() = runTest {
        var fail = true
        val vm = vm { content, options ->
            if (fail) error("boom") else LibraryLinkEngine().detect(content, options)
        }
        vm.state.test {
            skipItems(1)
            vm.onInputChange("example.com")
            advanceUntilIdle()
            assertEquals(UiState.Failed("That text could not be checked."), expectMostRecentItem().result)
            fail = false
            vm.onRetry()
            assertEquals(UiState.Loading, expectMostRecentItem().result)
            advanceUntilIdle()
            assertIs<UiState.Ready<DetectionUi>>(expectMostRecentItem().result)
        }
    }

    @Test
    fun `retry skips the debounce`() = runTest {
        var fail = true
        val vm = vm { content, options ->
            if (fail) error("boom") else LibraryLinkEngine().detect(content, options)
        }
        vm.onInputChange("example.com")
        advanceUntilIdle()
        fail = false
        vm.onRetry()
        runCurrent()
        assertIs<UiState.Ready<DetectionUi>>(vm.state.value.result)
        assertEquals(1, (vm.state.value.result as UiState.Ready).value.links.size)
    }

    @Test
    fun `selection follows the results`() = runTest {
        val vm = vm()
        vm.onInputChange("a.com b.com")
        advanceUntilIdle()
        vm.onSelectLink(1)
        assertEquals(1, vm.state.value.selected?.id)
        vm.onSelectLink(7)
        assertEquals(1, vm.state.value.selectedId)
        vm.onInputChange("a.com")
        assertNull(vm.state.value.selectedId)
    }

    @Test
    fun `clearing the input returns to empty at once`() = runTest {
        val vm = vm()
        vm.onInputChange("a.com")
        advanceUntilIdle()
        vm.onInputChange("")
        runCurrent()
        assertEquals(DetektorScreenState.Empty, screenStateOf(vm.state.value))
    }
}
