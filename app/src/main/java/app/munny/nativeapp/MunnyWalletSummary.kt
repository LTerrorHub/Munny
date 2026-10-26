package app.munny.nativeapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Exact per-account bank balances. Transfers cancel in the consolidated total. */
@Composable fun MunnyWalletSummary(data:MunnyDocument,onOpenAccounts:()->Unit) {
    val wallets=data.accounts()
    val entries=data.movements()
    val exceptions=data.exceptions()
    val at=today
    val positions=remember(data,at) {
        wallets.map{account->
            account to FinanceMath.breakdown(wallets,entries,exceptions,at,account.id).total
        }
    }
    val positiveSum=positions.sumOf{(_,amount)->amount.coerceAtLeast(0L)}
    var expanded by remember{mutableStateOf(false)}
    SheetCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Surface(shape=RoundedCornerShape(13.dp),color=SageSoft,modifier=Modifier.size(40.dp)) {
                Box(contentAlignment=Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet,null,tint=Sage)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Saldo por carteira",fontWeight=FontWeight.Bold,
                    style=MaterialTheme.typography.titleMedium)
                Text("O total da home soma todas as suas contas",
                    color=Muted,style=MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick=onOpenAccounts){Text("Contas")}
        }
        Spacer(Modifier.height(14.dp))
        if(positions.isEmpty()) {
            Text("Cadastre contas para acompanhar o saldo separado de cada carteira.",
                color=Muted)
        } else {
            val displayed=if(expanded)positions else positions.take(3)
            displayed.forEachIndexed{index,(account,amount)->
                if(index>0)Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(account.name,modifier=Modifier.weight(1f),
                        maxLines=1,overflow=TextOverflow.Ellipsis,
                        color=Ink,fontWeight=FontWeight.SemiBold)
                    Spacer(Modifier.width(12.dp))
                    Text(safeMoney(amount),color=if(amount<0L)Coral else Ink,
                        fontWeight=FontWeight.Bold)
                }
                Spacer(Modifier.height(7.dp))
                val share=if(positiveSum<=0L)0f
                    else (amount.coerceAtLeast(0L).toDouble()/positiveSum).toFloat().coerceIn(0f,1f)
                LinearProgressIndicator(progress={share},
                    color=if(amount<0L)Coral else Sage,
                    trackColor=MaterialTheme.colorScheme.surfaceVariant,
                    modifier=Modifier.fillMaxWidth().height(7.dp))
            }
            if(positions.size>3) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick={expanded=!expanded}) {
                    Text(if(expanded)"Mostrar menos" else "Exibir todas as ${positions.size} carteiras")
                    Icon(if(expanded)Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription=null)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("A barra compara apenas saldos positivos entre as carteiras. Saldos negativos continuam visíveis, mas não são tratados como participação positiva.",
                color=Muted,style=MaterialTheme.typography.labelSmall)
        }
    }
}
