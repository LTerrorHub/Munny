package app.munny.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val shareColors=listOf(Color(0xFF42CBA3),Color(0xFF70B9FF),Color(0xFFD6A4FF),Color(0xFFFFBF70),Color(0xFFEE91A5),Color(0xFFA4B3C1))

@Composable fun MunnyChartPanel(rows:List<MoneyMovement>,topic:String,style:String,
    compact:Boolean=false,onChange:(String,String)->Unit,onDetail:(()->Unit)?=null) {
    val safeTopic=MunnyChartMath.safeTopic(topic)
    val safeStyle=MunnyChartMath.safeStyle(safeTopic,style)
    SheetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text(if(compact)"Gráficos do mês" else "Explorar gráficos",Modifier.weight(1f),fontWeight=FontWeight.Bold,color=Ink)
            if(onDetail!=null)TextButton(onClick=onDetail){Text("Detalhar")}
        }
        Text(if(compact)"Resumo do mês atual" else "Os gráficos usam os mesmos registros e filtros da lista.",
            color=Muted,style=MaterialTheme.typography.labelSmall)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("trend" to "Evolução","categories" to "Categorias","channels" to "Meios").forEach{(id,label)->
                FilterChip(selected=safeTopic==id,onClick={onChange(id,MunnyChartMath.safeStyle(id,safeStyle))},label={Text(label)})
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            val choices=if(safeTopic=="trend")listOf("line" to "Linhas","bars" to "Barras")
                else listOf("donut" to "Rosca","bars" to "Barras")
            choices.forEach{(id,label)->FilterChip(selected=safeStyle==id,
                onClick={onChange(safeTopic,id)},label={Text(label)},modifier=Modifier.weight(1f))}
        }
        Spacer(Modifier.height(12.dp))
        if(!LocalMoneyVisible.current)Text("Gráficos e proporções ocultos por privacidade.",color=Muted)
        else if(safeTopic=="trend") {
            val trend=remember(rows,compact){MunnyChartMath.trend(rows,if(compact)6 else 12)}
            TrendPlot(trend,safeStyle,compact)
        } else {
            val groups=remember(rows,safeTopic,compact){MunnyChartMath.shares(rows,safeTopic,if(compact)5 else Int.MAX_VALUE)}
            SharePlot(groups,safeStyle,compact)
        }
        if(!compact) {
            HorizontalDivider(Modifier.padding(vertical=12.dp),color=Line)
            Text("Consumo = despesas pagas + compras no cartão. Transferências e pagamentos de fatura ficam fora dessa soma.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun SharePlot(groups:List<MunnyChartShare>,style:String,compact:Boolean) {
    val total=groups.sumOf{it.amount}
    if(total<=0L){Text("Sem consumo para este gráfico no período.",color=Muted);return}
    var selected by rememberSaveable{mutableStateOf<String?>(null)}
    val highlight=groups.find{it.key==selected}
    if(style=="donut") {
        val track=Line
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment=Alignment.Center,modifier=Modifier.size(if(compact)116.dp else 144.dp)) {
                Canvas(Modifier.fillMaxSize().semantics{contentDescription="Distribuição do consumo; valores na legenda abaixo."}) {
                    val thickness=18.dp.toPx()
                    val topLeft=Offset(thickness/2,thickness/2)
                    val arcSize=Size(size.width-thickness,size.height-thickness)
                    drawArc(track,-90f,360f,false,topLeft,arcSize,style=Stroke(thickness))
                    var start=-90f
                    groups.forEachIndexed{index,g->
                        val sweep=(360.0*g.amount/total).toFloat()
                        val color=shareColors[index%shareColors.size].copy(alpha=if(highlight==null||g.key==selected)1f else .3f)
                        drawArc(color,start,sweep,false,topLeft,arcSize,style=Stroke(thickness))
                        start+=sweep
                    }
                }
                Text(if(highlight==null)"100%" else String.format(Locale("pt","BR"),"%.0f%%",highlight.amount.toDouble()*100/total),
                    color=Ink,fontWeight=FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(highlight?.label?:"Consumo",color=Muted,style=MaterialTheme.typography.labelSmall)
                Text(safeMoney(highlight?.amount?:total),color=Ink,fontWeight=FontWeight.Bold)
                Text("Toque na legenda para destacar",color=Muted,style=MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    val max=groups.maxOf{it.amount}.coerceAtLeast(1)
    groups.forEachIndexed{index,g->
        Surface(onClick={selected=if(selected==g.key)null else g.key},color=if(selected==g.key)SageSoft else Color.Transparent,
            shape=RoundedCornerShape(10.dp),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical=7.dp,horizontal=4.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Surface(color=shareColors[index%shareColors.size],shape=RoundedCornerShape(3.dp),modifier=Modifier.size(10.dp)){}
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.label,color=Ink,style=MaterialTheme.typography.bodySmall)
                        if(!compact)Text(String.format(Locale("pt","BR"),"%.1f%% do consumo",g.amount.toDouble()*100/total),
                            color=Muted,style=MaterialTheme.typography.labelSmall)
                    }
                    Text(safeMoney(g.amount),color=Ink,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodySmall)
                }
                if(style=="bars")LinearProgressIndicator(progress={(g.amount.toDouble()/max).toFloat()},
                    color=shareColors[index%shareColors.size],trackColor=Line.copy(alpha=.35f),
                    modifier=Modifier.fillMaxWidth().padding(top=6.dp).height(if(compact)6.dp else 9.dp))
            }
        }
    }
}

