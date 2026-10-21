package app.munny.nativeapp

import android.app.Activity
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Paleta semântica: todos os cards e textos mudam junto com o tema. */
@Immutable data class MunnyPalette(
    val sage:Color, val sageSoft:Color, val canvas:Color, val ink:Color,
    val muted:Color, val line:Color, val coral:Color, val lilac:Color,
    val orange:Color, val gray:Color, val panel:Color
)
private val darkPalette=MunnyPalette(
    sage=Color(0xFF36DBAC),sageSoft=Color(0xFF123F36),canvas=Color(0xFF0A151A),
    ink=Color(0xFFF2F7F6),muted=Color(0xFFABC0C3),line=Color(0xFF34484D),
    coral=Color(0xFFFF899A),lilac=Color(0xFF25353A),
    orange=Color(0xFFFFA858),gray=Color(0xFF87999E),panel=Color(0xFF17252C))
private val lightPalette=MunnyPalette(
    sage=Color(0xFF087C62),sageSoft=Color(0xFFDDF4EB),canvas=Color(0xFFF6F8F7),
    ink=Color(0xFF14262A),muted=Color(0xFF596E71),line=Color(0xFFD9E4E1),
    coral=Color(0xFFB64256),lilac=Color(0xFFE8F0EE),
    orange=Color(0xFFC1782F),gray=Color(0xFF79898D),panel=Color(0xFFFFFFFF))
private val LocalMunnyPalette=staticCompositionLocalOf { darkPalette }
val Sage:Color @Composable get()=LocalMunnyPalette.current.sage
val SageSoft:Color @Composable get()=LocalMunnyPalette.current.sageSoft
val Canvas:Color @Composable get()=LocalMunnyPalette.current.canvas
val Ink:Color @Composable get()=LocalMunnyPalette.current.ink
val Muted:Color @Composable get()=LocalMunnyPalette.current.muted
val Line:Color @Composable get()=LocalMunnyPalette.current.line
val Coral:Color @Composable get()=LocalMunnyPalette.current.coral
val Lilac:Color @Composable get()=LocalMunnyPalette.current.lilac
val MatrixOrange:Color @Composable get()=LocalMunnyPalette.current.orange
val MatrixGray:Color @Composable get()=LocalMunnyPalette.current.gray
val Panel:Color @Composable get()=LocalMunnyPalette.current.panel
val LocalMoneyVisible=compositionLocalOf { true }

/** system | light | dark; respeita a configuração nativa e mantém barras legíveis. */
@Composable fun MunnyTheme(mode:String="system", content:@Composable ()->Unit) {
    val isDark=when(mode) {"light"->false;"dark"->true;else->isSystemInDarkTheme()}
    val p=if(isDark)darkPalette else lightPalette
    val colors=if(isDark) darkColorScheme(
        primary=p.sage,onPrimary=Color(0xFF05241D),primaryContainer=p.sageSoft,
        onPrimaryContainer=p.ink,background=p.canvas,onBackground=p.ink,
        surface=p.panel,onSurface=p.ink,surfaceVariant=Color(0xFF24363C),
        onSurfaceVariant=p.muted,outline=p.line,error=p.coral,secondary=p.orange
    ) else lightColorScheme(
        primary=p.sage,onPrimary=Color.White,primaryContainer=p.sageSoft,
        onPrimaryContainer=Color(0xFF124736),background=p.canvas,onBackground=p.ink,
        surface=p.panel,onSurface=p.ink,surfaceVariant=Color(0xFFEDF3F1),
        onSurfaceVariant=p.muted,outline=p.line,error=p.coral,secondary=p.orange
    )
    val view=LocalView.current
    if(!view.isInEditMode) SideEffect {
        (view.context as? Activity)?.window?.apply {
            statusBarColor=p.canvas.toArgb()
            navigationBarColor=p.canvas.toArgb()
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility=if(isDark) 0 else
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
    CompositionLocalProvider(LocalMunnyPalette provides p) {
        MaterialTheme(colorScheme=colors,typography=Typography(
            headlineMedium=TextStyle(fontSize=28.sp,fontWeight=FontWeight.Bold),
            headlineSmall=TextStyle(fontSize=23.sp,fontWeight=FontWeight.Bold),
            titleMedium=TextStyle(fontSize=17.sp,fontWeight=FontWeight.Bold),
            bodyMedium=TextStyle(fontSize=14.sp),
            labelSmall=TextStyle(fontSize=11.sp)
        ),content=content)
    }
}
private val brl=NumberFormat.getCurrencyInstance(Locale("pt","BR"))
fun money(cents:Long):String = synchronized(brl){brl.format(BigDecimal.valueOf(cents,2))}
@Composable fun safeMoney(cents:Long):String = if(LocalMoneyVisible.current)money(cents)else "R$ ••••••"
fun parseMoney(input:String):Long {
    val normalized=input.trim().replace("R$","").replace(" ","").replace(".","").replace(",",".")
    require(normalized.isNotBlank()) {"Informe o valor."}
    return BigDecimal(normalized).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
}
