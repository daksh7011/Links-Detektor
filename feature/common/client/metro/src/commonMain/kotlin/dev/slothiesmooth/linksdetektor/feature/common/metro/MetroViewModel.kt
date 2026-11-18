package dev.slothiesmooth.linksdetektor.feature.common.metro

import androidx.compose.runtime.Composable
import dev.slothiesmooth.linksdetektor.shared.presentation.viewmodel.BaseViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** The view model of a feature screen, created by the app graph's view-model factory (see `LocalMetroViewModelFactory`). */
@Composable
inline fun <reified T : BaseViewModel> metroFeatureViewModel(): T = metroViewModel<T>().also { it.bind() }
