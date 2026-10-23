package app.munny.nativeapp

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Composable private fun EditorDialog(title:String,onCancel:()->Unit,onSave:()->Unit,
                                    error:String,body:@Composable ColumnScope.()->Unit) {
    Dialog(onDismissRequest=onCancel,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(modifier=Modifier.fillMaxWidth().fillMaxHeight(.91f).padding(8.dp),shape=RoundedCornerShape(23.dp),color=Panel) {
            Column(Modifier.padding(18.dp)) {
                Text(title,style=MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(14.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp),content=body)
                if(error.isNotBlank())Text(error,color=Coral,style=MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
                    TextButton(onClick=onCancel){Text("Cancelar")}
                    Spacer(Modifier.width(8.dp));Button(onClick=onSave){Text("Salvar")}
                }
            }
        }
    }
}
@Composable private fun Entry(label:String,value:String,onValueChange:(String)->Unit, single:Boolean=true) {
    OutlinedTextField(value,onValueChange,modifier=Modifier.fillMaxWidth(),label={Text(label)},singleLine=single)
}
@Composable private fun Choices(label:String,options:List<Pair<String,String>>,value:String,onChanged:(String)->Unit) {
    var open by remember {mutableStateOf(false)}
    Column {
        Text(label,color=Muted,style=MaterialTheme.typography.labelSmall)
        Box {
            OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()) {
                Text(options.firstOrNull{it.first==value}?.second?:"Selecionar",modifier=Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false}) {
                options.forEach{(id,name)->DropdownMenuItem(text={Text(name)},onClick={onChanged(id);open=false})}
            }
        }
    }
}
private fun newMoney(v:String,allowZero:Boolean=false):Long {
    val result=parseMoney(v);require(if(allowZero) result>=0 else result>0) {"Informe um valor válido."};return result
}
@Composable fun AccountEditor(data:MunnyDocument,id:String?,onDismiss:()->Unit,onSave:(String?,String,Long)->Unit) {
    val old=data.accounts().find{it.id==id}
    var name by remember(id){mutableStateOf(old?.name?:"")}
    var amount by remember(id){mutableStateOf(old?.initial?.let{String.format(java.util.Locale("pt","BR"),"%.2f",it/100.0)}?:"")}
    var error by remember{mutableStateOf("")}
    EditorDialog(if(id==null)"Adicionar conta" else "Editar conta",onDismiss,{
        try{require(name.isNotBlank());onSave(id,name,newMoney(amount,true))}catch(t:Throwable){error=t.message?:"Confira os campos."}
    },error){
        Text("Registre o dinheiro que já existia nesta conta ANTES de começar os lançamentos.",color=Muted)
        Entry("Nome da conta ou carteira",name,{name=it})
        Entry("Dinheiro que já tenho (R$)",amount,{amount=it})
        Surface(shape=RoundedCornerShape(12.dp),color=SageSoft){Text("Não registre o mesmo dinheiro novamente como Entrada. Isso duplicaria seu saldo.",Modifier.padding(13.dp),color=Ink)}
    }
}
@Composable fun CardEditor(data:MunnyDocument,id:String?,onDismiss:()->Unit,onSave:(String?,String,Long,Int,Int)->Unit) {
    val card=data.cards().find{it.id==id}
    var name by remember(id){mutableStateOf(card?.name?:"")}
    var limit by remember(id){mutableStateOf(card?.limit?.let{String.format(java.util.Locale("pt","BR"),"%.2f",it/100.0)}?:"")}
    var closing by remember(id){mutableStateOf(card?.closing?.toString()?:"25")}
    var due by remember(id){mutableStateOf(card?.due?.toString()?:"5")}
    var error by remember{mutableStateOf("")}
    EditorDialog(if(id==null)"Adicionar cartão" else "Editar cartão",onDismiss,{
        try {onSave(id,name,newMoney(limit,true),closing.toInt(),due.toInt())}
        catch(t:Throwable){error=t.message?:"Confira as informações."}
    },error){
        Entry("Nome do cartão",name,{name=it})
        Entry("Limite (R$)",limit,{limit=it})
        Entry("Dia do fechamento (1 a 31)",closing,{closing=it})
        Entry("Dia do vencimento (1 a 31)",due,{due=it})
    }
}
@Composable fun MovementEditor(data:MunnyDocument,id:String?,onDismiss:()->Unit,onSave:(MoneyMovement,String,Int)->Unit) {
    val old=data.movements().find{it.id==id}
    val accounts=data.accounts();val cards=data.cards()
    var kind by remember(id){mutableStateOf(old?.kind?:"expense")}
    var title by remember(id){mutableStateOf(old?.title?:"")}
    var amount by remember(id){mutableStateOf(old?.amount?.let{String.format(java.util.Locale("pt","BR"),"%.2f",it/100.0)}?:"")}
    var date by remember(id){mutableStateOf(old?.date?:today)}
    var category by remember(id){mutableStateOf(old?.category?:"Outros")}
    var account by remember(id){mutableStateOf(old?.accountId?:accounts.firstOrNull()?.id.orEmpty())}
    var destination by remember(id){mutableStateOf(old?.toAccountId?:accounts.getOrNull(1)?.id.orEmpty())}
    var card by remember(id){mutableStateOf(old?.cardId?:cards.firstOrNull()?.id.orEmpty())}
    var invoiceMonth by remember(id){mutableStateOf(old?.invoiceMonth?:monthNow)}
    var installments by remember(id){mutableStateOf("1")}
    var status by remember(id){mutableStateOf(old?.status?:"paid")}
    var notes by remember(id){mutableStateOf(data.objects("transactions").find{it.optString("id")==id}?.optString("notes")?:"")}
    var repeat by remember(id){mutableStateOf(old?.repeat?:"none")}
    var error by remember(id){mutableStateOf("")}
    val selectedCard=cards.find{it.id==card}
    EditorDialog(if(old==null)"Novo lançamento" else "Editar lançamento",onDismiss,{
        try {
            val value=newMoney(amount)
            require(title.trim().isNotBlank()) {"Informe a descrição."}
            val safeDate=LocalDate.parse(date).toString()
            val safeInvoice=if(kind=="card")invoiceMonth else old?.invoiceMonth.orEmpty()
            val movement=MoneyMovement(id?:UUID.randomUUID().toString(),kind,value,safeDate,
                accountId=if(kind=="card")"" else account,toAccountId=if(kind=="transfer")destination else "",
                status=if(kind=="card")"planned" else status,
                category=if(kind=="transfer")"Outros" else category,title=title.trim(),
                cardId=if(kind=="card")card else old?.cardId.orEmpty(),invoiceMonth=safeInvoice,
                repeat=if(kind=="card")"none" else repeat)
            onSave(movement,notes,if(kind=="card"&&old==null)installments.toInt() else 1)
        } catch(t:Throwable){error=t.message?:"Confira os campos."}
    },error){
        Choices("Tipo de movimentação",listOf("income" to "Entrada","expense" to "Despesa","transfer" to "Transferência","card" to "Compra no cartão"),kind,{kind=it})
        if(kind=="income") Surface(shape=RoundedCornerShape(12.dp),color=SageSoft){Text("Entrada é dinheiro NOVO que chegou. Valores já informados como saldo inicial não são entradas.",Modifier.padding(12.dp),color=Ink)}
        Entry("Descrição",title,{title=it})
        Entry("Valor (R$)",amount,{amount=it})
        Entry("Data (AAAA-MM-DD)",date,{date=it})
        if(kind!="transfer")Choices("Categoria",data.strings("categories").map{it to it},category,{category=it})
        if(kind!="card"){
            Choices(if(kind=="income")"Conta que receberá" else "Conta de origem / pagamento",
                accounts.map{it.id to it.name},account,{account=it})
            if(kind=="transfer")Choices("Conta de destino",accounts.filter{it.id!=account}.map{it.id to it.name},destination,{destination=it})
            Choices("Situação",listOf("paid" to "Confirmado","planned" to "Previsto"),status,{status=it})
            if(kind!="transfer")Choices("Repetir",listOf("none" to "Não repetir","monthly" to "Mensal","weekly" to "Semanal","yearly" to "Anual"),repeat,{repeat=it})
        }else {
            Choices("Cartão",cards.map{it.id to it.name},card,{
                card=it
                cards.find{c->c.id==it}?.let{c->invoiceMonth=FinanceMath.invoiceMonth(c,date)}
            })
            Entry("Primeira fatura (AAAA-MM)",invoiceMonth,{invoiceMonth=it})
            if(old==null)Entry("Quantidade de parcelas (1 a 120)",installments,{installments=it})
            Text("O consumo é registrado por parcela. O saldo bancário só diminui ao pagar a fatura.",color=Muted)
        }
        Entry("Observações",notes,{notes=it},single=false)
    }
}
@Composable fun InvoiceEditor(data:MunnyDocument,key:String,onDismiss:()->Unit,onPay:(MoneyCard,Invoice,String,String)->Unit) {
    val cardId=key.substringBefore(':')
    val card=data.cards().find{it.id==cardId}
    val bill=FinanceMath.invoices(data.cards(),data.movements(),data.exceptions(),
        LocalDate.now().plusYears(3).toString()).find{it.cardId==cardId&&it.month==key.substringAfter(':')}
    var account by remember {mutableStateOf(data.accounts().firstOrNull()?.id.orEmpty())}
    var date by remember {mutableStateOf(today)}
    var error by remember{mutableStateOf("")}
    EditorDialog("Pagamento de fatura",onDismiss,{
        try {require(card!=null&&bill!=null&&bill.remaining>0);onPay(card,bill,account,LocalDate.parse(date).toString())}
        catch(t:Throwable){error=t.message?:"Falha ao pagar."}
    },error){
        if(card!=null&&bill!=null) {Text("${card.name} · ${bill.month}");BigMoney(bill.remaining)}
        Choices("Conta usada no pagamento",data.accounts().map{it.id to it.name},account,{account=it})
        Entry("Data do pagamento",date,{date=it})
        Text("Somente registre como pago após a saída real do dinheiro.",color=Muted)
    }
}
@Composable fun GoalEditor(data:MunnyDocument,id:String?,onDismiss:()->Unit,
    onSave:(String?,String,Long,Long,String)->Unit) {
    val goal=data.objects("goals").firstOrNull{it.optString("id")==id}
    var name by remember(id){mutableStateOf(goal?.optString("name")?:"")}
    var target by remember(id){mutableStateOf(goal?.optLong("target")?.let{
        String.format(java.util.Locale("pt","BR"),"%.2f",it/100.0)}?:"")}
    var saved by remember(id){mutableStateOf(goal?.optLong("saved")?.let{
        String.format(java.util.Locale("pt","BR"),"%.2f",it/100.0)}?:"0,00")}
    var deadline by remember(id){mutableStateOf(
        goal?.optString("deadline")?:LocalDate.now().plusMonths(6).toString())}
    var error by remember(id){mutableStateOf("")}
    EditorDialog(if(id==null)"Nova meta" else "Atualizar meta",onDismiss,{
        try{onSave(id,name,newMoney(target),newMoney(saved,true),LocalDate.parse(deadline).toString())}
        catch(t:Throwable){error=t.message?:"Confira os campos."}
    },error){
        Entry("Meta",name,{name=it})
        Entry("Objetivo (R$)",target,{target=it})
        Entry("Já reservado (R$)",saved,{saved=it})
        Entry("Prazo (AAAA-MM-DD)",deadline,{deadline=it})
        Text("O valor reservado é informado manualmente. Não cria transferência nem retira dinheiro da conta.",
            color=Muted,style=MaterialTheme.typography.labelSmall)
    }
}
@Composable fun BudgetEditor(data:MunnyDocument,onDismiss:()->Unit,onSave:(String,Long,String)->Unit) {
    var category by remember {mutableStateOf(data.strings("categories").firstOrNull()?:"Outros")}
    var amount by remember{mutableStateOf("")};var month by remember{mutableStateOf(monthNow)}
    var error by remember{mutableStateOf("")}
    EditorDialog("Novo limite de gastos",onDismiss,{
        try{onSave(category,newMoney(amount),YearMonth.parse(month).toString())}
        catch(t:Throwable){error=t.message?:"Confira os campos."}
    },error){
        Choices("Categoria",data.strings("categories").map{it to it},category,{category=it})
        Entry("Limite (R$)",amount,{amount=it});Entry("Mês (AAAA-MM)",month,{month=it})
    }
}
@Composable fun EventEditor(initialDate:String=today,onDismiss:()->Unit,onSave:(String,String,String,String)->Unit) {
    var title by remember{mutableStateOf("")};var date by remember(initialDate){mutableStateOf(initialDate)}
    var time by remember{mutableStateOf("09:00")};var notes by remember{mutableStateOf("")}
    var error by remember{mutableStateOf("")}
    EditorDialog("Novo evento",onDismiss,{
        try {require(time.matches(Regex("([01]\\d|2[0-3]):[0-5]\\d")));onSave(title,LocalDate.parse(date).toString(),time,notes)}
        catch(t:Throwable){error=t.message?:"Confira os campos."}
    },error){
        Entry("Nome do evento",title,{title=it});Entry("Data (AAAA-MM-DD)",date,{date=it})
        Entry("Horário (HH:MM)",time,{time=it});Entry("Notas",notes,{notes=it},single=false)
    }
}
