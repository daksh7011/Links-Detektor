package dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase

import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.toDetectedLink
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Marks the dispatcher link detection runs on. */
@Qualifier
annotation class DetectionDispatcher

@ContributesTo(AppScope::class)
interface DetektorBindings {
    @Provides
    @DetectionDispatcher
    fun provideDetectionDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

/** What one detection produced. */
sealed interface DetectionResult {
    data class Success(
        val text: String,
        val options: ImmutableSet<LinksDetektorOptions>,
        val links: ImmutableList<DetectedLink>,
    ) : DetectionResult

    data class Failure(val message: String) : DetectionResult
}

private const val FAILURE_MESSAGE = "That text could not be checked."

/** Finds the links in a text off the main thread and maps them to what the screen shows. */
@Inject
class DetectLinksUseCase(
    private val engine: LinkEngine,
    @DetectionDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(text: String, options: ImmutableSet<LinksDetektorOptions>): DetectionResult {
        if (text.isBlank()) return DetectionResult.Success(text, options, persistentListOf())
        return runCatching {
            withContext(dispatcher) {
                engine.detect(text, options)
                    .mapIndexed { index, url -> url.toDetectedLink(index) }
                    .toImmutableList()
            }
        }.fold(
            onSuccess = { links -> DetectionResult.Success(text, options, links) },
            onFailure = { error -> if (error is CancellationException) throw error else DetectionResult.Failure(FAILURE_MESSAGE) },
        )
    }
}
