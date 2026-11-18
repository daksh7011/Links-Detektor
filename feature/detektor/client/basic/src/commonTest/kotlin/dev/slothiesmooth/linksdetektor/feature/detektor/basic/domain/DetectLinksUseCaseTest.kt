package dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain

import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.toDetectedLink
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectLinksUseCase
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectionResult
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.LibraryLinkEngine
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.LinkEngine
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DetectLinksUseCaseTest {
    private val text = "see https://example.com/a?b=1#c and http://user:pw@Sub.Example.org:8080/x, ok"
    private val noOptions: ImmutableSet<LinksDetektorOptions> = persistentSetOf()

    @Test
    fun `links come back in order with ranges that cut the original out of the text`() = runTest {
        val useCase = DetectLinksUseCase(LibraryLinkEngine(), StandardTestDispatcher(testScheduler))
        val result = assertIs<DetectionResult.Success>(useCase(text, noOptions))
        assertEquals(2, result.links.size)
        assertEquals(persistentListOf(0, 1), result.links.map { it.id }.toImmutableList())
        result.links.forEach { link ->
            assertEquals(link.original.full, text.substring(link.start, link.endExclusive))
        }
        assertTrue(result.links[0].endExclusive <= result.links[1].start)
        val first = result.links[0].original
        assertEquals("example.com", first.host)
        assertEquals("/a", first.path)
        assertEquals("?b=1", first.query)
        assertEquals("#c", first.fragment)
        assertEquals("sub.example.org", assertNotNull(result.links[1].normalized).host)
    }

    @Test
    fun `a stray percent sign keeps the full source span highlighted`() = runTest {
        val useCase = DetectLinksUseCase(LibraryLinkEngine(), StandardTestDispatcher(testScheduler))
        val result = assertIs<DetectionResult.Success>(useCase("see http://exa%mple.com/x ok", noOptions))
        val link = result.links.single()
        assertEquals(4, link.start)
        assertEquals(25, link.endExclusive)
    }

    @Test
    fun `blank text succeeds with no links and never calls the engine`() = runTest {
        var calls = 0
        val engine = LinkEngine { _, _ ->
            calls++
            persistentListOf()
        }
        val useCase = DetectLinksUseCase(engine, StandardTestDispatcher(testScheduler))
        assertEquals(DetectionResult.Success("  \n", noOptions, persistentListOf()), useCase("  \n", noOptions))
        assertEquals(0, calls)
    }

    @Test
    fun `a throwing engine becomes Failure`() = runTest {
        val engine = LinkEngine { _, _ -> error("boom") }
        val useCase = DetectLinksUseCase(engine, StandardTestDispatcher(testScheduler))
        assertEquals(DetectionResult.Failure("That text could not be checked."), useCase("x.com", noOptions))
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        val engine = LinkEngine { _, _ -> throw CancellationException("stop") }
        val useCase = DetectLinksUseCase(engine, StandardTestDispatcher(testScheduler))
        assertFailsWith<CancellationException> { useCase("x.com", noOptions) }
    }

    @Test
    fun `a link that cannot be normalized keeps its original parts`() {
        val url = LibraryLinkEngine().detect("https://example.com", noOptions).first()
        val link = url.toDetectedLink(id = 0) { error("cannot normalize") }
        assertNull(link.normalized)
        assertEquals("example.com", link.original.host)
    }
}
