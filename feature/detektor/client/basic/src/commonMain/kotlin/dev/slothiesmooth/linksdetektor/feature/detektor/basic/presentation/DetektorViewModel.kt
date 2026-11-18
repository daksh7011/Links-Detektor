package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectLinksUseCase
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase.DetectionResult
import dev.slothiesmooth.linksdetektor.shared.presentation.state.UiState
import dev.slothiesmooth.linksdetektor.shared.presentation.viewmodel.BaseViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One thing to check; [attempt] makes a retry a new request, [immediate] skips the debounce. */
private data class Request(
    val text: String,
    val options: ImmutableSet<LinksDetektorOptions>,
    val attempt: Int = 0,
    val immediate: Boolean = false,
)

private const val LAST_CHAR = 1

/** Holds the text, the options and the detection result of the detector screen. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class DetektorViewModel(private val detectLinks: DetectLinksUseCase) : BaseViewModel() {
    private val mutableState = MutableStateFlow(DetektorUiState())
    val state: StateFlow<DetektorUiState> = mutableState.asStateFlow()

    private val requests = MutableStateFlow(Request("", persistentSetOf()))

    init {
        viewModelScope.launch { detectOnRequests() }
    }

    @OptIn(FlowPreview::class)
    private suspend fun detectOnRequests() {
        requests
            .debounce { if (it.text.isEmpty() || it.immediate) 0L else INPUT_DEBOUNCE_MS }
            .collectLatest { request -> detect(request) }
    }

    private suspend fun detect(request: Request) {
        if (mutableState.value.result is UiState.Failed) {
            mutableState.update { it.copy(result = UiState.Loading) }
        }
        val outcome = when (val result = detectLinks(request.text, request.options)) {
            is DetectionResult.Success -> UiState.Ready(DetectionUi(result.text, result.links))
            is DetectionResult.Failure -> UiState.Failed(result.message)
        }
        mutableState.update { current ->
            // A newer input is already waiting out its debounce: keep showing that work as in progress.
            val latest = current.input == request.text && current.options == request.options
            current.copy(result = outcome, detecting = current.detecting && !latest)
        }
    }

    fun onInputChange(text: String) {
        val kept = capped(text)
        mutableState.update {
            it.copy(input = kept, truncated = kept.length < text.length, detecting = true, selectedId = null)
        }
        requests.update { it.copy(text = kept, immediate = false) }
    }

    fun onToggleOption(option: LinksDetektorOptions) {
        val current = mutableState.value.options.toPersistentSet()
        val options = if (option in current) current.removing(option) else current.adding(option)
        mutableState.update { it.copy(options = options, detecting = true, selectedId = null) }
        requests.update { it.copy(options = options, immediate = false) }
    }

    fun onSelectLink(id: Int?) {
        mutableState.update { current ->
            if (id == null || (current.result as? UiState.Ready)?.value?.links?.any { it.id == id } == true) {
                current.copy(selectedId = id)
            } else {
                current
            }
        }
    }

    /** Checks the same text again at once, without waiting out the debounce. */
    fun onRetry() {
        mutableState.update { it.copy(result = UiState.Loading, detecting = true) }
        requests.update { it.copy(attempt = it.attempt + 1, immediate = true) }
    }

    private fun capped(text: String): String {
        if (text.length <= MAX_INPUT_CHARS) return text
        val cut = text.take(MAX_INPUT_CHARS)
        return if (cut.last().isHighSurrogate()) cut.dropLast(LAST_CHAR) else cut
    }
}
