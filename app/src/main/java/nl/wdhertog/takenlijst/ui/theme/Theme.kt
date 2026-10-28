package nl.wdhertog.takenlijst.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import nl.wdhertog.takenlijst.R
import nl.wdhertog.takenlijst.data.TaskGroup

private fun figtree(weight: Int) = Font(
    R.font.figtree,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Figtree = FontFamily(figtree(400), figtree(500), figtree(600), figtree(700), figtree(800))

/** Kleuren buiten Material: per groep en voor "te laat". */
@Immutable
data class AppColors(
    val card: Color,
    val line: Color,
    val muted: Color,
    val groupBg: Map<TaskGroup, Color>,
    val groupFg: Map<TaskGroup, Color>,
    val lateBg: Color,
    val lateFg: Color,
    val lateRing: Color,
    val hero: Color,
    val onHero: Color,
    val heroTrack: Color,
)

private val LightApp = AppColors(
    card = Color(0xFFFFFFFF),
    line = Color(0xFFE4E2DC),
    muted = Color(0xFF5E6660),
    groupBg = mapOf(TaskGroup.HOME to Color(0xFFE4ECF7), TaskGroup.GARDEN to Color(0xFFE3EFE7), TaskGroup.WORK to Color(0xFFEFE7F5)),
    groupFg = mapOf(TaskGroup.HOME to Color(0xFF244E85), TaskGroup.GARDEN to Color(0xFF245A40), TaskGroup.WORK to Color(0xFF663F83)),
    lateBg = Color(0xFFFBEBDF),
    lateFg = Color(0xFFA04A1C),
    lateRing = Color(0xFFF3C9AE),
    hero = Color(0xFF2F6B4F),
    onHero = Color(0xFFFFFFFF),
    heroTrack = Color(0xFF5E9479),
)

private val DarkApp = AppColors(
    card = Color(0xFF1E211F),
    line = Color(0xFF2E322F),
    muted = Color(0xFFA9B1AB),
    groupBg = mapOf(TaskGroup.HOME to Color(0xFF22324A), TaskGroup.GARDEN to Color(0xFF1F3A2C), TaskGroup.WORK to Color(0xFF3A2A47)),
    groupFg = mapOf(TaskGroup.HOME to Color(0xFFB7CDF0), TaskGroup.GARDEN to Color(0xFFA8D8BC), TaskGroup.WORK to Color(0xFFDCC3F0)),
    lateBg = Color(0xFF4A2A17),
    lateFg = Color(0xFFF3B48C),
    lateRing = Color(0xFF7A4526),
    hero = Color(0xFF1F4A36),
    onHero = Color(0xFFEAF5EE),
    heroTrack = Color(0xFF2F6B4F),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF2F6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E6DA),
    onPrimaryContainer = Color(0xFF0F2E20),
    secondaryContainer = Color(0xFFD3E6DA),
    onSecondaryContainer = Color(0xFF0F2E20),
    background = Color(0xFFF6F5F1),
    onBackground = Color(0xFF1C1F1D),
    surface = Color(0xFFF6F5F1),
    onSurface = Color(0xFF1C1F1D),
    onSurfaceVariant = Color(0xFF5E6660),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFEAE8E2),
    outline = Color(0xFF9AA39C),
    outlineVariant = Color(0xFFDAD7CF),
    error = Color(0xFFA04A1C),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF8FCFAE),
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF235A40),
    onPrimaryContainer = Color(0xFFD3E6DA),
    secondaryContainer = Color(0xFF235A40),
    onSecondaryContainer = Color(0xFFD3E6DA),
    background = Color(0xFF121412),
    onBackground = Color(0xFFE6E8E5),
    surface = Color(0xFF121412),
    onSurface = Color(0xFFE6E8E5),
    onSurfaceVariant = Color(0xFFA9B1AB),
    surfaceContainer = Color(0xFF1E211F),
    surfaceContainerLow = Color(0xFF1E211F),
    surfaceContainerHigh = Color(0xFF252926),
    surfaceContainerHighest = Color(0xFF2E322F),
    outline = Color(0xFF6F7871),
    outlineVariant = Color(0xFF3A3F3B),
    error = Color(0xFFF3B48C),
)

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = Figtree)
    return Typography(
        displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f().copy(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.f().copy(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp),
        headlineSmall = base.headlineSmall.f().copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.f().copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = base.titleSmall.f().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.f(),
        bodyMedium = base.bodyMedium.f(),
        bodySmall = base.bodySmall.f().copy(fontSize = 13.sp),
        labelLarge = base.labelLarge.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelMedium = base.labelMedium.f().copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
        labelSmall = base.labelSmall.f().copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.8.sp),
    )
}

val LocalAppColors = staticCompositionLocalOf { LightApp }

object App {
    val colors: AppColors @Composable get() = LocalAppColors.current
}

@Composable
fun TakenTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkApp else LightApp) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = typography(),
            content = content,
        )
    }
}
