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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import java.time.LocalDate
import java.util.Locale

/**
 * Credit limit vs open obligations. Unpaid installments (including future ones)
 * occupy the declared credit limit; invoice payments release that credit.
 * This is an estimate from Munny's own records, not issuer-reported available
 * balance: authorizations, fees, interest and issuer-specific rules are absent.
 */
data class MunnyCreditPosition(
    val limit:Long,
    val committed:Long,
    val available:Long,
    val overLimit:Long,
    val bills:List<Invoice>,
    val movements:List<MoneyMovement>
) {
    val utilization:Float get() =
        if(limit<=0L)0f else (committed.toDouble()/limit.toDouble()).toFloat().coerceIn(0f,1f)
}

object MunnyCardMath {
    fun position(
        card:MoneyCard,
        records:List<MoneyMovement>,exceptions:Set<String>,
        today:LocalDate=LocalDate.now()
    ):MunnyCreditPosition {
        // Includes installments already registered for future invoice months.
        val horizon=today.plusYears(10).toString()
        val all=FinanceMath.occurrences(records,exceptions,horizon)
            .filter{it.cardId==card.id && it.kind in setOf("card","cardPayment")}
        val bills=FinanceMath.invoices(listOf(card),records,exceptions,horizon)
            .filter{it.remaining>0L}.sortedBy{it.due}
        val committed=bills.sumOf{it.remaining}
        return MunnyCreditPosition(card.limit,committed,(card.limit-committed).coerceAtLeast(0L),
            (committed-card.limit).coerceAtLeast(0L),bills,
            all.sortedWith(compareByDescending<MoneyMovement>{it.date}.thenByDescending{it.id}))
    }
}

