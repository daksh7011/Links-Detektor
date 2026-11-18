package dev.slothiesmooth.linksdetektor.client

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelAssistedFactory
import kotlin.reflect.KClass

// The plain Map types below are Metro's multibinding types: the documented exception to the immutable-collections rule.
private typealias ManualFactories = Map<KClass<out ManualViewModelAssistedFactory>, () -> ManualViewModelAssistedFactory>

/** The one view-model factory of the app; view models contribute themselves into the maps it receives. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LdViewModelFactory(
    override val viewModelProviders: Map<KClass<out ViewModel>, () -> ViewModel>,
    override val assistedFactoryProviders: Map<KClass<out ViewModel>, () -> ViewModelAssistedFactory>,
    override val manualAssistedFactoryProviders: ManualFactories,
) : MetroViewModelFactory()
