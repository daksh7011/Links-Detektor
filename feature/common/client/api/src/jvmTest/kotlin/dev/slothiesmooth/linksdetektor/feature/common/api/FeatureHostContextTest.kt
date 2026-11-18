package dev.slothiesmooth.linksdetektor.feature.common.api

import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
internal data object Start

@Serializable
internal data object ScreenA

@Serializable
internal data object ScreenB

@Serializable
internal data object ScreenC

@OptIn(ExperimentalTestApi::class)
class FeatureHostContextTest {
    private fun NavHostController.stack(): ImmutableList<String?> =
        currentBackStack.value.mapNotNull { entry -> entry.destination.route }.map(::routeNameOfPattern).toImmutableList()

    private fun routeNameOfPattern(pattern: String): String? = pattern.substringBefore('/').substringBefore('?')

    @Test
    fun `reset stack leaves exactly the new screen, however long the stack was`() = runComposeUiTest {
        lateinit var navController: NavHostController
        lateinit var context: FeatureHostContext
        setContent {
            navController = rememberNavController()
            context = remember { FeatureHostContext(persistentListOf(), navController) }
            NavHost(navController, startDestination = Start) {
                composable<Start> {}
                composable<ScreenA> {}
                composable<ScreenB> {}
                composable<ScreenC> {}
            }
        }
        waitForIdle()
        val a = routeNameOf<ScreenA>()
        val b = routeNameOf<ScreenB>()
        val c = routeNameOf<ScreenC>()

        runOnIdle { context.resetStack(ScreenA) }
        waitForIdle()
        assertEquals(persistentListOf(a), navController.stack())

        runOnIdle { context.resetStack(ScreenB) }
        waitForIdle()
        assertEquals(persistentListOf(b), navController.stack())

        runOnIdle { context.setDestination(ScreenC) }
        waitForIdle()
        assertEquals(persistentListOf(b, c), navController.stack())

        runOnIdle { context.resetStack(ScreenA) }
        waitForIdle()
        assertEquals(persistentListOf(a), navController.stack())
    }
}
