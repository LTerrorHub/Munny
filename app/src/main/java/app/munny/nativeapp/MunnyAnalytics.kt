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
private val chartColors=listOf(Sage,MatrixOrange,Color(0xFF60B8A7),Color(0xFF9AB9B0),Color(0xFFBBA5CA),Color(0xFF7B9989),Color(0xFFF7C083))

@Composable fun MonthlyFinancePanel(data:MunnyDocument) {
    val visible=LocalMoneyVisible.current
    val periods=remember { (5 downTo 0).map{YearMonth.now().minusMonths(it.toLong())} }
    val values=remember(data,periods) { periods.map { FinanceMath.month(data.movements(),data.exceptions(),it.toString()) } }
    val maxVal=max(1L,values.maxOf { max(it.income,it.consumption) })
    SheetCard(Modifier.fillMaxWidth()) {
        Text("Fluxo financeiro · 6 meses",fontWeight=FontWeight.Bold)
        Text("Recebimentos confirmados × consumo por competência",color=Muted,style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(15.dp))
        if (!visible) {
            Text("Valores e gráficos ocultos. Toque no olho para visualizar.",color=Muted)
        } else if(values.all{it.income==0L&&it.consumption==0L}) {
            Text("Sem movimentações nesses meses. O gráfico será criado ao registrar lançamentos.",color=Muted)
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(15.dp),verticalAlignment=Alignment.CenterVertically){
                LegendDot(Sage,"Recebimentos")
                LegendDot(MatrixOrange,"Consumo")
            }
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(152.dp)) {
                val inset=9.dp.toPx()
                val height=size.height-inset
                val groupWidth=size.width/values.size
                val barWidth=(groupWidth*.25f).coerceAtMost(18.dp.toPx())
                drawLine(Line,Offset(0f,height),Offset(size.width,height),strokeWidth=1.dp.toPx())
                values.forEachIndexed { ix,entry ->
                    val x=ix*groupWidth+groupWidth*.25f
                    val incomeHeight=height*(entry.income.toDouble()/maxVal).toFloat()
                    val consumeHeight=height*(entry.consumption.toDouble()/maxVal).toFloat()
                    if(incomeHeight>0) drawRoundRect(color=Sage,topLeft=Offset(x,height-incomeHeight),size=androidx.compose.ui.geometry.Size(barWidth,incomeHeight),cornerRadius=CornerRadius(4.dp.toPx()))
                    if(consumeHeight>0) drawRoundRect(color=MatrixOrange,topLeft=Offset(x+barWidth+3.dp.toPx(),height-consumeHeight),size=androidx.compose.ui.geometry.Size(barWidth,consumeHeight),cornerRadius=CornerRadius(4.dp.toPx()))
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceAround){
                periods.forEach { ym -> Text("${ym.monthValue.toString().padStart(2,'0')}/${ym.year%100}",color=Muted,style=MaterialTheme.typography.labelSmall) }
            }
            Spacer(Modifier.height(7.dp))
            val last=values.last()
            Text("Mês atual: ${safeMoney(last.income)} recebidos · ${safeMoney(last.consumption)} consumidos",
                style=MaterialTheme.typography.labelSmall,color=Muted)
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
    SheetCard(Modifier.fillMaxWidth()) {
        Text("Detalhamento de gastos",fontWeight=FontWeight.Bold)
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
                        drawArc(Line,0f,360f,false,style=Stroke(width=width))
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