@Composable private fun TrendPlot(trend:MunnyTrend,style:String,compact:Boolean) {
    val points=trend.points
    if(points.isEmpty()){Text("Sem registros para analisar no período.",color=Muted);return}
    if(points.all{it.income==0L&&it.consumption==0L}) {
        Text("Sem recebimentos ou consumo neste filtro. Transferências e pagamentos de fatura não entram no gráfico.",color=Muted)
        return
    }
    var selectedDate by rememberSaveable{mutableStateOf<String?>(null)}
    val selectedIndex=points.indexOfFirst{it.date==selectedDate}.takeIf{it>=0}?:points.lastIndex
    val point=points[selectedIndex]
    val max=points.maxOf{maxOf(it.income,it.consumption)}.coerceAtLeast(1)
    val incomeColor=Sage;val expenseColor=Coral;val grid=Line
    Row(horizontalArrangement=Arrangement.spacedBy(18.dp)) {
        ChartLegend(incomeColor,"Recebido");ChartLegend(expenseColor,"Consumo")
    }
    Text("Escala: 0 a ${safeMoney(max)}",color=Muted,style=MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(if(compact)130.dp else 215.dp)
        .semantics{contentDescription="Evolução de recebimentos e consumo. Toque para consultar uma data."}
        .pointerInput(points,style){detectTapGestures{tap->
            val margin=14.dp.toPx();val width=(size.width-2*margin).coerceAtLeast(1f)
            val index=if(style=="bars")(((tap.x-margin)/width)*points.size).toInt().coerceIn(0,points.lastIndex)
                else (((tap.x-margin)/width)*points.lastIndex).roundToInt().coerceIn(0,points.lastIndex)
            selectedDate=points[index].date
        }}) {
        val margin=14.dp.toPx();val width=size.width-2*margin;val bottom=size.height-8.dp.toPx()
        val height=bottom-12.dp.toPx()
        for(i in 0..3){val y=bottom-height*i/3;drawLine(grid.copy(alpha=.6f),Offset(margin,y),Offset(size.width-margin,y),1.dp.toPx())}
        fun x(index:Int)=if(style=="bars")margin+width*(index+.5f)/points.size
            else if(points.size==1)size.width/2 else margin+width*index/points.lastIndex
        fun y(value:Long)=bottom-(value.toDouble()/max*height).toFloat()
        if(style=="line") {
            listOf(true,false).forEach{income->
                val color=if(income)incomeColor else expenseColor
                val path=Path()
                points.forEachIndexed{index,p->
                    val yy=y(if(income)p.income else p.consumption)
                    if(index==0)path.moveTo(x(index),yy)else path.lineTo(x(index),yy)
                }
                drawPath(path,color,style=Stroke(2.5.dp.toPx()))
                points.forEachIndexed{index,p->drawCircle(color,if(index==selectedIndex)4.dp.toPx()else 2.dp.toPx(),
                    Offset(x(index),y(if(income)p.income else p.consumption)))}
            }
        } else {
            val bar=(width/points.size*.32f).coerceAtMost(18.dp.toPx())
            points.forEachIndexed{index,p->
                val center=x(index)
                drawRect(incomeColor,Offset(center-bar,y(p.income)),Size(bar,(bottom-y(p.income)).coerceAtLeast(0f)))
                drawRect(expenseColor,Offset(center+1.dp.toPx(),y(p.consumption)),Size(bar,(bottom-y(p.consumption)).coerceAtLeast(0f)))
            }
        }
        drawLine(grid,Offset(x(selectedIndex),0f),Offset(x(selectedIndex),bottom),1.dp.toPx())
    }
    fun dateLabel(date:String)=if(trend.daily)LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd/MM"))
        else YearMonth.parse(date).format(DateTimeFormatter.ofPattern("MMM/yy",Locale("pt","BR")))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(dateLabel(points.first().date),color=Muted,style=MaterialTheme.typography.labelSmall)
        if(points.size>2)Text(dateLabel(points[points.size/2].date),color=Muted,style=MaterialTheme.typography.labelSmall)
        if(points.size>1)Text(dateLabel(points.last().date),color=Muted,style=MaterialTheme.typography.labelSmall)
    }
    Spacer(Modifier.height(8.dp))
    Text("${dateLabel(point.date)} · toque no gráfico para consultar",color=Muted,style=MaterialTheme.typography.labelSmall)
    Text("Recebido ${safeMoney(point.income)}",color=incomeColor,style=MaterialTheme.typography.bodySmall)
    Text("Consumo ${safeMoney(point.consumption)}",color=expenseColor,style=MaterialTheme.typography.bodySmall)
    if(trend.omitted>0)Text("Exibindo os últimos ${points.size} meses do filtro.",color=Muted,style=MaterialTheme.typography.labelSmall)
    if(!compact) {
        var showValues by rememberSaveable{mutableStateOf(false)}
        TextButton(onClick={showValues=!showValues}){Text(if(showValues)"Ocultar valores por data" else "Ver valores por data")}
        if(showValues) {
            Text("Data · Recebido · Consumo",color=Muted,style=MaterialTheme.typography.labelSmall)
            points.forEach{entry->Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                Text(dateLabel(entry.date),Modifier.weight(.8f),color=Muted,style=MaterialTheme.typography.labelSmall)
                Text(safeMoney(entry.income),Modifier.weight(1f),color=incomeColor,style=MaterialTheme.typography.labelSmall)
                Text(safeMoney(entry.consumption),Modifier.weight(1f),color=expenseColor,style=MaterialTheme.typography.labelSmall)
            }}
        }
    }
}

@Composable private fun ChartLegend(color:Color,label:String) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        Surface(Modifier.size(9.dp),color=color,shape=RoundedCornerShape(50)){}
        Text(label,color=Muted,style=MaterialTheme.typography.labelSmall)
    }
}
