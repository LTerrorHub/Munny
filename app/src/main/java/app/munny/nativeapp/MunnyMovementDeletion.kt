package app.munny.nativeapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable fun MovementDeletionDialog(data:MunnyDocument,id:String,onDismiss:()->Unit,onConfirm:(Boolean)->Unit) {
    val movement=data.movements().firstOrNull{it.id==id}
    if(movement==null) {
        AlertDialog(onDismissRequest=onDismiss,title={Text("Registro não encontrado")},
            text={Text("O lançamento já não está nesta base. Atualize a lista.")},
            confirmButton={TextButton(onClick=onDismiss){Text("Fechar")}})
        return
    }
    var whole by remember(id){mutableStateOf(false)}
    val group=remember(data,id){data.movementDeletionIds(id,true)}
    val count=if(whole)group.size else 1
    val amount=if(whole)data.movements().filter{it.id in group}.sumOf{it.amount}else movement.amount
    AlertDialog(onDismissRequest=onDismiss,
        title={Text(if(movement.kind=="card")"Excluir compra do cartão?" else "Excluir lançamento?")},
        text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(movement.title,fontWeight=FontWeight.Bold)
            if(movement.kind=="card" && group.size>1) {
                listOf(false to "Somente esta parcela",true to "Todas as ${group.size} parcelas desta compra").forEach{(value,label)->
                    Row(Modifier.fillMaxWidth().clickable{whole=value},verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=whole==value,onClick={whole=value});Text(label,Modifier.weight(1f))
                    }
                }
            }
            Text("$count registro(s) · ${safeMoney(amount)}",color=Ink)
            Text(when(movement.kind) {
                "card" -> "A compra será removida, recalculando as faturas e o limite do cartão. Pagamentos já registrados são mantidos."
                "cardPayment" -> "O pagamento será removido. A fatura voltará a ficar em aberto e o valor retornará ao saldo da conta utilizada."
                else -> "O registro será removido e os saldos relacionados serão recalculados."
            },color=Muted)
        }},confirmButton={Button(onClick={onConfirm(whole)},colors=ButtonDefaults.buttonColors(containerColor=Coral)) {
            Text(if(whole)"Excluir parcelas" else "Excluir")
        }},dismissButton={TextButton(onClick=onDismiss){Text("Cancelar")}})
}
