package app.munny.nativeapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val filterSaver=listSaver<TransactionFilter,String>(
    save={listOf(it.kind,it.source,it.status,it.period,it.from,it.to)},
    restore={TransactionFilter(it[0],it[1],it[2],it[3],it[4],it[5])})
private val kindOptions=listOf("all" to "Todos","income" to "Entradas","expense" to "Despesas",
    "card" to "Cartão","transfer" to "Transferências","cardPayment" to "Faturas pagas")
private val statusOptions=listOf("all" to "Todas","paid" to "Confirmados","planned" to "Previstos")

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TransactionWorkspace(data:MunnyDocument,onEdit:(String)->Unit,onDelete:(String)->Unit) {
    var filter by rememberSaveable(stateSaver=filterSaver){mutableStateOf(TransactionFilter())}
    var query by rememberSaveable{mutableStateOf("")}
    var month by rememberSaveable{mutableStateOf(monthNow)}
    var oldest by rememberSaveable{mutableStateOf(false)}
    var section by rememberSaveable{mutableStateOf("records")}
    var openFilters by remember{mutableStateOf(false)}
    val rows=remember(data,filter,query,month,oldest){
        MunnyTransactionQuery.select(data.movements(),data.exceptions(),filter,month,query,oldest)
    }
    val sources=listOf("all" to "Todas as origens")+data.accounts().map{"account:${it.id}" to it.name}+
        data.cards().map{"card:${it.id}" to it.name}
    val count=listOf(filter.kind!="all",filter.source!="all",filter.status!="all",filter.period!="month").count{it}
    LazyColumn(contentPadding=PaddingValues(17.dp,16.dp,17.dp,116.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        item {PageHeader("Lançamentos","Registros e análises do seu dinheiro.")}
        item {
            Surface(color=Panel,shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Line)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    IconButton(onClick={month=YearMonth.parse(month).minusMonths(1).toString();filter=filter.copy(period="month")}) {
                        Icon(Icons.Default.ChevronLeft,"Mês anterior")
                    }
                    Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally) {
                        Text(when(filter.period){"all"->"Todo o histórico";"custom"->"Período personalizado";
                            else->YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMMM yyyy",Locale("pt","BR")))},
                            fontWeight=FontWeight.SemiBold,color=Ink)
                        if(filter.period=="custom")Text("${filter.from} a ${filter.to}",color=Muted,style=MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick={month=YearMonth.parse(month).plusMonths(1).toString();filter=filter.copy(period="month")}) {
                        Icon(Icons.Default.ChevronRight,"Próximo mês")
                    }
                }
            }
        }
        item {OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),
            placeholder={Text("Buscar descrição ou categoria")},leadingIcon={Icon(Icons.Default.Search,null)},
            trailingIcon=if(query.isNotBlank()){{IconButton(onClick={query=""}){Icon(Icons.Default.Close,"Limpar busca")}}}else null,
            singleLine=true,shape=RoundedCornerShape(16.dp),
            colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Panel,focusedContainerColor=Panel))}
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick={openFilters=true},modifier=Modifier.weight(1f)) {
                    Icon(Icons.Default.Tune,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                    Text(if(count==0)"Filtros" else "Filtros · $count")
                }
                TextButton(onClick={oldest=!oldest},modifier=Modifier.weight(1f)) {
                    Icon(Icons.Default.Sort,null,Modifier.size(18.dp));Spacer(Modifier.width(4.dp))
                    Text(if(oldest)"Mais antigos" else "Mais recentes",maxLines=1)
                }
            }
            val chips=mutableListOf<Pair<String,()->Unit>>()
            if(filter.kind!="all")chips.add((kindOptions.find{it.first==filter.kind}?.second?:filter.kind) to {filter=filter.copy(kind="all")})
            if(filter.source!="all")chips.add((sources.find{it.first==filter.source}?.second?:"Origem") to {filter=filter.copy(source="all")})
            if(filter.status!="all")chips.add((statusOptions.find{it.first==filter.status}?.second?:filter.status) to {filter=filter.copy(status="all")})
            if(chips.isNotEmpty())Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                chips.forEach{(label,clear)->InputChip(selected=true,onClick=clear,label={Text(label)},
                    trailingIcon={Icon(Icons.Default.Close,"Remover filtro $label",Modifier.size(15.dp))})}
            }
        }
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(selected=section=="records",onClick={section="records"},label={Text("Registros")},
                    leadingIcon={Icon(Icons.Default.ReceiptLong,null,Modifier.size(18.dp))},modifier=Modifier.weight(1f))
                FilterChip(selected=section=="analytics",onClick={section="analytics"},label={Text("Análises")},
                    leadingIcon={Icon(Icons.Default.BarChart,null,Modifier.size(18.dp))},modifier=Modifier.weight(1f))
            }
        }
        item {SheetCard(Modifier.fillMaxWidth()) {
            Text("${rows.size} registro(s) no filtro",color=Muted,style=MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Recebido",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Text(safeMoney(MunnyTransactionQuery.income(rows)),color=Sage,fontWeight=FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Consumo",color=Muted,style=MaterialTheme.typography.labelSmall)
                    Text(safeMoney(MunnyTransactionQuery.consumption(rows)),color=Coral,fontWeight=FontWeight.Bold)
                }
            }
        }}
        if(section=="analytics") {
            item {FilteredTransactionCharts(rows)}
        } else {
            if(rows.isEmpty())item {SheetCard{Text("Nenhum lançamento encontrado. Ajuste os filtros ou adicione um registro.",color=Muted)}}
            rows.groupBy{it.date}.forEach{(date,movements)->
                item(key="date:$date") {Text(LocalDate.parse(date).format(DateTimeFormatter.ofPattern("dd MMM yyyy",Locale("pt","BR"))),
                    color=Muted,fontWeight=FontWeight.SemiBold)}
                items(movements,key={it.id}){m->MovementRow(m,onEdit={onEdit(m.id)},onDelete={onDelete(m.id)},data=data)}
            }
        }
    }
    if(openFilters)TransactionFilterSheet(filter,sources,onDismiss={openFilters=false},onApply={filter=it;openFilters=false})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TransactionFilterSheet(initial:TransactionFilter,sources:List<Pair<String,String>>,
    onDismiss:()->Unit,onApply:(TransactionFilter)->Unit) {
    var draft by remember{mutableStateOf(initial)}
    var error by remember{mutableStateOf("")}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Canvas,
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding()) {
            Text("Filtrar lançamentos",Modifier.padding(horizontal=20.dp),color=Ink,
                style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
            Column(Modifier.weight(1f,fill=false).verticalScroll(rememberScrollState()).padding(17.dp),
                verticalArrangement=Arrangement.spacedBy(12.dp)) {
                SheetCard(Modifier.fillMaxWidth()) {
                    Text("Período",fontWeight=FontWeight.SemiBold)
                    FilterDropdown(listOf("month" to "Mês selecionado","all" to "Todo o histórico","custom" to "Personalizar datas"),
                        draft.period,{draft=draft.copy(period=it,from=draft.from.ifBlank{YearMonth.now().atDay(1).toString()},to=draft.to.ifBlank{today})})
                    if(draft.period=="custom") {
                        DateEntry("De",draft.from){draft=draft.copy(from=it)}
                        DateEntry("Até",draft.to){draft=draft.copy(to=it)}
                    }
                }
                SheetCard(Modifier.fillMaxWidth()) {
                    Text("Tipo de lançamento",fontWeight=FontWeight.SemiBold)
                    kindOptions.chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        row.forEach{(id,label)->FilterChip(selected=draft.kind==id,onClick={draft=draft.copy(kind=id)},
                            label={Text(label,style=MaterialTheme.typography.labelSmall,maxLines=1)},modifier=Modifier.weight(1f))}
                    }}
                }
                SheetCard(Modifier.fillMaxWidth()) {
                    Text("Conta ou cartão",fontWeight=FontWeight.SemiBold)
                    FilterDropdown(sources,draft.source,{draft=draft.copy(source=it)})
                }
                SheetCard(Modifier.fillMaxWidth()) {
                    Text("Situação",fontWeight=FontWeight.SemiBold)
                    FilterDropdown(statusOptions,draft.status,{draft=draft.copy(status=it)})
                }
                if(error.isNotBlank())Text(error,color=Coral)
            }
            HorizontalDivider(color=Line)
            Row(Modifier.fillMaxWidth().padding(17.dp),verticalAlignment=Alignment.CenterVertically) {
                TextButton(onClick={draft=TransactionFilter(period="all");error=""}){Text("Limpar")}
                Spacer(Modifier.weight(1f))
                Button(onClick={runCatching{draft.validate();onApply(draft)}.onFailure{error=it.message?:"Confira o período."}}){Text("Aplicar filtros")}
            }
        }
    }
}

