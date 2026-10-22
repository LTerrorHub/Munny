package app.munny.nativeapp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val cardColor=Color(0xFF18B48C)
private val directColor=Color(0xFFF4AB56)

/** Outstanding installments are obligations, not yet debits from bank accounts. */
@Composable fun CardPositionDetails(data:MunnyDocument) {
    val cards=data.cards()
    val bills=remember(data) {
        FinanceMath.invoices(cards,data.movements(),data.exceptions(),LocalDate.now().plusYears(10).toString())
            .filter{it.remaining>0L}
    }
    val unpaid=bills.sumOf{it.remaining}
    val current=YearMonth.now().toString()
    val purchases=remember(data,current) {
        FinanceMath.spendingChannels(data.movements(),data.exceptions(),current).card
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color=Line)
    Spacer(Modifier.height(13.dp))
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)) {
        Icon(Icons.Default.CreditCard,null,tint=Sage)
        Text("Posição dos cartões",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
    }
    Text("Compras no cartão não reduzem o saldo das contas até o pagamento da fatura.",
        color=Muted,style=MaterialTheme.typography.labelSmall)
    Spacer(Modifier.height(9.dp))
    Row(Modifier.fillMaxWidth()) {
        Text("Compras deste mês",Modifier.weight(1f),color=Muted)
        Text(safeMoney(purchases),fontWeight=FontWeight.SemiBold)
    }
    Row(Modifier.fillMaxWidth().padding(top=7.dp)) {
        Text("Faturas em aberto¹",Modifier.weight(1f),color=Muted)
        Text(safeMoney(unpaid),fontWeight=FontWeight.SemiBold,
            color=if(unpaid>0)MatrixOrange else Ink)
    }
    cards.forEach { card ->
        val pending=bills.filter{it.cardId==card.id}
        val amount=pending.sumOf{it.remaining}
        val next=pending.filter{it.due>=LocalDate.now().toString()}.minByOrNull{it.due}
        Row(Modifier.fillMaxWidth().padding(top=8.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(card.name,fontWeight=FontWeight.SemiBold,
                    maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(if(next==null)"Sem vencimento futuro" else "Próx. vencimento: ${next.due}",
                    color=Muted,style=MaterialTheme.typography.labelSmall)
            }
            Text(safeMoney(amount),fontWeight=FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(9.dp))
    Text("¹ Inclui parcelas futuras; não é dinheiro já retirado nem limite exato disponível.",
        color=Muted,style=MaterialTheme.typography.labelSmall)
}

/** Percentual de consumo: cartão versus despesas diretas. Transferências em grupo separado. */
@Composable fun SpendingTypeDashboard(data:MunnyDocument,onSelectKind:(String)->Unit) {
    val visible=LocalMoneyVisible.current
    val chartTrack=Line
    var offset by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf("card") }
    val period=remember(offset){YearMonth.now().plusMonths(offset.toLong())}
    val channels=remember(data,period) {
        FinanceMath.spendingChannels(data.movements(),data.exceptions(),period.toString())
    }
    val rows=remember(data,period) {
        FinanceMath.occurrences(data.movements(),data.exceptions(),period.atEndOfMonth().toString())
            .filter{it.date.startsWith(period.toString())}
            .sortedWith(compareByDescending<MoneyMovement>{it.date}.thenBy{it.id})
    }
    val detail=rows.filter{m->when(selected) {
        "card" -> m.kind=="card"
        "expense" -> m.kind=="expense" && m.status=="paid"
        else -> m.kind=="transfer" && m.status=="paid"
    }}
    SheetCard(Modifier.fillMaxWidth()) {
        Text("Como você gastou",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
        Text("Meios de pagamento, sem duplicar faturas",color=Muted,style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            FilledTonalIconButton(onClick={offset=(offset-1).coerceAtLeast(-24)},enabled=offset>-24) {
                Icon(Icons.Default.ArrowBackIosNew,"Mês anterior",tint=Sage)
            }
            Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(period.month.getDisplayName(TextStyle.FULL,Locale("pt","BR"))
                    .replaceFirstChar{it.uppercase()},fontWeight=FontWeight.Bold)
                Text(period.year.toString(),style=MaterialTheme.typography.labelSmall,color=Muted)
            }
            FilledTonalIconButton(onClick={offset=(offset+1).coerceAtMost(0)},enabled=offset<0) {
                Icon(Icons.Default.ArrowForwardIos,"Mês seguinte",tint=Sage)
            }
        }
        Spacer(Modifier.height(16.dp))
        if(!visible) {
            Text("Detalhamento oculto pelo modo privacidade.",color=Muted)
        } else if(channels.consumption==0L && channels.transferOut==0L) {
            Text("Nenhuma compra, despesa paga ou transferência neste período.",color=Muted)
        } else {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(17.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Canvas(Modifier.size(110.dp)) {
                    val thickness=19.dp.toPx()
                    drawArc(chartTrack,-90f,360f,false,style=Stroke(width=thickness))
                    if(channels.consumption>0L) {
                        val sweep=360f*channels.cardPercentage
                        drawArc(cardColor,-90f,sweep,false,style=Stroke(width=thickness))
                        drawArc(directColor,-90f+sweep,360f-sweep,false,style=Stroke(width=thickness))
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text("Consumo mensal",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Text(safeMoney(channels.consumption),fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleLarge)
                    Text("Cartão + despesas pagas",color=Muted,style=MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(11.dp))
            listOf(
                Triple("card","Cartão",channels.card),
                Triple("expense","Despesa direta",channels.direct),
                Triple("transfer","Transferências",channels.transferOut)
            ).forEachIndexed {idx, (kind,label,amount)->
                val percent=if(channels.consumption==0L)0.0
                    else amount.toDouble()*100.0/channels.consumption
                Surface(onClick={selected=kind;onSelectKind(kind)},
                    modifier=Modifier.fillMaxWidth().padding(vertical=3.dp),
                    shape=RoundedCornerShape(13.dp),
                    color=if(selected==kind)SageSoft else MaterialTheme.colorScheme.surface,
                    border=if(selected==kind)androidx.compose.foundation.BorderStroke(
                        1.dp,Sage.copy(alpha=.55f)) else null) {
                    Row(Modifier.padding(horizontal=11.dp,vertical=12.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        Surface(color=when(idx){0->cardColor;1->directColor;else->Muted},
                            shape=RoundedCornerShape(4.dp),modifier=Modifier.size(11.dp)){}
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label,fontWeight=FontWeight.SemiBold)
                            Text(if(kind=="transfer")"Movimento interno; não é gasto"
                                else String.format(Locale("pt","BR"),"%.1f%% do consumo",percent),
                                color=Muted,style=MaterialTheme.typography.labelSmall)
                        }
                        Text(safeMoney(amount),fontWeight=FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(9.dp))
            Text(when(selected) {
                "card" -> "Compras e parcelas no cartão"
                "expense" -> "Despesas diretas confirmadas"
                else -> "Transferências entre contas"
            },fontWeight=FontWeight.Bold)
            if(detail.isEmpty())Text("Sem lançamentos neste grupo.",color=Muted)
            detail.take(8).forEach{m->
                Row(Modifier.fillMaxWidth().padding(vertical=7.dp),
                    verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.title,fontWeight=FontWeight.Medium,
                            maxLines=1,overflow=TextOverflow.Ellipsis)
                        Text("${m.category} · ${m.date}",color=Muted,
                            style=MaterialTheme.typography.labelSmall)
                    }
                    Text(safeMoney(m.amount),fontWeight=FontWeight.SemiBold)
                }
            }
            if(detail.size>8)Text("Mais ${detail.size-8} registros na lista abaixo.",
                style=MaterialTheme.typography.labelSmall,color=Muted)
            Spacer(Modifier.height(7.dp))
            Text("Percentuais: cartão + despesas diretas. Transferências e pagamentos de fatura ficam fora da soma de consumo.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}
