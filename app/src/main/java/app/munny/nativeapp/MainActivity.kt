package app.munny.nativeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model:MunnyViewModel=viewModel()
            val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {uri ->
                if(uri!=null) runCatching {
                    contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {it.write(model.exportJson())}
                }.onSuccess {model.inform("Backup JSON exportado. Guarde em local seguro: este arquivo não tem senha.")}
                  .onFailure{model.inform("Falha ao exportar: ${it.message}")}
            }
            val importer=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri ->
                if(uri!=null) runCatching {
                    contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {it.readText()}
                        ?: error("Arquivo não encontrado")
                }.onSuccess{model.importJson(it)}.onFailure{model.inform("Falha ao importar: ${it.message}")}
            }
            MunnyTheme(model.themeMode) { MunnyApp(model, onExport={export.launch("munny-backup-${java.time.LocalDate.now()}.json")},
                onImport={importer.launch(arrayOf("application/json","text/plain"))}) }
        }
    }
    override fun onResume() {
        super.onResume()
        // Não força sobrescrita. O usuário pode sincronizar manualmente na aba Mais.
    }
}
