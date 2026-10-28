package app.munny.nativeapp

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MunnyCardMovementEditTest {
    private fun document():MunnyDocument=MunnyDocument.blank().addAccount("Conta",100_000)
        .upsertCard("c","Cartão",100_000,15,25)
        .upsertMovement(MoneyMovement("purchase","card",90_000,"2026-10-10",cardId="c",invoiceMonth="2026-10",title="Compra"),installment=3)
    @Test fun addingPurchaseCreatesInstallmentsAndDeletingOneReleasesOnlyItsLimit() {
        val doc=document()
        val card=doc.cards().single()
        assertEquals(3,doc.movements().size)
        assertEquals(90_000L,doc.movements().sumOf{it.amount})
        val first=doc.movements().first()
        val updated=doc.deleteMovement(first.id)
        assertEquals(2,updated.movements().size)
        assertFalse(updated.movements().any{it.id==first.id})
        val position=MunnyCardMath.position(card,updated.movements(),updated.exceptions(),LocalDate.parse("2026-10-27"))
        assertEquals(60_000L,position.committed)
        assertEquals(40_000L,position.available)
        assertEquals(doc.accounts(),updated.accounts())
        assertEquals(3,doc.movements().size)
    }
    @Test fun removingPurchaseKeepsOtherPurchasesAndPayments() {
        var doc=document()
        val account=doc.accounts().single().id
        doc=doc.upsertMovement(MoneyMovement("other","card",7_000,"2026-10-10",cardId="c",invoiceMonth="2026-10",title="Compra"))
            .upsertMovement(MoneyMovement("payment","cardPayment",10_000,"2026-10-20",accountId=account,status="paid",cardId="c",invoiceMonth="2026-10",title="Pagamento"))
        val next=doc.deleteMovement("purchase-1",true)
        assertEquals(setOf("other","payment"),next.movements().map{it.id}.toSet())
        assertEquals(7_000L,MunnyTransactionQuery.consumption(next.movements()))
    }
    @Test fun purchaseGroupsNeverCrossCardsOrInferFromDescription() {
        var doc=document().upsertCard("otherCard","Outro",100_000,15,25)
            .upsertMovement(MoneyMovement("different","card",8_000,"2026-10-10",cardId="otherCard",invoiceMonth="2026-10",title="Compra"))
        val row=doc.objects("transactions").first{it.optString("id")=="different"}.put("groupId","purchase")
        doc=doc.replace("transactions",row)
        val next=doc.deleteMovement("purchase-1",true)
        assertEquals(listOf("different"),next.movements().map{it.id})
        val imported=doc.objects("transactions").first{it.optString("id")=="purchase-1"}
        imported.remove("groupId")
        doc=doc.replace("transactions",imported)
        assertEquals(setOf("purchase-1"),doc.movementDeletionIds("purchase-1",true))
    }
    @Test fun deletingPaymentReturnsCashWithoutDeletingPurchase() {
        var doc=document()
        val account=doc.accounts().single().id
        doc=doc.upsertMovement(MoneyMovement("payment","cardPayment",10_000,"2026-10-20",accountId=account,status="paid",cardId="c",invoiceMonth="2026-10",title="Pagamento"))
        val before=FinanceMath.breakdown(doc.accounts(),doc.movements(),doc.exceptions(),"2026-10-27").total
        val next=doc.deleteMovement("payment")
        val after=FinanceMath.breakdown(next.accounts(),next.movements(),next.exceptions(),"2026-10-27").total
        assertEquals(10_000L,after-before)
        assertEquals(3,next.movements().size)
    }
    @Test(expected=IllegalStateException::class) fun missingMovementDoesNotPretendToBeDeleted() {
        document().deleteMovement("missing")
    }
    @Test(expected=IllegalArgumentException::class) fun cannotAddPurchaseToMissingCard() {
        MunnyDocument.blank().upsertMovement(MoneyMovement("x","card",1_000,"2026-10-10",cardId="missing",invoiceMonth="2026-10",title="Compra"))
    }
}
