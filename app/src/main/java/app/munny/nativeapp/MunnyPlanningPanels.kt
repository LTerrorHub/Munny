package app.munny.nativeapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable private fun MunnyMetric(
    title:String,value:String,detail:String,accent:Color,modifier:Modifier=Modifier
) {
    Card(modifier=modifier,shape=RoundedCornerShape(20.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.55f))) {
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(title,color=Muted,style=MaterialTheme.typography.labelSmall)
            Text(value,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,
                color=accent,maxLines=1)
            Text(detail,color=Muted,style=MaterialTheme.typography.labelSmall,maxLines=2)
        }
    }
}

@Composable fun MunnyFinancialHealth(data:MunnyDocument) {
    val today=remember{LocalDate.now()}
    val summary=remember(data,today) {
        MunnyPlanningMath.health(data.accounts(),data.cards(),data.movements(),data.exceptions(),
            budgets=data.objects("budgets").map {
                BudgetIndicator(it.optString("category"),it.optString("month"),it.optLong("amount"))
            },today=today)
    }
    val visible=LocalMoneyVisible.current
    val commitment=summary.commitmentPercent
    var selectedMonth by remember{mutableIntStateOf(5)}
    val selected=summary.trend[selectedMonth.coerceIn(0,5)]
    val cardColor=Sage
    val secondaryColor=MatrixOrange
    val grid=Line
    val labelColor=Muted
    SheetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Surface(color=SageSoft,shape=RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.MonitorHeart,contentDescription=null,tint=Sage,
                    modifier=Modifier.padding(10.dp).size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Saúde financeira",style=MaterialTheme.typography.titleMedium,
                    fontWeight=FontWeight.Bold)
                Text("Fluxo, compromissos e posição líquida estimada",
                    color=Muted,style=MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            MunnyMetric("Entradas",
                safeMoney(summary.income),"Recebidas no mês",Sage,Modifier.weight(1f))
            MunnyMetric("Consumo",
                safeMoney(summary.consumption),"Cartão + despesas pagas",
                if(summary.consumption>summary.income&&summary.income>0)Coral else Ink,
                Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp))
        MunnyMetric("Margem do mês",
            safeMoney(summary.margin),"Entradas confirmadas menos consumo registrado",
            if(summary.margin<0L)Coral else Sage,Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Text("Comprometimento da renda",fontWeight=FontWeight.SemiBold,color=Ink)
        if(commitment==null) {
            Text("Ainda não há entradas confirmadas neste mês. Não é possível calcular a proporção.",
                color=Muted,style=MaterialTheme.typography.bodySmall)
        } else {
            val fraction=(commitment/100.0).toFloat().coerceIn(0f,1f)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(String.format(Locale("pt","BR"),"%.1f%%",commitment),
                    fontWeight=FontWeight.Bold,color=if(commitment>=100.0)Coral else Sage,
                    modifier=Modifier.weight(1f))
                Text("Consumo ÷ entradas confirmadas",color=Muted,
                    style=MaterialTheme.typography.labelSmall)
            }
            LinearProgressIndicator(progress={fraction},modifier=Modifier.fillMaxWidth().height(9.dp),
                color=if(commitment>=100.0)Coral else Sage,trackColor=MaterialTheme.colorScheme.surfaceVariant)
            Text("Indicador descritivo. Não inclui renda ainda não recebida nem todas as obrigações futuras.",
                color=Muted,style=MaterialTheme.typography.labelSmall,
                modifier=Modifier.padding(top=6.dp))
        }
    }
    Spacer(Modifier.height(13.dp))
    SheetCard(Modifier.fillMaxWidth()) {
        Text("Evolução da posição financeira",fontWeight=FontWeight.Bold,
            style=MaterialTheme.typography.titleMedium)
        Text("Últimos 6 meses · saldos bancários menos faturas abertas em cada data",
            color=Muted,style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(10.dp))
        if(!visible) {
            Text("Gráfico oculto pelo modo privacidade.",color=Muted)
        } else if(data.accounts().isEmpty()) {
            Text("Cadastre uma conta para acompanhar a evolução.",color=Muted)
        } else {
            Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(9.dp).background(cardColor,RoundedCornerShape(50)))
                Text("Contas",color=Muted,style=MaterialTheme.typography.labelSmall)
                Box(Modifier.size(9.dp).background(secondaryColor,RoundedCornerShape(50)))
                Text("Após faturas abertas",color=Muted,style=MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(13.dp))
            Canvas(Modifier.fillMaxWidth().height(158.dp)) {
                val left=7.dp.toPx()
                val right=size.width-7.dp.toPx()
                val top=9.dp.toPx()
                val bottom=size.height-10.dp.toPx()
                val values=summary.trend.flatMap{listOf(it.bankBalance,it.netPosition)}
                val minVal=min(0.0,values.minOf{it.toDouble()})
                val maxVal=max(1.0,values.maxOf{it.toDouble()})
                val difference=(maxVal-minVal).coerceAtLeast(1.0)
                fun locate(index:Int,amount:Long):Offset {
                    val x=left+(right-left)*index/5f
                    val fraction=((amount.toDouble()-minVal)/difference).toFloat()
                    val y=bottom-fraction*(bottom-top)
                    return Offset(x,y)
                }
                repeat(4){j->
                    val y=top+(bottom-top)*j/3f
                    drawLine(grid.copy(alpha=.38f),Offset(left,y),Offset(right,y),
                        strokeWidth=1.dp.toPx())
                }
                fun line(color:Color,points:List<Offset>) {
                    val path=Path()
                    points.forEachIndexed { i,p->
                        if(i==0)path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y)
                    }
                    drawPath(path,color,style=Stroke(width=2.7.dp.toPx(),cap=StrokeCap.Round))
                    points.forEachIndexed{i,p->
                        drawCircle(color,if(i==selectedMonth)4.5.dp.toPx() else 2.3.dp.toPx(),p)
                    }
                }
                line(cardColor,summary.trend.mapIndexed{i,p->locate(i,p.bankBalance)})
                line(secondaryColor,summary.trend.mapIndexed{i,p->locate(i,p.netPosition)})
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                summary.trend.forEachIndexed{i,p->
                    val short=p.month.month.getDisplayName(TextStyle.SHORT,Locale("pt","BR")).take(3)
                    Text(short,modifier=Modifier.clickable{selectedMonth=i}.padding(vertical=5.dp),
                        color=if(selectedMonth==i)Sage else labelColor,
                        fontWeight=if(selectedMonth==i)FontWeight.Bold else FontWeight.Normal,
                        style=MaterialTheme.typography.labelSmall)
                }
            }
            HorizontalDivider(color=Line,modifier=Modifier.padding(vertical=7.dp))
            Text(selected.month.month.getDisplayName(TextStyle.FULL,Locale("pt","BR"))
                .replaceFirstChar{it.uppercase()}+" / "+selected.month.year,
                fontWeight=FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth()) {
                Text("Em contas",Modifier.weight(1f),color=Muted)
                Text(safeMoney(selected.bankBalance),fontWeight=FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Faturas abertas",Modifier.weight(1f),color=Muted)
                Text(safeMoney(selected.openCardBills),fontWeight=FontWeight.SemiBold)
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Posição líquida estimada",Modifier.weight(1f),color=Muted)
                Text(safeMoney(selected.netPosition),fontWeight=FontWeight.Bold,
                    color=if(selected.netPosition<0)Coral else Ink)
            }
            Spacer(Modifier.height(8.dp))
            Text("Visão reconstruída com os saldos iniciais e lançamentos cadastrados. Não representa patrimônio total: outros investimentos, bens e dívidas externas não estão registrados.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
    Spacer(Modifier.height(13.dp))
    SheetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsActive,contentDescription=null,tint=MatrixOrange)
            Spacer(Modifier.width(8.dp))
            Text("Próximos compromissos",fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
        }
        Text("Faturas, despesas previstas e limites de orçamento",color=Muted,
            style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(11.dp))
        if(summary.alerts.isEmpty())Text("Nenhum alerta identificado para os próximos 30 dias.",
            color=Muted)
        else summary.alerts.forEach { alert ->
            HorizontalDivider(color=Line.copy(alpha=.55f))
            Row(Modifier.fillMaxWidth().padding(vertical=11.dp),
                verticalAlignment=Alignment.Top) {
                Icon(if(alert.priority==0)Icons.Default.WarningAmber
                    else Icons.Default.EventAvailable,contentDescription=null,
                    tint=if(alert.priority==0)Coral else MatrixOrange,
                    modifier=Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(alert.title,fontWeight=FontWeight.SemiBold)
                    Text(alert.details,color=Muted,style=MaterialTheme.typography.bodySmall)
                    if(alert.date.isNotBlank())Text(alert.date,color=Muted,
                        style=MaterialTheme.typography.labelSmall)
                }
                if(alert.amount!=null)Text(safeMoney(alert.amount),
                    fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(7.dp))
        Text("Avisos informativos derivados dos registros existentes; não são notificações automáticas de pagamento.",
            color=Muted,style=MaterialTheme.typography.labelSmall)
    }
}

private fun plainMoney(amount:Long)=String.format(Locale("pt","BR"),"%.2f",amount/100.0)
private fun parseRate(value:String):Double {
    val normalized=value.trim().replace("%","").replace(",",".")
    require(normalized.isNotEmpty()){"Informe a taxa de juros."}
    return normalized.toDoubleOrNull() ?: throw IllegalArgumentException("Taxa inválida.")
}

@Composable fun MunnyInvestmentSimulator(data:MunnyDocument) {
    val now=remember{LocalDate.now()}
    val health=remember(data,now){
        MunnyPlanningMath.health(data.accounts(),data.cards(),data.movements(),data.exceptions(),today=now)
    }
    var initial by remember{mutableStateOf("0,00")}
    var monthly by remember{mutableStateOf("100,00")}
    var rate by remember{mutableStateOf("1,00")}
    var basis by remember{mutableStateOf("monthly")}
    var duration by remember{mutableStateOf("24")}
    val visible=LocalMoneyVisible.current
    val evaluation=runCatching {
        MunnyInvestmentMath.calculate(parseMoney(initial),parseMoney(monthly),
            parseRate(rate),basis,duration.trim().toInt())
    }
    val projection=evaluation.getOrNull()
    SheetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.AutoGraph,contentDescription=null,tint=Sage,modifier=Modifier.size(26.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Simulador de investimentos",fontWeight=FontWeight.Bold,
                    style=MaterialTheme.typography.titleMedium)
                Text("Juros compostos, aportes e prazo — atualização instantânea",
                    color=Muted,style=MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(value=initial,onValueChange={initial=it.take(20)},
            label={Text("Valor inicial (R$)")},singleLine=true,
            modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(value=monthly,onValueChange={monthly=it.take(20)},
            label={Text("Aporte por mês (R$)")},singleLine=true,
            modifier=Modifier.fillMaxWidth())
        val tentative=health.illustrativeContribution
        if(tentative!=null) {
            TextButton(onClick={monthly=plainMoney(tentative)}) {
                Icon(Icons.Default.TipsAndUpdates,contentDescription=null,
                    modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Simular com margem histórica: "+safeMoney(tentative))
            }
            Text("Referência: 50% do menor excedente mensal entre os últimos 3 meses, quando há renda registrada em pelo menos 2 deles. Não é recomendação de investimento.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        } else {
            Text("Adicione entradas confirmadas em pelo menos 2 dos últimos 3 meses para habilitar a referência de aporte baseada no histórico.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=rate,onValueChange={rate=it.take(12)},
                label={Text("Taxa (%)")},singleLine=true,modifier=Modifier.weight(1f))
            Column {
                Text("Período da taxa",color=Muted,style=MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    FilterChip(selected=basis=="monthly",onClick={basis="monthly"},
                        label={Text("Mês")})
                    FilterChip(selected=basis=="annual",onClick={basis="annual"},
                        label={Text("Ano")})
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(value=duration,onValueChange={duration=it.take(3)},
            label={Text("Prazo em meses (1–600)")},singleLine=true,
            modifier=Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf(12 to "1 ano",36 to "3 anos",60 to "5 anos",
                120 to "10 anos",240 to "20 anos").forEach{(n,label)->
                FilterChip(selected=duration==n.toString(),onClick={duration=n.toString()},
                    label={Text(label)})
            }
        }
        Spacer(Modifier.height(14.dp))
        if(projection==null) {
            Text(evaluation.exceptionOrNull()?.message?:"Confira os valores informados.",
                color=Coral,style=MaterialTheme.typography.bodySmall)
        } else {
            Card(colors=CardDefaults.cardColors(containerColor=SageSoft),
                shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("Total projetado",color=Ink,style=MaterialTheme.typography.bodyMedium)
                    Text(safeMoney(projection.finalCents),fontWeight=FontWeight.Bold,
                        color=Sage,style=MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Total aportado",Modifier.weight(1f),color=Muted)
                        Text(safeMoney(projection.investedCents),fontWeight=FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth()) {
                        Text("Juros acumulados",Modifier.weight(1f),color=Muted)
                        Text(safeMoney(projection.interestCents),fontWeight=FontWeight.SemiBold,
                            color=Sage)
                    }
                    val ratio=if(projection.finalCents>0)
                        (projection.investedCents.toDouble()/projection.finalCents).toFloat().coerceIn(0f,1f)
                        else 0f
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress={ratio},modifier=Modifier.fillMaxWidth().height(10.dp),
                        color=Sage,trackColor=MatrixOrange.copy(alpha=.28f))
                    Text("Verde: dinheiro aportado · restante: juros simulados",
                        color=Muted,style=MaterialTheme.typography.labelSmall,
                        modifier=Modifier.padding(top=7.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            val accent=Sage
            val grid=Line
            val values=projection.points
            Text("Crescimento acumulado",fontWeight=FontWeight.SemiBold)
            if(visible) {
                Canvas(Modifier.fillMaxWidth().height(142.dp)) {
                    val top=8.dp.toPx()
                    val bottom=size.height-8.dp.toPx()
                    val left=7.dp.toPx()
                    val right=size.width-7.dp.toPx()
                    val maxValue=projection.finalCents.toDouble().coerceAtLeast(1.0)
                    repeat(4){i->
                        val y=top+(bottom-top)*i/3f
                        drawLine(grid.copy(alpha=.4f),Offset(left,y),Offset(right,y),1.dp.toPx())
                    }
                    val path=Path()
                    values.forEachIndexed{index,point->
                        val x=left+(right-left)*point.month/projection.months.toFloat()
                        val fraction=(point.amountCents.toDouble()/maxValue).toFloat()
                        val y=bottom-(bottom-top)*fraction
                        if(index==0)path.moveTo(x,y) else path.lineTo(x,y)
                    }
                    drawPath(path,accent,style=Stroke(width=3.dp.toPx(),cap=StrokeCap.Round))
                    values.lastOrNull()?.let {point->
                        drawCircle(accent,4.dp.toPx(),Offset(right,
                            bottom-(bottom-top)*(point.amountCents.toDouble()/maxValue).toFloat()))
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Hoje",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Text(projection.months.toString()+" meses",color=Muted,
                        style=MaterialTheme.typography.labelSmall)
                }
            } else Text("Gráfico oculto pelo modo privacidade.",color=Muted)
            Spacer(Modifier.height(8.dp))
            val monthlyPct=projection.effectiveMonthlyRate*100.0
            Text("Taxa equivalente: "+
                String.format(Locale("pt","BR"),"%.4f",monthlyPct)+"% efetivos ao mês.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(12.dp))
        Text("Simulação matemática: rendimento nominal constante com capitalização composta e aportes no fim de cada mês. Não considera impostos, taxas, inflação, risco ou variação real do mercado. Não movimenta dinheiro nem garante rentabilidade.",
            color=Muted,style=MaterialTheme.typography.labelSmall)
    }
}
