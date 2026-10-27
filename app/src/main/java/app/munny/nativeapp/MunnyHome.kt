package app.munny.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable fun HomeHud(vm:MunnyViewModel,onNew:()->Unit,onCustomize:()->Unit) {
    val data=vm.data
    val accounts=remember(data){data.accounts()}
    val moves=remember(data){data.movements()}
    val exceptions=remember(data){data.exceptions()}
    val current=YearMonth.now()
    val monthly=remember(data,current){FinanceMath.month(moves,exceptions,current.toString())}
    val balance=remember(data,today){FinanceMath.breakdown(accounts,moves,exceptions,today).total}
    val bills=remember(data,today){FinanceMath.invoices(data.cards(),moves,exceptions,
        LocalDate.now().plusYears(10).toString()).filter{it.remaining>0L}.sortedBy{it.due}}
    val visibleModules=vm.homeOrder.filterNot{it in vm.homeHidden}
    LazyColumn(contentPadding=PaddingValues(17.dp,16.dp,17.dp,32.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    val name=data.settings().optString("name").trim()
                    Text(if(name.isBlank())"Seu panorama" else "Olá, $name",color=Ink,
                        style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                    Text(current.month.getDisplayName(TextStyle.FULL,Locale("pt","BR"))
                        .replaceFirstChar{it.uppercase()}+" "+current.year,color=Muted)
                }
                IconButton(onClick=onCustomize){Icon(Icons.Default.DashboardCustomize,"Personalizar Início",tint=Sage)}
            }
        }
        if(visibleModules.isEmpty())item {SheetCard {
            Text("Monte o seu painel inicial",fontWeight=FontWeight.Bold)
            Text("Escolha os resumos que deseja acompanhar.",color=Muted)
            Button(onClick=onCustomize){Text("Escolher blocos")}
        }}
        items(visibleModules,key={it}){id->
            when(id) {
                "balance" -> SheetCard(Modifier.fillMaxWidth().clickable{vm.activeTab="accounts"}) {
                    Text("Saldo disponível",color=Muted)
                    BigMoney(balance)
                    Text("Somente contas e carteira · Ver contas →",color=Sage,
                        style=MaterialTheme.typography.labelSmall)
                }
                "monthly" -> Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    NumberTile("Recebido no mês",monthly.income,"Confirmado",Modifier.weight(1f))
                    NumberTile("Gastos do mês",monthly.consumption,"Despesas + cartão",Modifier.weight(1f),positive=false)
                }
                "attention" -> SheetCard(Modifier.fillMaxWidth()) {
                    HudTitle("Precisa de atenção","Ver cartões"){vm.activeTab="accounts"}
                    val urgent=bills.filter{it.due<=LocalDate.now().plusDays(7).toString()}
                    if(urgent.isEmpty())Text("Nenhuma fatura vencida ou a vencer nos próximos 7 dias.",color=Muted)
                    urgent.take(2).forEach{bill->
                        Text(data.cards().find{it.id==bill.cardId}?.name?:"Cartão",fontWeight=FontWeight.SemiBold)
                        Text("${if(bill.due<today)"Em atraso" else "Vence"} · ${bill.due} · ${safeMoney(bill.remaining)}",
                            color=if(bill.due<today)Coral else MatrixOrange,style=MaterialTheme.typography.bodySmall)
                    }
                }
                "cards" -> SheetCard(Modifier.fillMaxWidth()) {
                    HudTitle("Cartões","Ver cartões"){vm.activeTab="accounts"}
                    val limit=data.cards().sumOf{it.limit}
                    val committed=bills.sumOf{it.remaining}
                    Text("${data.cards().size} cartão(ões) · Limite total ${safeMoney(limit)}",color=Muted,
                        style=MaterialTheme.typography.bodySmall)
                    Text("Consumido ${safeMoney(committed)}",color=Ink,fontWeight=FontWeight.SemiBold)
                    val available=data.cards().sumOf{card->
                        (card.limit-bills.filter{it.cardId==card.id}.sumOf{it.remaining}).coerceAtLeast(0)
                    }
                    Text("Disponível ${safeMoney(available)}",color=Sage)
                }
                "budgets" -> SheetCard(Modifier.fillMaxWidth()) {
                    HudTitle("Orçamentos","Planejar"){vm.activeTab="planning"}
                    val budgets=data.objects("budgets").filter{it.optString("month")==current.toString()}
                    val spending=remember(data,current){categoryConsumption(data,current)}
                    if(budgets.isEmpty())Text("Defina limites por categoria para este mês.",color=Muted)
                    budgets.take(3).forEach{budget->
                        val category=budget.optString("category")
                        val used=spending[category]?:0L
                        val limit=budget.optLong("amount")
                        Text(category,fontWeight=FontWeight.SemiBold)
                        if(LocalMoneyVisible.current)LinearProgressIndicator(
                            progress={if(limit<=0)0f else (used.toDouble()/limit).toFloat().coerceIn(0f,1f)},
                            modifier=Modifier.fillMaxWidth().padding(vertical=6.dp),color=if(used>limit)Coral else Sage)
                        Text("${safeMoney(used)} de ${safeMoney(limit)}",color=Muted,style=MaterialTheme.typography.labelSmall)
                    }
                }
                "goals" -> SheetCard(Modifier.fillMaxWidth()) {
                    HudTitle("Metas","Ver metas"){vm.activeTab="planning"}
                    val goals=data.objects("goals")
                    if(goals.isEmpty())Text("Crie uma meta para acompanhar seu progresso.",color=Muted)
                    goals.take(2).forEach{goal->
                        Text(goal.optString("name"),fontWeight=FontWeight.SemiBold)
                        Text("${safeMoney(goal.optLong("saved"))} de ${safeMoney(goal.optLong("target"))}",color=Muted)
                    }
                }
                "forecast" -> SheetCard(Modifier.fillMaxWidth().clickable{vm.activeTab="planning"}) {
                    Text("Projeção no fim do mês",color=Muted)
                    BigMoney(remember(data,current){FinanceMath.projected(accounts,data.cards(),moves,exceptions,current.atEndOfMonth().toString())})
                    Text("Inclui previsões e faturas a vencer · Planejar →",color=Muted,
                        style=MaterialTheme.typography.labelSmall)
                }
                "agenda" -> SheetCard(Modifier.fillMaxWidth()) {
                    HudTitle("Agenda","Abrir agenda"){vm.activeTab="agenda"}
                    val event=data.objects("events").filter{it.optString("date")>=today}
                        .minByOrNull{it.optString("date")+it.optString("time")}
                    Text(event?.optString("title")?:"Nenhum evento futuro cadastrado.",color=Ink)
                    if(event!=null)Text(event.optString("date")+" · "+event.optString("time"),color=Muted)
                }
                "shortcuts" -> SheetCard(Modifier.fillMaxWidth()) {
                    HudTitle("Atalhos")
                    Button(onClick=onNew,modifier=Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Registrar lançamento")
                    }
                    OutlinedButton(onClick={vm.activeTab="transactions"},modifier=Modifier.fillMaxWidth()) {
                        Text("Registros e análises")
                    }
                }
            }
        }
    }
}

@Composable private fun HudTitle(title:String,action:String="",onClick:()->Unit={}) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
        Text(title,Modifier.weight(1f),fontWeight=FontWeight.Bold,color=Ink)
        if(action.isNotBlank())TextButton(onClick=onClick){Text(action,style=MaterialTheme.typography.labelSmall)}
    }
    Spacer(Modifier.height(5.dp))
}
