package dev.slothiesmooth.linksdetektor.client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.compose.rememberNavController
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.LdTheme
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureHost
import dev.slothiesmooth.linksdetektor.feature.common.api.FeatureHostContext
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.collections.immutable.toImmutableSet

/** The app root: hosts the ordered feature providers on one navigation controller, starting at the detector. */
@Composable
fun App(graph: BaseAppGraph) {
    CompositionLocalProvider(LocalMetroViewModelFactory provides graph.metroViewModelFactory) {
        LdTheme {
            val navController = rememberNavController()
            // Metro's plain Set is read once, here, and converted at once.
            val features = remember(graph) { graph.features.toImmutableSet() }
            val context = remember(graph, navController) {
                FeatureHostContext(sortFeatures(features, graph.featureOrder), navController)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .semantics { contentDescription = APP_TITLE },
            ) {
                FeatureHost(context) { graph.appStartRoute.route.takeIf { features.isNotEmpty() } }
            }
        }
    }
}
