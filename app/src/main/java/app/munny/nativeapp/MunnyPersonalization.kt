package app.munny.nativeapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

data class HomeModule(val id:String,val title:String,val description:String)
object HomeModules {
    val all=listOf(
        HomeModule("balance","Saldo disponível","Total atual de contas e carteira"),
        HomeModule("monthly","Resumo do mês","Recebimentos e consumo do mês"),
        HomeModule("attention","Precisa de atenção","Faturas próximas ou em atraso"),
        HomeModule("cards","Cartões","Resumo do limite consumido e disponível"),
        HomeModule("budgets","Orçamentos","Uso dos limites por categoria"),
        HomeModule("goals","Metas","Progresso dos seus objetivos"),
        HomeModule("forecast","Projeção","Saldo estimado no fim do mês"),
        HomeModule("agenda","Agenda","Próximo evento cadastrado"),
        HomeModule("shortcuts","Atalhos","Registrar e consultar lançamentos")
    )
    val ids=all.map{it.id}
    fun normalize(order:List<String>)=order.filter{it in ids}.distinct().let { it+ids.filterNot(it::contains) }
    fun move(order:List<String>,id:String,target:String):List<String> {
        val result=normalize(order).toMutableList()
        val to=result.indexOf(target)
        if(to>=0 && id!=target) { result.remove(id);result.add(to,id) }
        return result
    }
}

@Composable fun ThemeTonePicker(vm:MunnyViewModel) {
    Text("Tons do app",fontWeight=FontWeight.SemiBold,color=Ink)
    Text("O tom muda os fundos e os destaques. A chuva Matrix mantém sua identidade.",
        color=Muted,style=MaterialTheme.typography.labelSmall)
    listOf(
        Triple("mint","Menta",Color(0xFF40D9A6)),Triple("ocean","Oceano",Color(0xFF74CFFF)),
        Triple("violet","Violeta",Color(0xFFC6AEFF)),Triple("amber","Âmbar",Color(0xFFF3C776))
    ).chunked(2).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        row.forEach{(id,label,color)->FilterChip(selected=vm.themeTone==id,onClick={vm.updateThemeTone(id)},
            modifier=Modifier.weight(1f),label={Text(label)},leadingIcon={
                Surface(Modifier.size(16.dp),color=color,shape=RoundedCornerShape(50)){}
            })}
    }}
}

/** Draft layout: drag by the handle; saving changes only local presentation preferences. */
@Composable fun HomeLayoutEditor(vm:MunnyViewModel,onDismiss:()->Unit) {
    var order by remember { mutableStateOf(vm.homeOrder) }
    var hidden by remember { mutableStateOf(vm.homeHidden) }
    val list=rememberLazyListState()
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var edgeScroll by remember { mutableFloatStateOf(0f) }
    val currentOrder by rememberUpdatedState(order)
    fun finishDrag(){dragging=null;dragOffset=0f;edgeScroll=0f}
    LaunchedEffect(dragging) {
        while(dragging!=null) {
            withFrameNanos { }
            if(edgeScroll!=0f) dragOffset+=list.scrollBy(edgeScroll)
        }
    }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=Canvas) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Personalizar Início",style=MaterialTheme.typography.titleLarge,color=Ink)
                        Text("Segure o puxador e arraste para reorganizar.",color=Muted,
                            style=MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick=onDismiss){Icon(Icons.Default.Close,"Fechar")}
                }
                Text("Escolha os blocos que aparecem no seu HUD. As setas também permitem mudar a ordem.",
                    Modifier.padding(horizontal=16.dp),color=Muted,style=MaterialTheme.typography.bodySmall)
                LazyColumn(state=list,modifier=Modifier.weight(1f),
                    contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(order,key={it}){id->
                        val module=HomeModules.all.first{it.id==id}
                        val index=order.indexOf(id)
                        val active=dragging==id
                        Surface(shape=RoundedCornerShape(16.dp),color=if(active)SageSoft else Panel,
                            border=BorderStroke(1.dp,if(active)Sage else Line),
                            shadowElevation=if(active)8.dp else 0.dp,
                            modifier=Modifier.fillMaxWidth().zIndex(if(active)1f else 0f)
                                .offset{IntOffset(0,if(active)dragOffset.roundToInt()else 0)}) {
                            Row(Modifier.heightIn(min=100.dp).padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                                Icon(Icons.Default.DragIndicator,"Segure e arraste ${module.title}",tint=Sage,
                                    modifier=Modifier.size(44.dp).pointerInput(id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart={dragging=id;dragOffset=0f},
                                            onDragEnd={finishDrag()},onDragCancel={finishDrag()},
                                            onDrag={change,amount->
                                                change.consume();dragOffset+=amount.y
                                                val info=list.layoutInfo.visibleItemsInfo.firstOrNull{it.key==id}
                                                if(info!=null) {
                                                    val center=info.offset+dragOffset+info.size/2f
                                                    val target=list.layoutInfo.visibleItemsInfo.firstOrNull {
                                                        it.key!=id && center>=it.offset && center<=it.offset+it.size
                                                    }
                                                    if(target!=null) {
                                                        order=HomeModules.move(currentOrder,id,target.key as String)
                                                        dragOffset+=info.offset-target.offset
                                                    }
                                                    edgeScroll=when {
                                                        center<list.layoutInfo.viewportStartOffset+70 -> -12f
                                                        center>list.layoutInfo.viewportEndOffset-70 -> 12f
                                                        else -> 0f
                                                    }
                                                }
                                            })
                                    })
                                Column(Modifier.weight(1f).padding(horizontal=4.dp)) {
                                    Text(module.title,fontWeight=FontWeight.SemiBold,color=Ink)
                                    Text(module.description,color=Muted,style=MaterialTheme.typography.labelSmall)
                                }
                                Column {
                                    IconButton(onClick={order=HomeModules.move(order,id,order[index-1])},
                                        enabled=index>0,modifier=Modifier.size(36.dp)) {
                                        Icon(Icons.Default.KeyboardArrowUp,"Subir ${module.title}")
                                    }
                                    IconButton(onClick={order=HomeModules.move(order,id,order[index+1])},
                                        enabled=index<order.lastIndex,modifier=Modifier.size(36.dp)) {
                                        Icon(Icons.Default.KeyboardArrowDown,"Descer ${module.title}")
                                    }
                                }
                                Switch(checked=id !in hidden,onCheckedChange={show->
                                    hidden=if(show)hidden-id else hidden+id
                                })
                            }
                        }
                    }
                }
                HorizontalDivider(color=Line)
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick={order=HomeModules.ids;hidden=emptySet()}){Text("Restaurar padrão")}
                    Spacer(Modifier.weight(1f))
                    Button(onClick={vm.updateHomeLayout(order,hidden);onDismiss()}){Text("Salvar HUD")}
                }
            }
        }
    }
}
