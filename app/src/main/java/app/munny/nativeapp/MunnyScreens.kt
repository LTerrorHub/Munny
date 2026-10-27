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
        shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Panel),
        border=BorderStroke(1.dp,Line.copy(alpha=.72f)),
        elevation=CardDefaults.cardElevation(defaultElevation=1.dp),
        content={Column(Modifier.padding(17.dp),content=content)})
}
@Composable fun PageHeader(title:String,subtitle:String) {
    Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold,color=Ink)
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
    var movementKind by remember { mutableStateOf("expense") }
    var editingGoalId by remember { mutableStateOf<String?>(null) }
    var askDelete by remember { mutableStateOf<Pair<String,String>?>(null) }
    var customizeHome by remember { mutableStateOf(false) }
    val savedTabs=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
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
        floatingActionButton={if(vm.activeTab=="transactions") {
            ExtendedFloatingActionButton(onClick={editor="movement";editId=null;movementKind="expense"},containerColor=Sage,contentColor=MaterialTheme.colorScheme.onPrimary,
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
            savedTabs.SaveableStateProvider(activeTab) {
            when(activeTab) {
                "home" -> HomeHud(vm,onNew={editor="movement";editId=null;movementKind="expense"},onCustomize={customizeHome=true})
                "transactions" -> TransactionWorkspace(data,onEdit={editor="movement";editId=it},
                    onDelete={askDelete="transactions" to it})
                "accounts" -> AccountsScreen(data,onNewAccount={editor="account";editId=null},onEditAccount={editor="account";editId=it},
                    onNewCard={editor="card";editId=null},onEditCard={editor="card";editId=it},onPay={editor="invoice";editId=it},
                    onDeleteAccount={askDelete="accounts" to it},onDeleteCard={askDelete="cards" to it},
                    onTransfer={editor="movement";editId=null;movementKind="transfer"})
                "planning" -> PlanningScreen(data,onGoal={editingGoalId=null;editor="goal"},
                     onEditGoal={editingGoalId=it;editor="goal"},onBudget={editor="budget"})
                "agenda" -> MunnyAgendaScreen(data,onNewEvent={eventDate=it;editId=null;editor="event"},
                    onEditEvent={editId=it;editor="event"})
                "settings" -> SettingsScreen(vm,onExport=onExport,onImport=onImport,onCustomizeHome={customizeHome=true})
            }
            }
            }
        }
    }
    }
    }
    if(customizeHome)HomeLayoutEditor(vm,onDismiss={customizeHome=false})
    when(editor){
        "movement"->MovementEditor(data,editId,initialKind=movementKind,onDismiss={editor=""},onSave={m,notes,parts->
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
        "event"->EventEditor(data,editId,initialDate=eventDate,onDismiss={editor=""},onSave={title,date,time,notes->if(vm.mutate{it.upsertEvent(editId,title,date,time,notes)})editor=""})
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
    var menu by remember{mutableStateOf(false)}
    SheetCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment=Alignment.CenterVertically){
            Surface(shape=RoundedCornerShape(12.dp),color=if(m.kind=="income")SageSoft else if(m.kind=="card")Lilac else Canvas) {
                Icon(if(m.kind=="income")Icons.Default.SouthWest else if(m.kind=="card")Icons.Default.CreditCard else Icons.Default.NorthEast,
                    null,tint=if(m.kind=="income")Sage else Ink,modifier=Modifier.padding(11.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)){
                Text(m.title, fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
                val kindName=when(m.kind){"card"->"Cartão";"expense"->"Despesa";"transfer"->"Transferência interna";"cardPayment"->"Fatura paga";else->"Entrada"}
                Text("${kindName} · ${m.category}",
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
            Column(horizontalAlignment=Alignment.End,modifier=Modifier.widthIn(max=150.dp)) {
                Text("${if(m.kind=="income")"+" else if(m.kind=="transfer")"⇄" else "−"} ${safeMoney(m.amount)}",
                    color=if(m.kind=="income")Sage else if(m.kind=="transfer")Muted else Coral,fontWeight=FontWeight.Bold)
                Surface(shape=RoundedCornerShape(8.dp),color=if(m.status=="paid")SageSoft else MaterialTheme.colorScheme.surfaceVariant) {
                    Text(if(m.status=="paid")"Confirmado" else "Previsto",Modifier.padding(horizontal=7.dp,vertical=3.dp),
                        color=if(m.status=="paid")Sage else MatrixOrange,style=MaterialTheme.typography.labelSmall)
                }
                if(onEdit!=null || onDelete!=null)Box {
                    IconButton(onClick={menu=true},modifier=Modifier.size(32.dp)){Icon(Icons.Default.MoreHoriz,"Ações de ${m.title}",tint=Muted)}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}) {
                        if(onEdit!=null)DropdownMenuItem(text={Text("Editar")},onClick={menu=false;onEdit()})
                        if(onDelete!=null)DropdownMenuItem(text={Text("Excluir",color=Coral)},onClick={menu=false;onDelete()})
                    }
                }
            }
        }
    }
}
@Composable fun AccountsScreen(data:MunnyDocument,onNewAccount:()->Unit,onEditAccount:(String)->Unit,
    onNewCard:()->Unit,onEditCard:(String)->Unit,onPay:(String)->Unit,
    onDeleteAccount:(String)->Unit,onDeleteCard:(String)->Unit,
    onTransfer:()->Unit) {
    val accounts=data.accounts();val cards=data.cards();val moves=data.movements();val ex=data.exceptions()
    var section by androidx.compose.runtime.saveable.rememberSaveable{mutableStateOf("accounts")}
    LazyColumn(contentPadding=PaddingValues(start=17.dp,top=18.dp,end=17.dp,bottom=116.dp),
        verticalArrangement=Arrangement.spacedBy(17.dp)){
        item {PageHeader("Contas e cartões","Dinheiro que você tem e compromissos separados.")}
        item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilterChip(selected=section=="accounts",onClick={section="accounts"},label={Text("Contas")},modifier=Modifier.weight(1f))
            FilterChip(selected=section=="cards",onClick={section="cards"},label={Text("Cartões")},modifier=Modifier.weight(1f))
        }}
        if(section=="accounts") {
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Dinheiro nas contas",fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
            TextButton(onClick=onNewAccount){Text("+ Conta")}
        }}
        if(accounts.isEmpty())item{SheetCard(Modifier.fillMaxWidth()) {Text("Cadastre uma conta e seu saldo inicial.",color=Muted)}}
        items(accounts,key={it.id}) {account ->
            val b=FinanceMath.breakdown(accounts,moves,ex,today,account.id)
            SheetCard {
                Text(account.name,fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(5.dp));BigMoney(b.total)
                Text("Valor de partida ${safeMoney(account.initial)}",color=Muted,style=MaterialTheme.typography.labelSmall)
                Row {TextButton(onClick={onEditAccount(account.id)}){Text("Editar")};TextButton(onClick={onDeleteAccount(account.id)}){Text("Excluir",color=Coral)}}
            }
        }
        item {OutlinedButton(onClick=onTransfer,modifier=Modifier.fillMaxWidth()) {
            Icon(Icons.Default.SwapHoriz,null);Spacer(Modifier.width(8.dp));Text("Transferir entre contas")
        }}
        } else {
        item {Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("Meus cartões",fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.weight(1f))
            TextButton(onClick=onNewCard){Text("+ Cartão")}
        }}
        item {
            MunnyCardStack(data,onEdit=onEditCard,onDelete=onDeleteCard,onPay=onPay)
        }
        }
        item{Spacer(Modifier.height(25.dp))}
    }
}
@Composable fun PlanningScreen(data:MunnyDocument,onGoal:()->Unit,
    onEditGoal:(String)->Unit,onBudget:()->Unit) {
    var section by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("goals") }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal=17.dp,vertical=10.dp)) {
            PageHeader("Planejar","Saúde financeira, investimentos, metas e limites.")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(7.dp)
            ) {
                listOf(
                    "goals" to "Metas e limites",
                    "health" to "Saúde",
                    "invest" to "Simulador"
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
                if(LocalMoneyVisible.current) {
                    Spacer(Modifier.height(7.dp));LinearProgressIndicator(progress={if(limit>0)(total.toFloat()/limit).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth())
                }
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
        item{Spacer(Modifier.height(30.dp))}
    }
}
@Composable fun SettingsScreen(vm:MunnyViewModel,onExport:()->Unit,onImport:()->Unit,onCustomizeHome:()->Unit){
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
            ThemeTonePicker(vm)
            HorizontalDivider(Modifier.padding(vertical=12.dp),color=Line)
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
        item {SheetCard(Modifier.fillMaxWidth()) {
            Text("Seu painel inicial",fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
            Text("Escolha os resumos do HUD e arraste para mudar a ordem.",color=Muted)
            Spacer(Modifier.height(10.dp))
            Button(onClick=onCustomizeHome){Icon(Icons.Default.DashboardCustomize,null);Spacer(Modifier.width(8.dp));Text("Personalizar Início")}
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