@Composable fun MunnyCardStack(
    data:MunnyDocument,
    onEdit:(String)->Unit,
    onDelete:(String)->Unit,
    onPay:(String)->Unit,
    onNewPurchase:(String)->Unit,
    onManageMovements:(String)->Unit
) {
    val cards=data.cards()
    val visible=LocalMoneyVisible.current
    var showBills by remember { mutableStateOf(false) }
    var chosen by remember(cards.map{it.id}){mutableStateOf(cards.firstOrNull()?.id?:"")}
    if(cards.isEmpty()) {
        SheetCard {
            Text("Nenhum cartão cadastrado.",color=Muted)
            Text("Adicione um cartão para visualizar a carteira empilhada e suas faturas.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
        return
    }
    val selected=cards.firstOrNull{it.id==chosen}?:cards.first()
    val position=remember(data,selected.id){
        MunnyCardMath.position(selected,data.movements(),data.exceptions())
    }
    // Nonselected cards are folded to a compact header. Selected card is placed
    // last so its entire surface remains visible and interactive.
    val ordered=cards.filterNot{it.id==selected.id}+selected
    val overlap=67.dp
    val cardHeight=166.dp
    val fullHeight=cardHeight+overlap*(ordered.size-1)
    val isDark=MaterialTheme.colorScheme.background.red<.25f

    Column(verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("Minha carteira",fontWeight=FontWeight.Bold,
                    style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                Surface(color=SageSoft,shape=RoundedCornerShape(10.dp)) {
                    Text("${cards.size} cartão(ões)",Modifier.padding(horizontal=10.dp,vertical=5.dp),
                        color=Sage,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                }
            }
            Text("Toque em um cartão para consultar limite, consumo e disponível.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
        Box(Modifier.fillMaxWidth().height(fullHeight)) {
            ordered.forEachIndexed{index,card->
                val active=card.id==selected.id
                val base=runCatching {
                    val raw=data.objects("cards").firstOrNull{it.optString("id")==card.id}
                        ?.optString("color")?:"#176F62"
                    Color(android.graphics.Color.parseColor(raw))
                }.getOrElse{Color(0xFF147A67)}
                val shade=Color(
                    (base.red*.42f).coerceIn(0f,1f),
                    (base.green*.50f).coerceIn(0f,1f),
                    (base.blue*.58f).coerceIn(0f,1f)
                )
                val primary=if(base.red+base.green+base.blue>2.05f)Color(0xFF267A66)else base
                Surface(
                    onClick={chosen=card.id},
                    modifier=Modifier.fillMaxWidth().height(cardHeight)
                        .offset(y=overlap*index).zIndex(index.toFloat())
                        .shadow(if(active)9.dp else 3.dp,RoundedCornerShape(22.dp)),
                    shape=RoundedCornerShape(22.dp),
                    color=shade,
                    border=BorderStroke(if(active)2.dp else 1.dp,
                        if(active)Sage else Color.White.copy(alpha=.16f))
                ) {
                    Box(Modifier.fillMaxSize()
                        .background(Brush.linearGradient(listOf(primary,shade,Color(0xFF101B22))))) {
                        // Entire card is a material-style tile; no actual bank
                        // logo, card number or sensitive payment credential.
                        Column(Modifier.fillMaxSize().padding(19.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.Contactless,contentDescription=null,
                                    tint=Color.White.copy(alpha=.95f),modifier=Modifier.size(27.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(card.name,fontWeight=FontWeight.Bold,color=Color.White,
                                    maxLines=1,overflow=TextOverflow.Ellipsis,
                                    style=MaterialTheme.typography.titleMedium,
                                    modifier=Modifier.weight(1f))
                                if(active) {
                                    Surface(color=Color.White.copy(alpha=.16f),
                                        shape=RoundedCornerShape(10.dp)) {
                                        Text("ATIVO",Modifier.padding(horizontal=9.dp,vertical=4.dp),
                                            color=Color.White,style=MaterialTheme.typography.labelSmall)
                                    }
                                } else Icon(Icons.Default.KeyboardArrowDown,null,tint=Color.White)
                            }
                            if(active) {
                                Spacer(Modifier.weight(1f))
                                Text("LIMITE CADASTRADO",
                                    color=Color.White.copy(alpha=.73f),
                                    style=MaterialTheme.typography.labelSmall)
                                Text(safeMoney(card.limit),color=Color.White,
                                    fontWeight=FontWeight.Bold,
                                    style=MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(9.dp))
                                Row(horizontalArrangement=Arrangement.spacedBy(17.dp)) {
                                    Text("Fecha dia ${card.closing}",color=Color.White.copy(alpha=.9f),
                                        style=MaterialTheme.typography.labelSmall)
                                    Text("Vence dia ${card.due}",color=Color.White.copy(alpha=.9f),
                                        style=MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        SheetCard {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selected.name,fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium)
                    Text("Limite e faturas registrados no Munny",color=Muted,
                        style=MaterialTheme.typography.labelSmall)
                }
                Icon(Icons.Default.CreditCard,null,tint=Sage)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Limite consumido",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Text(safeMoney(position.committed),fontWeight=FontWeight.Bold,
                        color=if(position.overLimit>0)Coral else Ink,
                        style=MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text("Disponível",color=Muted,
                        style=MaterialTheme.typography.labelSmall)
                    Text(safeMoney(position.available),fontWeight=FontWeight.Bold,
                        color=Sage,style=MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(10.dp))
            if(visible)LinearProgressIndicator(progress={position.utilization},
                modifier=Modifier.fillMaxWidth().height(10.dp),
                color=if(position.overLimit>0)Coral else Sage,
                trackColor=MaterialTheme.colorScheme.surfaceVariant)
            if(visible) {
                Text(String.format(Locale("pt","BR"),"%.1f%% do limite comprometido",
                    if(position.limit<=0L)0.0 else position.committed.toDouble()*100/position.limit),
                    color=Muted,style=MaterialTheme.typography.labelSmall)
                if(position.overLimit>0)Text(
                    "Lançamentos acima do limite: "+safeMoney(position.overLimit),
                    color=Coral,style=MaterialTheme.typography.labelSmall)
            } else Text("Proporção oculta por privacidade.",color=Muted,
                style=MaterialTheme.typography.labelSmall)
            HorizontalDivider(color=Line)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Fechamento: dia ${selected.closing}",color=Muted,
                    style=MaterialTheme.typography.bodySmall)
                Text("Vencimento: dia ${selected.due}",color=Muted,
                    style=MaterialTheme.typography.bodySmall)
            }
            position.bills.firstOrNull()?.let{invoice->
                Text("Próxima fatura: ${invoice.due} · ${safeMoney(invoice.remaining)}",
                    color=Ink,style=MaterialTheme.typography.bodySmall)
            }
            Text("Estimativa pelos registros do Munny. O consumo do limite inclui parcelas futuras em aberto.",
                style=MaterialTheme.typography.labelSmall,color=Muted)
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick={onEdit(selected.id)}){Text("Editar cartão")}
                TextButton(onClick={onDelete(selected.id)}){Text("Excluir cartão",color=Coral)}
            }
        }
        Button(onClick={onNewPurchase(selected.id)},modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Adicionar compra")
        }
        OutlinedButton(onClick={onManageMovements(selected.id)},modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.ReceiptLong,null);Spacer(Modifier.width(8.dp));Text("Gerenciar lançamentos")
        }
        OutlinedButton(onClick={showBills=!showBills},modifier=Modifier.fillMaxWidth()) {
            Text(if(showBills)"Ocultar faturas" else "Consultar faturas (${position.bills.size})")
        }
        if(showBills)SheetCard {
            Text("Faturas de ${selected.name}",fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
            if(position.bills.isEmpty())Text("Nenhuma fatura em aberto.",color=Muted)
            position.bills.take(12).forEach{invoice->
                HorizontalDivider(color=Line.copy(alpha=.6f))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Fatura ${invoice.month}",fontWeight=FontWeight.SemiBold)
                        Text("Vence ${invoice.due}",color=Muted,
                            style=MaterialTheme.typography.labelSmall)
                    }
                    Text(safeMoney(invoice.remaining),fontWeight=FontWeight.SemiBold)
                }
                TextButton(onClick={onPay("${selected.id}:${invoice.month}")}) {
                    Text("Registrar pagamento")
                }
            }
        }
    }
}
