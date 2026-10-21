package app.munny.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

/** Mesma competência e mesmo status usados no cálculo de consumo do Munny. */
internal fun categoryConsumption(data:MunnyDocument, month:YearMonth):Map<String,Long> {
    val period=month.toString()
    val occurred=FinanceMath.occurrences(data.movements(),data.exceptions(),month.atEndOfMonth().toString())
    return occurred.asSequence()
        .filter { it.date.startsWith(period) && (it.kind=="card" || (it.kind=="expense" && it.status=="paid")) }
        .groupBy { it.category.ifBlank { "Outros" } }
        .mapValues { (_,rows)->rows.sumOf { it.amount } }
        .filterValues { it>0L }
        .toList().sortedByDescending { it.second }.toMap()
}

private val monthLabels=Locale("pt","BR")
private val chartColors=listOf(Color(0xFF16AD88),Color(0xFFF3A24F),Color(0xFF52B4E6),Color(0xFFAD91DE),Color(0xFFEB8193),Color(0xFF81949B),Color(0xFF62C8A4))

@Composable fun MonthlyFinancePanel(data:MunnyDocument) {
    val visible=LocalMoneyVisible.current
    var range by remember { mutableIntStateOf(6) }
    val periods=remember(range) {(range-1 downTo 0).map{YearMonth.now().minusMonths(it.toLong())}}
    val values=remember(data,periods){periods.map{FinanceMath.month(data.movements(),data.exceptions(),it.toString())}}
    val maxVal=max(1L,values.maxOf{max(it.income,it.consumption)})
    val incomeColor=Sage
    val consumptionColor=Color(0xFF54A9EC)
    val gridColor=Line
    SheetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Fluxo de caixa",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                Text("Recebimentos × consumo por mês",color=Muted,style=MaterialTheme.typography.labelSmall)
            }
            listOf(3,6).forEach { months ->
                FilterChip(selected=range==months,onClick={range=months},
                    modifier=Modifier.padding(start=4.dp),
                    label={Text("${months}m",style=MaterialTheme.typography.labelSmall)})
            }
        }
        Spacer(Modifier.height(16.dp))
        if(!visible) {
            Text("Gráfico oculto pelo modo privacidade.",color=Muted)
        } else if(values.all{it.income==0L&&it.consumption==0L}) {
            Text("Registre lançamentos para visualizar o fluxo financeiro.",color=Muted)
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(17.dp),verticalAlignment=Alignment.CenterVertically) {
                LegendDot(incomeColor,"Entradas")
                LegendDot(consumptionColor,"Gastos")
            }
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(177.dp)) {
                val left=6.dp.toPx()
                val right=size.width-6.dp.toPx()
                val bottom=size.height-12.dp.toPx()
                val top=10.dp.toPx()
                val usable=bottom-top
                for(i in 0..3){
                    val y=top+usable*i/3
                    drawLine(gridColor.copy(alpha=.48f),Offset(left,y),Offset(right,y),strokeWidth=1.dp.toPx())
                }
                fun xy(index:Int,value:Long):Offset {
                    val x=left+(right-left)*(index.toFloat()/((values.size-1).coerceAtLeast(1)))
                    val y=bottom-(value.toFloat()/maxVal.toFloat())*usable
                    return Offset(x,y)
                }
                fun curve(entries:List<Long>):Path {
                    val points=entries.mapIndexed{index,value->xy(index,value)}
                    val path=Path()
                    if(points.isNotEmpty()) {
                        path.moveTo(points[0].x,points[0].y)
                        for(i in 1 until points.size){
                            val prev=points[i-1];val next=points[i]
                            val mid=(prev.x+next.x)/2f
                            path.cubicTo(mid,prev.y,mid,next.y,next.x,next.y)
                        }
                    }
                    return path
                }
                fun plot(entries:List<Long>,color:Color,fill:Boolean) {
                    val points=entries.mapIndexed{index,value->xy(index,value)}
                    if(points.isEmpty())return
                    val path=curve(entries)
                    if(fill) {
                        val area=Path().apply {
                            addPath(path)
                            lineTo(points.last().x,bottom)
                            lineTo(points.first().x,bottom)
                            close()
                        }
                        drawPath(area,brush=Brush.verticalGradient(
                            listOf(color.copy(alpha=.23f),color.copy(alpha=.015f)),startY=top,endY=bottom))
                    }
                    drawPath(path,color=color,style=Stroke(width=2.9.dp.toPx(),cap=StrokeCap.Round))
                    points.forEach{pt->drawCircle(color,3.dp.toPx(),pt)}
                }
                plot(values.map{it.income},incomeColor,true)
                plot(values.map{it.consumption},consumptionColor,false)
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                periods.forEach{period->Text(
                    period.month.getDisplayName(TextStyle.SHORT,monthLabels).replaceFirstChar{it.uppercase()},
                    color=Muted,style=MaterialTheme.typography.labelSmall)}
            }
            Spacer(Modifier.height(7.dp))
            val current=values.last()
            Text("Neste mês: ${safeMoney(current.income)} recebidos · ${safeMoney(current.consumption)} consumidos",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun LegendDot(color:Color,label:String){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
        Surface(color=color,shape=RoundedCornerShape(50),modifier=Modifier.size(9.dp)){}
        Text(label,color=Muted,style=MaterialTheme.typography.labelSmall)
    }
}

