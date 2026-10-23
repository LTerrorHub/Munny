package app.munny.nativeapp

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun currencyInput(cents:Long)=String.format(Locale("pt","BR"),"%.2f",cents/100.0)
private fun monthsLabel(n:Int):String {
    val years=n/12
    val months=n%12
    return when{
        n==0->"agora"
        years==0->"${months} mês(es)"
        months==0->"${years} ano(s)"
        else->"${years} ano(s) e ${months} mês(es)"
    }
}

/** Each goal is manual and independent of the actual account balances. */
@Composable fun MunnyGoalDashboard(
    goal:JSONObject,onEdit:()->Unit,historicalContribution:Long?
) {
    val name=goal.optString("name")
    val target=goal.optLong("target")
    val saved=goal.optLong("saved")
    val deadline=goal.optString("deadline")
    var amount by remember(goal.optString("id")){
        mutableStateOf("0,00")
    }
    val amountCents=runCatching{parseMoney(amount)}.getOrNull()
    val projected=remember(target,saved,deadline,amountCents) {
        if(amountCents==null) null else runCatching {
            MunnyGoalMath.calculate(target,saved,amountCents,deadline)
        }.getOrNull()
    }
    val empty=runCatching{MunnyGoalMath.calculate(target,saved,0L,deadline)}.getOrNull()
    val visible=LocalMoneyVisible.current
    val tint=Sage
    val neutral=Line
    SheetCard {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name,fontWeight=FontWeight.Bold,
                    style=MaterialTheme.typography.titleMedium)
                Text("Prazo registrado: ${deadline}",color=Muted,
                    style=MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick=onEdit) {
                Icon(Icons.Default.Edit,contentDescription=null,modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Atualizar")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(114.dp),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val thickness=14.dp.toPx()
                    drawArc(neutral,-90f,360f,false,style=Stroke(width=thickness))
                    if(visible && empty!=null) {
                        drawArc(tint,-90f,360f*empty.progress,false,
                            style=Stroke(width=thickness))
                    }
                }
                Text(if(visible && empty!=null)
                        String.format(Locale("pt","BR"),"%.0f%%",empty.progress*100)
                    else "••%",
                    fontWeight=FontWeight.Bold,
                    color=if(visible)tint else Muted,
                    style=MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                Text("Já reservado",color=Muted,style=MaterialTheme.typography.labelSmall)
                Text(safeMoney(saved),fontWeight=FontWeight.Bold,color=Sage,
                    style=MaterialTheme.typography.titleMedium)
                HorizontalDivider(color=Line)
                Text("Ainda falta",color=Muted,style=MaterialTheme.typography.labelSmall)
                Text(safeMoney((target-saved).coerceAtLeast(0L)),
                    fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium,
                    color=Ink)
                Text("Objetivo: ${safeMoney(target)}",color=Muted,
                    style=MaterialTheme.typography.labelSmall)
            }
        }
        Spacer(Modifier.height(15.dp))
        HorizontalDivider(color=Line)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment=Alignment.CenterVertically) {
            Icon(Icons.Default.Calculate,null,tint=Sage,modifier=Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Como chegar à meta",fontWeight=FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        Text("Quanto pretende guardar por mês?",color=Muted,
            style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(
            value=amount,onValueChange={amount=it.take(20)},
            label={Text("Aporte mensal (R$)")},
            singleLine=true,
            visualTransformation=if(visible)
                androidx.compose.ui.text.input.VisualTransformation.None
                else androidx.compose.ui.text.input.PasswordVisualTransformation(),
            modifier=Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            listOf(50_000L,100_000L,200_000L).forEach{cents->
                FilterChip(
                    selected=amountCents==cents,
                    onClick={amount=currencyInput(cents)},
                    label={Text(if(cents==50_000L)"R$ 500" else if(cents==100_000L)"R$ 1.000" else "R$ 2.000",
                        style=MaterialTheme.typography.labelSmall)}
                )
            }
        }
        if(historicalContribution!=null && historicalContribution>0L) {
            TextButton(onClick={amount=currencyInput(historicalContribution)}) {
                Icon(Icons.Default.TipsAndUpdates,contentDescription=null,
                    modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Simular com margem histórica: ${safeMoney(historicalContribution)}")
            }
        }
        if(!visible) {
            Text("Projeções monetárias ocultas pelo modo privacidade.",color=Muted)
        } else if(projected==null) {
            Text("Informe um aporte mensal válido, maior ou igual a zero.",
                color=Coral,style=MaterialTheme.typography.labelSmall)
        } else if(projected.missing==0L) {
            Text("Meta atingida! Seu valor reservado já alcança o objetivo.",
                color=Sage,fontWeight=FontWeight.SemiBold)
        } else {
            val n=projected.monthsToGoal
            if(n==null) {
                Text("Digite um aporte maior que zero para calcular o tempo necessário.",
                    color=Muted,style=MaterialTheme.typography.bodySmall)
            } else {
                Surface(color=SageSoft,shape=RoundedCornerShape(17.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Tempo estimado para atingir",color=Muted,
                            style=MaterialTheme.typography.labelSmall)
                        Text(monthsLabel(n),color=Sage,fontWeight=FontWeight.Bold,
                            style=MaterialTheme.typography.titleLarge)
                        Text("Previsão: ${projected.projectedDate?.month?.getDisplayName(TextStyle.FULL,Locale("pt","BR"))} ${projected.projectedDate?.year}",
                            color=Ink,style=MaterialTheme.typography.bodySmall)
                        Text("Rendimento assumido: 0%. Reservas ao fim de cada mês.",
                            color=Muted,style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
            val needed=projected.requiredPerMonthForDeadline
            if(needed==null) {
                Text("O prazo cadastrado já venceu. Atualize a data da meta para calcular um plano.",
                    color=Coral,style=MaterialTheme.typography.labelSmall)
            } else if(needed>0L) {
                Spacer(Modifier.height(8.dp))
                Text("Para alcançar no prazo cadastrado:",
                    color=Muted,style=MaterialTheme.typography.bodySmall)
                Text(safeMoney(needed)+" por mês",color=Ink,
                    fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                val difference=(needed-projected.monthlyContribution).coerceAtLeast(0L)
                if(difference>0L) Text(
                    "Isso exigiria ${safeMoney(difference)} a mais por mês do que o aporte que você simulou.",
                    color=Muted,style=MaterialTheme.typography.bodySmall)
                else Text("Seu aporte simulado é suficiente para o prazo informado, mantendo as premissas.",
                    color=Sage,style=MaterialTheme.typography.bodySmall)
            }
            if(n!=null && projected.monthlyContribution>0L && projected.missing>0L) {
                Spacer(Modifier.height(9.dp))
                Text("Alternativas matemáticas",fontWeight=FontWeight.SemiBold)
                listOf(125L,150L).forEach{percent->
                    val larger=(projected.monthlyContribution/100L*percent)+
                        (projected.monthlyContribution%100L*percent/100L)
                    if(larger>projected.monthlyContribution){
                        val quicker=MunnyGoalMath.calculate(target,saved,larger,deadline)
                        if(quicker.monthsToGoal!=null && quicker.monthsToGoal<n) {
                            Text("• Guardando ${safeMoney(larger)}/mês, você anteciparia ${n-quicker.monthsToGoal} mês(es).",
                                color=Muted,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Valores de metas são declarados manualmente e não bloqueiam saldo em conta. São cenários para planejamento; nenhuma transferência ou investimento é realizado.",
            color=Muted,style=MaterialTheme.typography.labelSmall)
    }
}
