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
        content={Column(Modifier.padding(21.dp),content=content)})
}
@Composable fun PageHeader(title:String,subtitle:String) {
    Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle,color=Muted,style=MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(19.dp))
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
    var editingGoalId by remember { mutableStateOf<String?>(null) }
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
                    onDeleteAccount={askDelete="accounts" to it},onDeleteCard={askDelete="cards" to it},
                    onEditMovement={editor="movement";editId=it},onDeleteMovement={askDelete="transactions" to it})
                "planning" -> PlanningScreen(data,onGoal={editingGoalId=null;editor="goal"},
                     onEditGoal={editingGoalId=it;editor="goal"},onBudget={editor="budget"})
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
        "goal"->GoalEditor(data,editingGoalId,onDismiss={editor=""},
             onSave={id,name,target,saved,deadline->
                 if(vm.mutate{it.upsertGoal(id,name,target,saved,deadline)})editor=""
             })
        "budget"->BudgetEditor(data,onDismiss={editor=""},onSave={cat,amount,month->if(vm.mutate{it.addBudget(cat,amount,month)})editor=""})
        "event"->EventEditor(initialDate=eventDate,onDismiss={editor=""},onSave={title,date,time,notes->if(vm.mutate{it.addEvent(title,date,time,notes)})editor=""})
    }
    askDelete?.let{(kind,id)->
        AlertDialog(onDismissRequest={askDelete=null},title={Text("Excluir registro?")},
            text={Text(when {
                kind=="transactions" && data.movements().any{it.id==id&&it.kind=="cardPayment"} ->
                    "Excluir este pagamento fará a fatura voltar a ficar em aberto e devolverá o valor ao saldo da conta utilizada. Confirme apenas se o pagamento foi registrado incorretamente."
                kind=="transactions" && data.movements().any{it.id==id&&it.kind=="card"} ->
                    "A compra será removida dos lançamentos, alterando a fatura, o limite do cartão e o consumo. Se for parcelada, somente a parcela selecionada será excluída."
                else -> "Esta ação altera os saldos e pode ser sincronizada com a sua conta. Confira antes de continuar."
            })},
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
    LazyColumn(contentPadding=PaddingValues(start=17.dp,top=19.dp,end=17.dp,bottom=128.dp),
        verticalArrangement=Arrangement.spacedBy(18.dp)) {
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
        item { MunnyWalletSummary(data,onOpenAccounts=onAccount) }
        item { Text("Resumo do mês",fontWeight=FontWeight.Bold,
            style=MaterialTheme.typography.titleMedium,color=Ink) }
        item {Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            NumberTile("Entradas do mês",monthly.income,"Recebidos",Modifier.weight(1f),positive=true)
            NumberTile("Gastos do mês",monthly.consumption,"Despesas + cartão",Modifier.weight(1f),positive=false)
        }}
        item { Text("Evolução financeira",fontWeight=FontWeight.Bold,
            style=MaterialTheme.typography.titleMedium,color=Ink) }
        item {MonthlyFinancePanel(data)}
        item { Text("Para onde foi o dinheiro",fontWeight=FontWeight.Bold,
            style=MaterialTheme.typography.titleMedium,color=Ink) }
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
@Composable fun MovementRow(m:MoneyMovement,onEdit:(()->Unit)?=null,
    onDelete:(()->Unit)?=null,data:MunnyDocument?=null){
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
                data?.let{doc->
                    val origin=when {
                        m.cardId.isNotBlank() -> doc.cards().find{it.id==m.cardId}?.name
                        m.accountId.isNotBlank() -> doc.accounts().find{it.id==m.accountId}?.name
                        else -> null
                    }
                    val destination=if(m.kind=="transfer")
                        doc.accounts().find{it.id==m.toAccountId}?.name else null
                    val detail=when {
                        destination!=null -> "${origin?:"Conta"} → $destination"
                        origin!=null && m.kind=="card" && m.invoiceMonth.isNotBlank() ->
                            "$origin · Fatura ${m.invoiceMonth}"
                        origin!=null -> origin
                        else -> ""
                    }
                    if(detail.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(detail,color=Muted,style=MaterialTheme.typography.labelSmall,
                            maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                }
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
    val entries=data.movements().sortedWith(compareByDescending<MoneyMovement>{it.date}.thenByDescending{it.id})
    var filter by remember {mutableStateOf("all")}
    var query by remember {mutableStateOf("")}
    var source by remember {mutableStateOf("all")}
    var status by remember {mutableStateOf("all")}
    var monthFilter by remember {mutableStateOf(false)}
    var showMix by remember {mutableStateOf(false)}
    val accounts=data.accounts()
    val cards=data.cards()
    val sourceOptions=listOf("all" to "Todas as origens")+
        accounts.map{("account:"+it.id) to it.name}+
        cards.map{("card:"+it.id) to it.name}
    val shown=entries.filter{m->
        (filter=="all"||m.kind==filter) &&
        (status=="all"||m.status==status) &&
        (!monthFilter||m.date.startsWith(monthNow)) &&
        (source=="all"||
            (source.startsWith("account:") &&
                (m.accountId==source.removePrefix("account:")||
                    m.toAccountId==source.removePrefix("account:"))) ||
            (source.startsWith("card:") && m.cardId==source.removePrefix("card:"))) &&
        (query.isBlank()||m.title.contains(query,true)||m.category.contains(query,true))
    }
    val incoming=shown.filter{it.kind=="income"&&it.status=="paid"}.sumOf{it.amount}
    val expenses=shown.filter{it.kind=="expense"&&it.status=="paid"}.sumOf{it.amount}
    val cardConsumption=shown.filter{it.kind=="card"}.sumOf{it.amount}
    LazyColumn(
        contentPadding=PaddingValues(start=17.dp,top=18.dp,end=17.dp,bottom=128.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp)
    ){
        item {PageHeader("Lançamentos","Consulte por tipo, conta, cartão, período e situação.")}
        item {SheetCard(Modifier.fillMaxWidth()) {
            Text("Visão dos registros filtrados",fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Entradas recebidas",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(5.dp))
                    Text(safeMoney(incoming),color=Sage,fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium,maxLines=1)
                }
                Column(Modifier.weight(1f)) {
                    Text("Consumo registrado",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(5.dp))
                    Text(safeMoney(expenses+cardConsumption),color=Ink,fontWeight=FontWeight.Bold,
                        style=MaterialTheme.typography.titleMedium,maxLines=1)
                }
            }
            Spacer(Modifier.height(11.dp))
            Text("Consumo = despesas diretas pagas + compras em cartão. Pagamentos de fatura e transferências não são somados novamente.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }}
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Composição dos gastos",fontWeight=FontWeight.Bold)
                    Text("Mini dashboard por meio",color=Muted,
                        style=MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick={showMix=!showMix}) {
                    Text(if(showMix)"Ocultar" else "Exibir")
                    Icon(if(showMix)Icons.Default.ExpandLess else Icons.Default.ExpandMore,null)
                }
            }
        }
        if(showMix)item { SpendingTypeDashboard(data,onSelectKind={filter=it}) }
        item {
            OutlinedTextField(query,{query=it},
                Modifier.fillMaxWidth(),label={Text("Pesquisar descrição ou categoria")},
                leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true,
                shape=RoundedCornerShape(18.dp))
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Período",Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
                FilterChip(selected=!monthFilter,onClick={monthFilter=false},label={Text("Todo histórico")})
                Spacer(Modifier.width(7.dp))
                FilterChip(selected=monthFilter,onClick={monthFilter=true},label={Text("Este mês")})
            }
        }
        item {
            Text("Tipo de lançamento",fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Todos","income" to "Entradas","expense" to "Despesas",
                    "card" to "Cartão","transfer" to "Transferências",
                    "cardPayment" to "Fatura paga").forEach{(id,label)->
                    FilterChip(selected=filter==id,onClick={filter=id},label={Text(label)})
                }
            }
        }
        item {
            Text("Conta ou cartão",fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                sourceOptions.forEach{(id,label)->
                    FilterChip(selected=source==id,onClick={source=id},
                        label={Text(label,maxLines=1)})
                }
            }
        }
        item {
            Text("Situação",fontWeight=FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("all" to "Todas","paid" to "Confirmados","planned" to "Previstos")
                    .forEach{(id,label)->
                        FilterChip(selected=status==id,onClick={status=id},
                            label={Text(label)})
                    }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("${shown.size} lançamento(s) encontrados",fontWeight=FontWeight.Bold,
                    modifier=Modifier.weight(1f))
                TextButton(onClick=onNew) {Icon(Icons.Default.Add,null);Text("Novo")}
            }
        }
        if(shown.isEmpty())item {SheetCard{Text("Nenhum lançamento encontrado. Ajuste os filtros.",color=Muted)}}
        shown.groupBy{it.date}.forEach{(date,movements)->
            item(key="date:${date}") {
                Text(date,color=Muted,fontWeight=FontWeight.SemiBold,
                    style=MaterialTheme.typography.labelMedium,
                    modifier=Modifier.padding(top=5.dp,bottom=1.dp))
            }
            items(movements,key={it.id}){movement->
                MovementRow(movement,onEdit={onEdit(movement.id)},
                    onDelete={onDelete(movement.id)},data=data)
            }
        }
    }
}
@Composable fun AccountsScreen(data:MunnyDocument,onNewAccount:()->Unit,onEditAccount:(String)->Unit,
    onNewCard:()->Unit,onEditCard:(String)->Unit,onPay:(String)->Unit,
    onDeleteAccount:(String)->Unit,onDeleteCard:(String)->Unit,
    onEditMovement:(String)->Unit,onDeleteMovement:(String)->Unit) {
    val accounts=data.accounts();val cards=data.cards();val moves=data.movements();val ex=data.exceptions()
    val bills=FinanceMath.invoices(cards,moves,ex,LocalDate.now().plusYears(2).toString())
    LazyColumn(contentPadding=PaddingValues(start=17.dp,top=18.dp,end=17.dp,bottom=116.dp),
        verticalArrangement=Arrangement.spacedBy(17.dp)){
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
        item {
            MunnyCardStack(data,onEdit=onEditCard,onDelete=onDeleteCard,onPay=onPay,
                onEditMovement=onEditMovement,onDeleteMovement=onDeleteMovement)
        }
        item{Spacer(Modifier.height(25.dp))}
    }
}
@Composable fun PlanningScreen(data:MunnyDocument,onGoal:()->Unit,
    onEditGoal:(String)->Unit,onBudget:()->Unit) {
    var section by remember { mutableStateOf("health") }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal=17.dp,vertical=10.dp)) {
            PageHeader("Planejar","Saúde financeira, investimentos, metas e limites.")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ) {
                listOf(
                    "health" to "Saúde",
                    "invest" to "Simulador",
                    "goals" to "Metas e limites"
                ).forEach { (key,label) ->
                    FilterChip(
                        selected=section==key,
                        onClick={section=key},
                        label={Text(label)},
                        leadingIcon=if(section==key) {{
                            Icon(Icons.Default.Check,contentDescription=null,modifier=Modifier.size(16.dp))
                        }} else null
                    )
                }
            }
        }
        when(section) {
            "health" -> LazyColumn(
                modifier=Modifier.fillMaxSize(),
                contentPadding=PaddingValues(start=17.dp,end=17.dp,bottom=120.dp),
                verticalArrangement=Arrangement.spacedBy(13.dp)
            ) {
                item { Column { MunnyFinancialHealth(data) } }
                item { SheetCard {
                    Text("Saldo projetado até o fim do mês",color=Muted)
                    val forecast=FinanceMath.projected(
                        data.accounts(),data.cards(),data.movements(),
                        data.exceptions(),YearMonth.now().atEndOfMonth().toString()
                    )
                    BigMoney(forecast)
                    Text("Projeção de caixa com previsões e vencimentos. Não representa patrimônio total.",
                        color=Muted,style=MaterialTheme.typography.labelSmall)
                } }
            }
            "invest" -> LazyColumn(
                modifier=Modifier.fillMaxSize(),
                contentPadding=PaddingValues(start=17.dp,end=17.dp,bottom=120.dp)
            ) { item { MunnyInvestmentSimulator(data) } }
            else -> MunnyLegacyPlanningScreen(data,onGoal,onEditGoal,onBudget)
        }
    }
}