@Composable fun CategorySpendingPanel(data:MunnyDocument) {
    val visible=LocalMoneyVisible.current
    var relativeMonth by remember{mutableIntStateOf(0)}
    val period=remember(relativeMonth) { YearMonth.now().plusMonths(relativeMonth.toLong()) }
    val groups=remember(data,period){categoryConsumption(data,period)}
    val total=groups.values.sum()
    var selected by remember(period,data) {mutableStateOf<String?>(null)}
    val movements=remember(data,period){FinanceMath.occurrences(data.movements(),data.exceptions(),period.atEndOfMonth().toString())
        .filter{it.date.startsWith(period.toString()) && (it.kind=="card"||(it.kind=="expense"&&it.status=="paid"))}
        .sortedByDescending{it.date}}
    val donutTrack=Line
    SheetCard(Modifier.fillMaxWidth()) {
        Text("Gastos por categoria",fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
            IconButton(onClick={relativeMonth=(relativeMonth-1).coerceAtLeast(-24)}){
                Icon(Icons.Default.ArrowBackIosNew,"Mês anterior",tint=Sage)
            }
            Text("${period.month.getDisplayName(TextStyle.FULL,monthLabels).replaceFirstChar{it.uppercase()}} ${period.year}",fontWeight=FontWeight.SemiBold)
            IconButton(onClick={relativeMonth=(relativeMonth+1).coerceAtMost(0)},enabled=relativeMonth<0){
                Icon(Icons.Default.ArrowForwardIos,"Próximo mês",tint=Sage)
            }
        }
        when {
            !visible -> Text("Detalhamento oculto. Toque no olho para visualizar.",color=Muted)
            groups.isEmpty() -> Text("Sem despesas confirmadas ou compras no cartão neste período.",color=Muted)
            else -> {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(17.dp)){
                    Canvas(Modifier.size(118.dp)) {
                        val width=18.dp.toPx()
                        drawArc(donutTrack,0f,360f,false,style=Stroke(width=width))
                        var start=-90f
                        groups.entries.forEachIndexed{index,(_,value)->
                            val sweep=360f*(value.toDouble()/total).toFloat()
                            drawArc(chartColors[index%chartColors.size],start,sweep,false,
                                style=Stroke(width=width,cap=StrokeCap.Butt))
                            start+=sweep
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Consumo do mês",color=Muted,style=MaterialTheme.typography.labelSmall)
                        Text(safeMoney(total),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                        Text("Compra no cartão aparece aqui antes do pagamento da fatura.",color=Muted,style=MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.height(9.dp))
                groups.entries.forEachIndexed{ix,(category,value)->
                    val pct=100.0*value/total
                    Surface(color=if(selected==category)SageSoft else Color.Transparent,
                        shape=RoundedCornerShape(10.dp),modifier=Modifier.fillMaxWidth().clickable {selected=if(selected==category)null else category}) {
                        Row(Modifier.fillMaxWidth().padding(vertical=9.dp,horizontal=5.dp),verticalAlignment=Alignment.CenterVertically){
                            Surface(color=chartColors[ix%chartColors.size],shape=RoundedCornerShape(4.dp),modifier=Modifier.size(11.dp)){}
                            Spacer(Modifier.width(9.dp))
                            Text(category,Modifier.weight(1f),color=Ink)
                            Text("${String.format(Locale("pt","BR"),"%.1f",pct)}%",color=Muted,style=MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.width(10.dp))
                            Text(safeMoney(value),fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(if(selected==null)"Toque em uma categoria para detalhar os lançamentos" else "Lançamentos · $selected",color=Sage,style=MaterialTheme.typography.labelSmall)
                val detailed=if(selected==null) emptyList() else movements.filter{it.category.ifBlank {"Outros"}==selected}
                detailed.take(10).forEach { m->
                    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){
                            Text(m.title,color=Ink,fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodyMedium)
                            Text("${m.date} · ${if(m.kind=="card")"Cartão" else "Despesa paga"}",color=Muted,style=MaterialTheme.typography.labelSmall)
                        }
                        Text(safeMoney(m.amount),color=MatrixOrange,fontWeight=FontWeight.SemiBold)
                    }
                }
                if(detailed.size>10)Text("Mostrando os 10 lançamentos mais recentes (${detailed.size} no mês).",color=Muted,style=MaterialTheme.typography.labelSmall)
            }
        }
    }
}
