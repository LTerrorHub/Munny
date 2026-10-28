package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test

class MunnyEventEditTest {
    @Test fun editingPastEventPreservesIdExtraFieldsAndFinancialData() {
        val original=MunnyDocument.blank().addAccount("Conta",100_000).addEvent("Antigo","2001-01-01","09:00","Notas")
        val event=original.objects("events").single().put("extra","preservar").put("repeat","monthly")
        val withExtra=original.replace("events",event)
        val id=event.getString("id")
        val edited=withExtra.upsertEvent(id,"Corrigido","2000-12-31","08:30","Revisado")
        val saved=edited.objects("events").single()
        assertEquals(id,saved.getString("id"))
        assertEquals("2000-12-31",saved.getString("date"))
        assertEquals("Corrigido",saved.getString("title"))
        assertEquals("08:30",saved.getString("time"))
        assertEquals("Revisado",saved.getString("notes"))
        assertEquals("preservar",saved.getString("extra"))
        assertEquals("monthly",saved.getString("repeat"))
        assertEquals(withExtra.accounts(),edited.accounts())
        assertEquals(withExtra.movements(),edited.movements())
    }
    @Test fun eventCanMoveToFutureWithoutCreatingDuplicate() {
        val doc=MunnyDocument.blank().addEvent("Evento","2000-01-01","","")
        val id=doc.objects("events").single().getString("id")
        val next=doc.upsertEvent(id,"Futuro","2099-12-31","23:59","Novo")
        assertEquals(1,next.objects("events").size)
        assertEquals("2099-12-31",next.objects("events").single().getString("date"))
        assertEquals("2000-01-01",doc.objects("events").single().getString("date"))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidTimeIsRejected() {
        MunnyDocument.blank().addEvent("Inválido","2026-10-27","24:00","")
    }
    @Test(expected=IllegalArgumentException::class) fun missingEventCannotSilentlyBecomeAnotherEvent() {
        MunnyDocument.blank().upsertEvent("missing","Evento","2026-10-27","09:00","")
    }
}
