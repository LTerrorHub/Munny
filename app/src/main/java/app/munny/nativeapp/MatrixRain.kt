package app.munny.nativeapp

import android.graphics.Paint
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.floor

/** Camada decorativa atrás das superfícies sólidas. ~12 fps; sem acesso à rede. */
@Composable fun MatrixRainBackground(enabled:Boolean, modifier:Modifier=Modifier) {
    val context=LocalContext.current
    val reducedMotion=remember { runCatching {
        Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f)==0f
    }.getOrDefault(false) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(enabled,reducedMotion) {
        if (enabled && !reducedMotion) while(isActive) { delay(84);frame=(frame+1)%100000 }
    }
    if(!enabled)return
    val paint=remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface=android.graphics.Typeface.create("monospace",android.graphics.Typeface.NORMAL)
        textAlign=Paint.Align.CENTER
    } }
    val alphabet=remember { "01アイウエカキクケサシスセタチツナニヌネハヒフヘマミムメヤユヨラリルレ" }
    Canvas(modifier.fillMaxSize()) {
        val w=size.width;val h=size.height
        if(w<=0f||h<=0f)return@Canvas
        val spacing=32f*density
        val cols=(w/spacing).toInt().coerceIn(4,28)
        val fallSpace=h+24f*density
        paint.textSize=12f*density
        for (col in 0 until cols) {
            val x=(col+.5f)*w/cols
            val speed=2.1f+(col*7%9)*.26f
            val head=(frame*speed*3.7f*density+col*71f*density)%fallSpace
            for (trail in 0..10) {
                val y=head-trail*23f*density
                if(y<0||y>h)continue
                val ratio=y/h
                val alpha=(40+((11-trail)*5)).coerceAtMost(104)
                val color=when {
                    ratio<.50f -> android.graphics.Color.argb(alpha,86,241,151)
                    ratio<.81f -> android.graphics.Color.argb(alpha,250,151,67)
                    else -> android.graphics.Color.argb(alpha,144,153,148)
                }
                paint.color=color
                val symbolIndex=((col*17+trail*13+floor(frame.toDouble()/4).toInt())%alphabet.length)
                drawContext.canvas.nativeCanvas.drawText(alphabet[symbolIndex].toString(),x,y,paint)
            }
        }
    }
}