@Composable private fun MunnyLegacyPlanningScreen(data:MunnyDocument,onGoal:()->Unit,
    onEditGoal:(String)->Unit,onBudget:()->Unit) {
    val accounts=data.accounts();val moves=data.movements();val ex=data.exceptions()
    val end=YearMonth.now().atEndOfMonth().toString()
    val estimate=FinanceMath.projected(accounts,data.cards(),moves,ex,end)
    val consumption=FinanceMath.month(moves,ex,monthNow).consumption
    val historical=remember(data) {
        MunnyPlanningMath.health(data.accounts(),data.cards(),data.movements(),
            data.exceptions()).illustrativeContribution
    }
    LazyColumn(contentPadding=PaddingValues(17.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
        item {PageHeader("Metas e limites","Orçamentos, objetivos e lançamentos previstos.")}
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
        if(data.objects("goals").isEmpty())item {
            SheetCard {
                Text("Sem metas cadastradas.",color=Muted)
                Text("Crie uma meta para acompanhar o que já reservou e simular aportes mensais.",
                    color=Muted,style=MaterialTheme.typography.labelSmall)
            }
        }
        items(data.objects("goals"),key={it.getString("id")}){goal ->
            MunnyGoalDashboard(goal,onEdit={onEditGoal(goal.optString("id"))},
                historicalContribution=historical)
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
            Text("Widgets para a tela inicial",fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Três widgets nativos: Saldo, Cartão × Despesas e Faturas em aberto.",
                color=Muted)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Exibir valores nos widgets",fontWeight=FontWeight.SemiBold)
                    Text("Desativado por padrão. O botão de olho do app continua podendo ocultar todos os valores.",
                        color=Muted,style=MaterialTheme.typography.labelSmall)
                }
                Switch(checked=vm.widgetAmountsVisible,
                    onCheckedChange=vm::updateWidgetAmountsVisible)
            }
            Spacer(Modifier.height(9.dp))
            Text("Para adicionar: toque e segure a tela inicial do Android → Widgets → Munny.",
                color=Sage,style=MaterialTheme.typography.bodySmall)
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
