package app.munny.nativeapp

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

// Paleta Matrix premium: contraste suficiente para valores e controles financeiros.
val Sage = Color(0xFF67E9A5)
val SageSoft = Color(0xFF15372A)
val Canvas = Color(0xFF091410)
val Ink = Color(0xFFF1F8F1)
val Muted = Color(0xFFB3C5BA)
val Line = Color(0xFF365149)
val Coral = Color(0xFFFF897B)
val Lilac = Color(0xFF24352F)
val MatrixOrange = Color(0xFFFFA858)
val MatrixGray = Color(0xFF83918B)
val Panel = Color(0xFF17271F)
val LocalMoneyVisible = compositionLocalOf { true }

@Composable fun MunnyTheme(content:@Composable ()->Unit) {
    val scheme=darkColorScheme(primary=Sage,onPrimary=Color(0xFF072215),primaryContainer=SageSoft,
        onPrimaryContainer=Ink,background=Canvas,onBackground=Ink,surface=Panel,onSurface=Ink,
        surfaceVariant=Color(0xFF22352C),onSurfaceVariant=Muted,error=Coral,outline=Line,
        secondary=MatrixOrange,onSecondary=Color(0xFF1F1000))
    MaterialTheme(colorScheme=scheme,typography=Typography(
        headlineMedium=TextStyle(fontSize=29.sp,fontWeight=FontWeight.Bold),
        headlineSmall=TextStyle(fontSize=23.sp,fontWeight=FontWeight.Bold),
        titleMedium=TextStyle(fontSize=17.sp,fontWeight=FontWeight.Bold),
        bodyMedium=TextStyle(fontSize=14.sp),
        labelSmall=TextStyle(fontSize=11.sp)
    ),content=content)
}
private val brl=NumberFormat.getCurrencyInstance(Locale("pt","BR"))
fun money(cents:Long):String = synchronized(brl){brl.format(BigDecimal.valueOf(cents,2))}
@Composable fun safeMoney(cents:Long):String = if(LocalMoneyVisible.current) money(cents) else "R$ ••••••"
fun parseMoney(input:String):Long {
    val normalized=input.trim().replace("R$","").replace(" ","").replace(".","").replace(",",".")
    require(normalized.isNotBlank()) {"Informe o valor."}
    return BigDecimal(normalized).setScale(2,RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
}
