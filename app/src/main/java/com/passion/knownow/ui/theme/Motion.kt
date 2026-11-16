package com.passion.knownow.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object KnowNowMotion {
    const val Micro = 120
    const val Short = 180
    const val Medium = 260
    const val Container = 380

    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Decelerate = CubicBezierEasing(0f, 0f, 0f, 1f)

    fun <T> emphasized(duration: Int = Container): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = Emphasized)

    fun <T> standard(duration: Int = Medium): FiniteAnimationSpec<T> =
        tween(durationMillis = duration, easing = Standard)

    fun <T> springy(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
}
