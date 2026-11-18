package dev.slothiesmooth.linksdetektor.feature.common.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Common provider plumbing: an action channel a screen can post to and the provider handles with a [FeatureContext]. */
abstract class BaseFeatureProvider : FeatureProvider {
    private val actionFlow = Channel<Action>(Channel.UNLIMITED)

    @Composable
    final override fun provideContent(context: FeatureContext, content: @Composable () -> Unit) {
        LaunchedEffect(context) { actionFlow.receiveAsFlow().collect { action -> onReceiveAction(action, context) } }
        onProvideContent(context, content)
    }

    @Composable
    abstract fun onProvideContent(context: FeatureContext, content: @Composable () -> Unit)

    final override fun provideNavigation(context: FeatureContext, builder: NavGraphBuilder) {
        onProvideNavigation(context, builder)
    }

    open fun onProvideNavigation(context: FeatureContext, builder: NavGraphBuilder) = Unit

    protected open suspend fun onReceiveAction(action: Action, context: FeatureContext) = Unit

    /** Enqueues an action for [onReceiveAction]; always accepted and held until a collector is active (unbounded buffer). */
    protected fun onSendAction(action: Action): Boolean = actionFlow.trySend(action).isSuccess

    /** Marker for the private action types a provider sends itself. */
    interface Action
}