@Composable private fun FilterDropdown(options:List<Pair<String,String>>,selected:String,onChange:(String)->Unit) {
    var expanded by remember{mutableStateOf(false)}
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)) {
            Text(options.find{it.first==selected}?.second?:"Selecionar",Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis)
            Icon(Icons.Default.ExpandMore,null)
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false},modifier=Modifier.heightIn(max=320.dp)) {
            options.forEach{(id,label)->DropdownMenuItem(text={Text(label)},onClick={onChange(id);expanded=false},
                leadingIcon=if(id==selected){{Icon(Icons.Default.Check,null,tint=Sage)}}else null)}
        }
    }
}

@Composable private fun FilteredTransactionCharts(rows:List<MoneyMovement>) {
    val visible=LocalMoneyVisible.current
    Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
        SheetCard(Modifier.fillMaxWidth()) {
            Text("Evolução no período",fontWeight=FontWeight.Bold)
            Text("Mesmos registros da lista e mesmos filtros.",color=Muted,style=MaterialTheme.typography.labelSmall)
            if(!visible)Text("Gráficos ocultos por privacidade.",color=Muted)
            else if(rows.isEmpty())Text("Sem registros para analisar.",color=Muted)
            else {
                val months=MunnyTransactionQuery.months(rows)
                val max=months.values.maxOf{maxOf(MunnyTransactionQuery.income(it),MunnyTransactionQuery.consumption(it))}.coerceAtLeast(1)
                months.entries.toList().takeLast(6).forEach{(month,entries)->
                    Spacer(Modifier.height(12.dp))
                    Text(YearMonth.parse(month).format(DateTimeFormatter.ofPattern("MMM yyyy",Locale("pt","BR"))),fontWeight=FontWeight.SemiBold)
                    ChartBar("Recebido",MunnyTransactionQuery.income(entries),max,true)
                    ChartBar("Consumo",MunnyTransactionQuery.consumption(entries),max,false)
                }
                if(months.size>6)Text("Exibindo os últimos 6 meses com registros no filtro.",color=Muted,style=MaterialTheme.typography.labelSmall)
            }
        }
        SheetCard(Modifier.fillMaxWidth()) {
            Text("Gastos por categoria",fontWeight=FontWeight.Bold)
            val groups=MunnyTransactionQuery.categories(rows)
            if(!visible)Text("Distribuição oculta por privacidade.",color=Muted)
            else if(groups.isEmpty())Text("Sem despesas confirmadas ou compras no cartão neste filtro.",color=Muted)
            else {
                val total=groups.sumOf{it.second}
                groups.forEach{(category,value)->
                    Spacer(Modifier.height(11.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(category,Modifier.weight(1f),color=Ink)
                        Text(String.format(Locale("pt","BR"),"%.1f%%",value.toDouble()*100/total),color=Muted,
                            style=MaterialTheme.typography.labelSmall)
                    }
                    ChartBar("",value,total,true)
                }
            }
        }
        SheetCard(Modifier.fillMaxWidth()) {
            Text("Meios de pagamento",fontWeight=FontWeight.Bold)
            val card=rows.filter{it.kind=="card"}.sumOf{it.amount}
            val direct=rows.filter{it.kind=="expense"&&it.status=="paid"}.sumOf{it.amount}
            val total=(card+direct).coerceAtLeast(1)
            if(visible) {
                ChartBar("Cartão",card,total,true)
                ChartBar("Despesa direta",direct,total,false)
                Text("Transferências: ${safeMoney(rows.filter{it.kind=="transfer"&&it.status=="paid"}.sumOf{it.amount})}",color=Muted)
            } else Text("Gráficos ocultos por privacidade.",color=Muted)
            Spacer(Modifier.height(8.dp))
            Text("Consumo = despesas pagas + compras no cartão. Transferências e pagamentos de fatura não entram novamente nessa soma.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun ChartBar(label:String,value:Long,max:Long,positive:Boolean) {
    Row(Modifier.fillMaxWidth().padding(top=5.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),color=Muted,style=MaterialTheme.typography.labelSmall)
        Text(safeMoney(value),fontWeight=FontWeight.SemiBold,color=if(positive)Sage else Coral,
            style=MaterialTheme.typography.bodySmall)
    }
    LinearProgressIndicator(progress={(value.toDouble()/max.coerceAtLeast(1)).toFloat().coerceIn(0f,1f)},
        modifier=Modifier.fillMaxWidth().padding(top=4.dp).height(6.dp),color=if(positive)Sage else Coral,
        trackColor=Line.copy(alpha=.4f))
}
