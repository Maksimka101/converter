package com.zemlianikin.currency.ui

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Вид в духе Material 3 Expressive на стабильном material3 1.4.0: цвета из обоев (Android 12+),
 * крупные скругления, пружинная анимация [Motion]. Настоящие `MaterialExpressiveTheme` и expressive-типографика
 * в 1.4.0 закрыты (`internal`), они открываются в material3 1.5.0-alpha.
 */
@Composable
fun CurrencyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = platformColorScheme(isSystemInDarkTheme()),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}

/** Пружины как в expressive-motion: пространственные с лёгким отскоком, для цвета — без отскока. */
object Motion {
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 400f)
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 1400f)
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
}
