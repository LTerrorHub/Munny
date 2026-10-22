package app.munny.nativeapp

import androidx.compose.foundation.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

val today:String get()=LocalDate.now().toString()
val monthNow:String get()=YearMonth.now().toString()

@Composable fun SheetCard(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Card(modifier=modifier.animateContentSize(animationSpec=spring(stiffness=Spring.StiffnessMediumLow)),
        shape=RoundedCornerShape(26.dp),colors=CardDefaults.cardColors(containerColor=Panel),
        border=BorderStroke(1.dp,Line.copy(alpha=.72f)),
        elevation=CardDefaults.cardElevation(defaultElevation=1.dp),
        content={Column(Modifier.padding(18.dp),content=content)})
}
@Composable fun PageHeader(title:String,subtitle:String) {
    Text(title,style=MaterialTheme.typography.headlineSmall)
    Text(subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
}
@Composable fun BigMoney(value:Long,highlight:Boolean=true) {
    Text(safeMoney(value),style=MaterialTheme.typography.headlineMedium,color=if(value<0)Coral else if(highlight)Ink else Sage)
}
@Composable fun NumberTile(label:String,value:Long,detail:String,modifier:Modifier=Modifier,positive:Boolean=true) {
    val soft=if(positive) SageSoft else MaterialTheme.colorScheme.errorContainer.copy(alpha=.30f)
    SheetCard(modifier) {
        Surface(shape=RoundedCornerShape(12.dp),color=soft,modifier=Modifier.size(35.dp)){
            Box(contentAlignment=Alignment.Center) {
                Icon(if(positive)Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription=null,tint=if(positive)Sage else Coral,modifier=Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(label,color=Muted,style=MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(5.dp))
        Text(safeMoney(value),style=MaterialTheme.typography.titleMedium,
            fontWeight=FontWeight.Bold,color=Ink,maxLines=1)
        Spacer(Modifier.height(5.dp))
        Text(detail,color=Muted,style=MaterialTheme.typography.labelSmall,maxLines=2)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun MunnyApp(vm:MunnyViewModel,onExport:()->Unit,onImport:()->Unit) {
    val data=vm.data
    var editor by remember { mutableStateOf("") }
    var editId by remember { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf<Pair<String,String>?>(null) }
    var showSnack by remember { mutableStateOf(false) }
    var eventDate by remember { mutableStateOf(today) }
    val tabs=listOf("home","transactions","accounts","planning","agenda")
    val labels=listOf("Início","Lançamentos","Contas","Planejar","Agenda")
    val icons=listOf(Icons.Default.Home,Icons.Default.ReceiptLong,Icons.Default.AccountBalanceWallet,
        Icons.Default.TrackChanges,Icons.Default.CalendarMonth)
    BackHandler(vm.activeTab=="settings") { vm.activeTab="home" }
    CompositionLocalProvider(LocalMoneyVisible provides vm.balancesVisible) {
    Box(Modifier.fillMaxSize().background(Canvas)) {
    MatrixRainBackground(vm.matrixEnabled, vm.themeMode=="dark" || (vm.themeMode=="system" && androidx.compose.foundation.isSystemInDarkTheme()))
    Scaffold(containerColor=Color.Transparent,
        topBar={TopAppBar(title={Row(verticalAlignment=Alignment.CenterVertically){
            Surface(color=SageSoft,shape=RoundedCornerShape(17.dp),modifier=Modifier.size(49.dp)) {
                androidx.compose.foundation.Image(
                    painter=androidx.compose.ui.res.painterResource(R.drawable.ic_munny_launcher),
                    contentDescription="Ícone Munny",modifier=Modifier.padding(2.dp)
                )
            }
            Spacer(Modifier.width(11.dp));Column {Text("Munny",fontWeight=FontWeight.Bold);Text("Seu dinheiro, com clareza",color=Muted,style=MaterialTheme.typography.labelSmall)}
        }},colors=TopAppBarDefaults.topAppBarColors(containerColor=Canvas.copy(alpha=.93f)),
        actions={
            IconButton(onClick=vm::toggleBalanceVisibility) {
                Icon(if(vm.balancesVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription=if(vm.balancesVisible) "Ocultar valores" else "Mostrar valores",
                    tint=Sage)
            }
            IconButton(onClick={if(vm.loggedIn)vm.performSync() else vm.activeTab="settings"}) {
                Icon(Icons.Default.Cloud,contentDescription=if(vm.loggedIn)"Sincronizar nuvem" else "Entrar na nuvem",
                    tint=if(vm.loggedIn)Sage else Muted)
            }
            IconButton(onClick={vm.activeTab="settings"}) {
                Icon(Icons.Default.Settings,contentDescription="Configurações",tint=Ink)
            }
        })},
        bottomBar={NavigationBar(containerColor=Panel.copy(alpha=.98f),tonalElevation=3.dp) {
            tabs.forEachIndexed{index,tab ->
                val chosen=vm.activeTab==tab
                val navScale by animateFloatAsState(if(chosen)1.17f else 1f,
                    animationSpec=spring(dampingRatio=Spring.DampingRatioMediumBouncy,
                        stiffness=Spring.StiffnessMediumLow),label="munny-nav-stretch")
                NavigationBarItem(selected=chosen,onClick={vm.activeTab=tab},
                    icon={Icon(icons[index],contentDescription=null,modifier=Modifier.graphicsLayer(
                        scaleX=navScale,scaleY=navScale))},
                    label={Text(labels[index],maxLines=1)},alwaysShowLabel=true,
                    colors=NavigationBarItemDefaults.colors(selectedIconColor=Sage,
                        selectedTextColor=Sage,indicatorColor=SageSoft))
            }
        }},
        floatingActionButton={if(vm.activeTab in setOf("home","transactions")) {
            ExtendedFloatingActionButton(onClick={editor="movement";editId=null},containerColor=Sage,contentColor=Color(0xFF061810),
                icon={Icon(Icons.Default.Add,contentDescription=null)},text={Text("Lançamento")})
        }}
    ) {padding->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if(vm.notice.isNotBlank()) Surface(color=SageSoft,modifier=Modifier.fillMaxWidth().clickable{vm.inform("")}) {
                Row(Modifier.padding(horizontal=16.dp,vertical=9.dp),verticalAlignment=Alignment.CenterVertically){
                    Text(vm.notice,Modifier.weight(1f),color=Ink,style=MaterialTheme.typography.labelSmall)
                    Icon(Icons.Default.Close,contentDescription="Fechar aviso",tint=Sage,modifier=Modifier.size(17.dp))
                }
            }
            AnimatedContent(targetState=vm.activeTab,
                transitionSpec={
                    (fadeIn(tween(270))+slideInHorizontally(tween(330)){it/7}) togetherWith
                    (fadeOut(tween(170))+slideOutHorizontally(tween(210)){-it/9})
                },
                label="munny-tab-transition") { activeTab ->
            when(activeTab) {
                "home" -> HomeScreen(data,onAccount={vm.activeTab="accounts"},onTransaction={editor="movement"},onPlanning={vm.activeTab="planning"},onToggleBalance=vm::toggleBalanceVisibility)
                "transactions" -> TransactionsScreen(data,onNew={editor="movement";editId=null},onEdit={editor="movement";editId=it},
                    onDelete={askDelete="transactions" to it})
                "accounts" -> AccountsScreen(data,onNewAccount={editor="account";editId=null},onEditAccount={editor="account";editId=it},
                    onNewCard={editor="card";editId=null},onEditCard={editor="card";editId=it},onPay={editor="invoice";editId=it},
                    onDeleteAccount={askDelete="accounts" to it},onDeleteCard={askDelete="cards" to it})
                "planning" -> PlanningScreen(data,onGoal={editor="goal"},onBudget={editor="budget"})
                "agenda" -> MunnyAgendaScreen(data,onNewEvent={eventDate=it;editor="event"})
                "settings" -> SettingsScreen(vm,onExport=onExport,onImport=onImport)
            }
            }
        }
    }
    }
    }
    when(editor){
        "movement"->MovementEditor(data,editId,onDismiss={editor=""},onSave={m,notes,parts->
            if(vm.mutate{it.upsertMovement(m,notes,parts)})editor=""
        })
        "account"->AccountEditor(data,editId,onDismiss={editor=""},onSave={id,name,value->if(vm.mutate{it.upsertAccount(id,name,value)})editor=""})
        "card"->CardEditor(data,editId,onDismiss={editor=""},onSave={id,name,value,closing,due->if(vm.mutate{it.upsertCard(id,name,value,closing,due)})editor=""})
        "invoice"->InvoiceEditor(data,editId?:"",onDismiss={editor=""},onPay={card,bill,acc,date->if(vm.mutate{it.payInvoice(card,bill,acc,date)})editor=""})
        "goal"->GoalEditor(onDismiss={editor=""},onSave={name,target,saved,deadline->if(vm.mutate{it.addGoal(name,target,saved,deadline)})editor=""})
        "budget"->BudgetEditor(data,onDismiss={editor=""},onSave={cat,amount,month->if(vm.mutate{it.addBudget(cat,amount,month)})editor=""})
        "event"->EventEditor(initialDate=eventDate,onDismiss={editor=""},onSave={title,date,time,notes->if(vm.mutate{it.addEvent(title,date,time,notes)})editor=""})
    }
    askDelete?.let{(kind,id)->
        AlertDialog(onDismissRequest={askDelete=null},title={Text("Excluir registro?")},
            text={Text("Esta ação altera os saldos e pode ser sincronizada com a sua conta. Confira antes de continuar.")},
            confirmButton={TextButton(onClick={
                val isUsed=if(kind=="accounts") data.movements().any{it.accountId==id||it.toAccountId==id}
                    else if(kind=="cards")data.movements().any{it.cardId==id} else false
                if(isUsed)vm.inform("Não é possível excluir enquanto existem lançamentos vinculados.")
                else vm.mutate{it.delete(kind,id)}
                askDelete=null
            }){Text("Excluir",color=Coral)}},dismissButton={TextButton(onClick={askDelete=null}){Text("Cancelar")}})
    }
}

@Composable fun HomeScreen(data:MunnyDocument,onAccount:()->Unit,onTransaction:()->Unit,onPlanning:()->Unit,onToggleBalance:()->Unit) {
    val accounts=data.accounts();val moves=data.movements();val exceptions=data.exceptions()
    val balance=remember(data) {FinanceMath.breakdown(accounts,moves,exceptions,today)}
    val monthly=remember(data) {FinanceMath.month(moves,exceptions,monthNow)}
    val forecast=remember(data){FinanceMath.projected(accounts,data.cards(),moves,exceptions,YearMonth.now().atEndOfMonth().toString())}
    var expanded by remember {mutableStateOf(false)}
    val visible=LocalMoneyVisible.current
    val settingsName=data.settings().optString("name").trim()
    LazyColumn(contentPadding=PaddingValues(horizontal=16.dp,vertical=17.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if(settingsName.isNotBlank())"Olá, $settingsName!" else "Seu panorama financeiro",
                        style=MaterialTheme.typography.headlineSmall,color=Ink,fontWeight=FontWeight.Bold)
                    Text("Seu dinheiro, com mais clareza.",color=Muted)
                }
                Surface(shape=RoundedCornerShape(50),color=SageSoft) {
                    Text(YearMonth.now().month.getDisplayName(java.time.format.TextStyle.SHORT,Locale("pt","BR"))
                        .replaceFirstChar{it.uppercase()},Modifier.padding(horizontal=14.dp,vertical=9.dp),
                        color=Sage,fontWeight=FontWeight.SemiBold)
                }
            }
        }
        item {SheetCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Surface(color=SageSoft,shape=RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.AccountBalanceWallet,null,Modifier.padding(10.dp),tint=Sage)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Saldo disponível",fontWeight=FontWeight.SemiBold,color=Ink)
                    Text("Valor atual nas contas",color=Muted,style=MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick=onToggleBalance) {
                    Icon(if(visible)Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription=if(visible)"Ocultar saldo" else "Exibir saldo",tint=Sage)
                }
            }
            Spacer(Modifier.height(8.dp))
            BigMoney(balance.total)
            Text("Somente valores efetivamente confirmados",color=Muted,style=MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(9.dp))
            TextButton(onClick={expanded=!expanded},contentPadding=PaddingValues(0.dp)){
                Text(if(expanded)"Ocultar composição" else "Detalhar saldo")
                Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
            }
            if(expanded) {
                HorizontalDivider(color=Line);Spacer(Modifier.height(10.dp))
                BalanceRow("Saldo inicial",balance.initial,true)
                BalanceRow("Recebimentos",balance.income,true)
                BalanceRow("Despesas pagas",balance.expenses,false)
                BalanceRow("Faturas pagas",balance.invoices,false)
                Text("Transferências internas se anulam no total.",color=Muted,style=MaterialTheme.typography.labelSmall)
                HorizontalDivider(Modifier.padding(vertical=8.dp),color=Line)
                BalanceRow("Saldo reconciliado",balance.total,balance.total>=0L)
                CardPositionDetails(data)
                TextButton(onClick=onAccount) {
                    Icon(Icons.Default.CreditCard,null)
                    Spacer(Modifier.width(7.dp))
                    Text("Ver todas as faturas")
                }
            }
        }}
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            NumberTile("Entradas do mês",monthly.income,"Recebidos",Modifier.weight(1f),positive=true)
            NumberTile("Gastos do mês",monthly.consumption,"Despesas + cartão",Modifier.weight(1f),positive=false)
        }}
        item {MonthlyFinancePanel(data)}
        item {CategorySpendingPanel(data)}
        item {SheetCard {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp,null,tint=Sage)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Saldo projetado",fontWeight=FontWeight.SemiBold)
                    Text("Até o fim do mês, considerando previsões",color=Muted,style=MaterialTheme.typography.labelSmall)
                }
                Text(safeMoney(forecast),color=Ink,fontWeight=FontWeight.SemiBold)
            }
        }}
        if(accounts.isEmpty())item {SheetCard {Text("Cadastre sua primeira conta",fontWeight=FontWeight.Bold)
            Text("Informe o dinheiro que você já possui. Não registre esse mesmo valor novamente como Entrada.",color=Muted)
            Spacer(Modifier.height(10.dp));Button(onClick=onAccount){Text("Adicionar conta")}
        }}
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Lançamentos recentes",modifier=Modifier.weight(1f),fontWeight=FontWeight.Bold)
            TextButton(onClick=onTransaction){Text("Novo lançamento")}
        }}
        items(FinanceMath.occurrences(moves,exceptions,today).sortedByDescending{it.date}.take(6),key={it.id}){MovementRow(it)}
        item {Spacer(Modifier.height(55.dp))}
    }
}
@Composable fun BalanceRow(label:String,value:Long,plus:Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp),verticalAlignment=Alignment.CenterVertically){
        Text(label,color=Muted,modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium)
        val visible=LocalMoneyVisible.current
        val sign=if(value<0L)"−" else if(plus)"+" else "−"
        Text(if(visible) "${sign} ${safeMoney(kotlin.math.abs(value))}" else safeMoney(value),
            fontWeight=FontWeight.SemiBold,color=if(value<0L)Coral else Ink)
    }
}
@Composable fun MovementRow(m:MoneyMovement,onEdit:(()->Unit)?=null,onDelete:(()->Unit)?=null){
    SheetCard {
        Row(verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(12.dp),color=if(m.kind=="income")SageSoft else if(m.kind=="card")Lilac else Canvas) {
                Icon(if(m.kind=="income")Icons.Default.SouthWest else if(m.kind=="card")Icons.Default.CreditCard else Icons.Default.NorthEast,
                    null,tint=if(m.kind=="income")Sage else Ink,modifier=Modifier.padding(11.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)){
                Text(m.title, fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                val kindName=when(m.kind){"card"->"Cartão";"expense"->"Despesa";"transfer"->"Transferência interna";"cardPayment"->"Fatura paga";else->"Entrada"}
                Text("${kindName} · ${m.category} · ${m.date} · ${if(m.status=="paid")"Confirmado" else "Previsto"}",
                    color=Muted,style=MaterialTheme.typography.labelSmall)
            }
            Text("${if(m.kind=="income")"+" else if(m.kind=="transfer")"⇄" else "−"} ${safeMoney(m.amount)}",
                color=if(m.kind=="income")Sage else Ink,fontWeight=FontWeight.Bold)
        }
        if(onEdit!=null||onDelete!=null) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                if(onEdit!=null)TextButton(onClick=onEdit){Text("Editar")}
                if(onDelete!=null)TextButton(onClick=onDelete){Text("Excluir",color=Coral)}
            }
        }
    }
}
@Composable fun TransactionsScreen(data:MunnyDocument,onNew:()->Unit,onEdit:(String)->Unit,onDelete:(String)->Unit) {
    val entries=data.movements().sortedByDescending {it.date}
    var filter by remember {mutableStateOf("all")}
    var query by remember {mutableStateOf("")}
    val shown=entries.filter{(filter=="all"||it.kind==filter)&&
        (query.isBlank()||it.title.contains(query,true)||it.category.contains(query,true))}
    LazyColumn(contentPadding=PaddingValues(17.dp),verticalArrangement=Arrangement.spacedBy(11.dp)){
        item {PageHeader("Lançamentos","Entradas, despesas, cartões e transferências.")}
        item {SpendingTypeDashboard(data,onSelectKind={filter=it})}
        item {OutlinedTextField(query,{query=it},Modifier.fillMaxWidth(),label={Text("Buscar lançamento")},
            leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)}
        item {Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
            listOf("all" to "Todos","income" to "Entradas","expense" to "Despesas","card" to "Cartão","transfer" to "Transferências","cardPayment" to "Fatura paga").forEach { (id,label) ->
                FilterChip(selected=filter==id,onClick={filter=id},label={Text(label)}) }
        }}
        if(shown.isEmpty())item{SheetCard{Text("Nenhum lançamento encontrado.")}}
        items(shown,key={it.id}){MovementRow(it,onEdit={onEdit(it.id)},onDelete={onDelete(it.id)})}
        item{Spacer(Modifier.height(60.dp))}
    }
}
@Composable fun AccountsScreen(data:MunnyDocument,onNewAccount:()->Unit,onEditAccount:(String)->Unit,
    onNewCard:()->Unit,onEditCard:(String)->Unit,onPay:(String)->Unit,onDeleteAccount:(String)->Unit,onDeleteCard:(String)->Unit) {
    val accounts=data.accounts();val cards=data.cards();val moves=data.movements();val ex=data.exceptions()
    val bills=FinanceMath.invoices(cards,moves,ex,LocalDate.now().plusYears(2).toString())
    LazyColumn(contentPadding=PaddingValues(17.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
        item {PageHeader("Contas e cartões","Dinheiro que você tem e compromissos separados.")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick=onNewAccount){Icon(Icons.Default.Add,null);Spacer(Modifier.width(5.dp));Text("Conta")}
            OutlinedButton(onClick=onNewCard){Icon(Icons.Default.CreditCard,null);Spacer(Modifier.width(5.dp));Text("Cartão")}
        }}
        item {Text("Dinheiro nas contas",fontWeight=FontWeight.Bold)}
        items(accounts,key={it.id}) {account ->
            val b=FinanceMath.breakdown(accounts,moves,ex,today,account.id)
            SheetCard {
                Text(account.name,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(5.dp));BigMoney(b.total)
                Text("Valor de partida ${safeMoney(account.initial)}",color=Muted,style=MaterialTheme.typography.labelSmall)
                Text("Entradas ${safeMoney(b.income)} · Pagamentos ${safeMoney(b.expenses+b.invoices)}",color=Muted,style=MaterialTheme.typography.labelSmall)
                if(b.outgoing!=0L||b.incoming!=0L)Text("Transferências: +${safeMoney(b.incoming)} / −${safeMoney(b.outgoing)}",color=Muted,style=MaterialTheme.typography.labelSmall)
                Row {TextButton(onClick={onEditAccount(account.id)}){Text("Editar")};TextButton(onClick={onDeleteAccount(account.id)}){Text("Excluir",color=Coral)}}
            }
        }
        item {Text("Cartões e faturas",fontWeight=FontWeight.Bold)}
        if(cards.isEmpty())item{SheetCard{Text("Nenhum cartão cadastrado.",color=Muted)}}
        items(cards,key={it.id}) {card ->
            SheetCard {
                Text(card.name,fontWeight=FontWeight.Bold)
                Text("Limite ${safeMoney(card.limit)} · Fecha dia ${card.closing} · Vence dia ${card.due}",color=Muted)
                val cardBills=bills.filter{it.cardId==card.id&&it.remaining>0}.take(8)
                cardBills.forEach{bill ->
                    HorizontalDivider(Modifier.padding(vertical=9.dp),color=Line)
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){Text("Fatura ${bill.month}",fontWeight=FontWeight.SemiBold)
                            Text("Vencimento: ${bill.due}",color=Muted,style=MaterialTheme.typography.labelSmall)}
                        Text(safeMoney(bill.remaining),fontWeight=FontWeight.Bold)
                    }
                    TextButton(onClick={onPay("${card.id}:${bill.month}")}){Text("Registrar pagamento")}
                }
                Row {TextButton(onClick={onEditCard(card.id)}){Text("Editar")};TextButton(onClick={onDeleteCard(card.id)}){Text("Excluir",color=Coral)}}
            }
        }
        item{Spacer(Modifier.height(25.dp))}
    }
}
@Composable fun PlanningScreen(data:MunnyDocument,onGoal:()->Unit,onBudget:()->Unit) {
    val accounts=data.accounts();val moves=data.movements();val ex=data.exceptions()
    val end=YearMonth.now().atEndOfMonth().toString()
    val estimate=FinanceMath.projected(accounts,data.cards(),moves,ex,end)
    val consumption=FinanceMath.month(moves,ex,monthNow).consumption
    LazyColumn(contentPadding=PaddingValues(17.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
        item {PageHeader("Planejamento","Projeções e limites sem misturar caixa com consumo.")}
        item {SheetCard {Text("Saldo projetado no fim do mês",color=Muted)
            BigMoney(estimate)
            Text("Inclui compromissos previstos e o saldo de faturas com vencimento no período.",color=Muted,style=MaterialTheme.typography.labelSmall)
        }}
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text("Limites do mês",Modifier.weight(1f),fontWeight=FontWeight.Bold)
            TextButton(onClick=onBudget){Text("+ Limite")}
        }}
        items(data.objects("budgets").filter{it.optString("month")==monthNow},key={it.getString("id")}){budget->
            val total=FinanceMath.occurrences(moves,ex,YearMonth.now().atEndOfMonth().toString()).filter{
                it.date.startsWith(monthNow)&&it.category==budget.optString("category") && (it.kind=="card"||it.kind=="expense"&&it.status=="paid")}.sumOf{it.amount}
            val limit=budget.optLong("amount")
            SheetCard {
                Text(budget.optString("category"),fontWeight=FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp));LinearProgressIndicator(progress={if(limit>0)(total.toFloat()/limit).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(7.dp));Text("${safeMoney(total)} de ${safeMoney(limit)}",color=Muted)
            }
        }
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            Text("Metas",Modifier.weight(1f),fontWeight=FontWeight.Bold)
            TextButton(onClick=onGoal){Text("+ Meta")}
        }}
        items(data.objects("goals"),key={it.getString("id")}) {goal ->
            val target=goal.optLong("target");val saved=goal.optLong("saved")
            SheetCard {
                Text(goal.optString("name"),fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(6.dp));LinearProgressIndicator(progress={if(target>0)(saved.toFloat()/target).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp));Text("${safeMoney(saved)} de ${safeMoney(target)} · Até ${goal.optString("deadline")}",color=Muted)
            }
        }
        item {Text("Lançamentos previstos",fontWeight=FontWeight.Bold)}
        items(FinanceMath.occurrences(moves,ex,end).filter{it.status=="planned"&&it.kind!="card"}.take(8),key={it.id}){MovementRow(it)}
        item{Spacer(Modifier.height(30.dp))}
    }
}
@Composable fun SettingsScreen(vm:MunnyViewModel,onExport:()->Unit,onImport:()->Unit){
    var emailInput by remember {mutableStateOf("")};var password by remember {mutableStateOf("")}
    var confirmSync by remember {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(17.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
        item {PageHeader("Configurações","Aparência, autenticação, sincronização e backup.")}
        item {SheetCard {
            Text("Aparência",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                listOf("system" to "Sistema","light" to "Claro","dark" to "Escuro").forEach{(key,label)->
                    FilterChip(selected=vm.themeMode==key,onClick={vm.updateThemeMode(key)},label={Text(label)})
                }
            }
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Chuva digital Matrix",fontWeight=FontWeight.SemiBold)
                    Text("Verde no topo, laranja no meio, cinza ao final. Desative para economizar bateria.",color=Muted,style=MaterialTheme.typography.labelSmall)
                }
                Switch(checked=vm.matrixEnabled,onCheckedChange=vm::updateMatrixEnabled)
            }
            Text("Privacidade de valores",fontWeight=FontWeight.SemiBold)
            Text("Use o ícone de olho na barra superior para ocultar ou revelar valores.",color=Muted,style=MaterialTheme.typography.labelSmall)
        }}
        item {SheetCard {
            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Cloud,null,tint=Sage);Spacer(Modifier.width(9.dp));Text("Munny Cloud",fontWeight=FontWeight.Bold)}
            Spacer(Modifier.height(8.dp));Text(vm.cloudStatus,color=Muted)
            if(vm.loggedIn){
                Text("Conectado: ${vm.email}",color=Sage)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(onClick={vm.performSync() },enabled=!vm.busy){Text("Sincronizar")}
                    OutlinedButton(onClick={confirmSync="cloud"},enabled=!vm.busy){Text("Receber nuvem")}
                    OutlinedButton(onClick={confirmSync="local"},enabled=!vm.busy){Text("Enviar aparelho")}
                }
                TextButton(onClick={vm.logout()},enabled=!vm.busy){Text("Sair da conta")}
            } else {
                OutlinedTextField(emailInput,{emailInput=it},label={Text("E-mail")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(password,{password=it},label={Text("Senha")},modifier=Modifier.fillMaxWidth(),singleLine=true,
                    visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation())
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Button(onClick={vm.authenticate(emailInput,password,false)},enabled=!vm.busy&&password.isNotBlank()&&emailInput.isNotBlank()){Text("Entrar")}
                    OutlinedButton(onClick={vm.authenticate(emailInput,password,true)},enabled=!vm.busy&&password.length>=6){Text("Criar conta")}
                }
            }
        }}
        item {SheetCard {
            Text("Exportação e recuperação",fontWeight=FontWeight.Bold)
            Text("Faça um backup antes de trocar de aparelho ou importar dados.",color=Muted)
            Spacer(Modifier.height(8.dp))
            Button(onClick=onExport){Icon(Icons.Default.FileUpload,null);Spacer(Modifier.width(7.dp));Text("Exportar JSON")}
            Spacer(Modifier.height(7.dp))
            OutlinedButton(onClick=onImport){Icon(Icons.Default.FileDownload,null);Spacer(Modifier.width(7.dp));Text("Importar JSON")}
            Text("O JSON exportado não é criptografado. Guarde-o em um local privado.",color=Coral,style=MaterialTheme.typography.labelSmall)
        }}
        item{Spacer(Modifier.height(30.dp))}
    }
    if(confirmSync.isNotBlank())AlertDialog(onDismissRequest={confirmSync=""},
        title={Text("Confirmar substituição de dados")},
        text={Text(if(confirmSync=="cloud")"Os dados deste aparelho serão substituídos pelos da nuvem. O app criará uma cópia local antes de aplicar." else
            "A nuvem receberá os dados deste aparelho. Confirme que esta é a base correta; a revisão será verificada para evitar sobrescritas simultâneas.")},
        confirmButton={Button(onClick={vm.performSync(confirmSync);confirmSync=""}){Text("Confirmar")}},
        dismissButton={TextButton(onClick={confirmSync=""}){Text("Cancelar")}})
}
