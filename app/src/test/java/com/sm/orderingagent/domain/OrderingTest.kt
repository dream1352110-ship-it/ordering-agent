package com.sm.orderingagent.domain

import org.junit.Assert.*
import org.junit.Test

class OrderingTest {
    @Test fun sameOrderSequentialWritesAndStaleIdentity() {
        val engine = engine()
        val identity = engine.state.currentOrderId
        engine.transaction({}) { requireOrder(identity); add("fries", 1) }
        engine.transaction({}) { requireOrder(identity); setDining("打包", 4) }
        assertEquals(identity, engine.state.currentOrderId)
        engine.startNewOrder()
        val before = engine.state
        assertEquals("ORDER_ID_CONFLICT", assertThrows(OrderingException::class.java) {
            engine.transaction({}) { requireOrder(identity); add("fries", 1) }
        }.code)
        assertEquals(before, engine.state)
    }

    @Test fun shoppingOrderIdentityPersistsUntilSubmitOrExplicitNewOrder() {
        val engine = engine()
        val orderId = engine.state.currentOrderId
        assertTrue(orderId.isNotBlank())
        engine.add("fries", 1)
        engine.setDining("打包", 4)
        engine.clear()
        assertEquals(orderId, engine.state.currentOrderId)
        engine.add("fries", 1)
        val quote = engine.quote(1000)
        assertEquals(orderId, engine.state.currentOrderId)
        val order = engine.submit(quote.id, "现金", 2000)
        assertEquals(orderId, order.id)
        val nextId = engine.state.currentOrderId
        assertNotEquals(orderId, nextId)
        val restored = OrderingEngine(DemoMenu.items, engine.state)
        assertEquals(nextId, restored.state.currentOrderId)
        assertEquals(order, restored.submit(quote.id, "现金", 2100))
        assertEquals(nextId, restored.state.currentOrderId)
        restored.startNewOrder()
        assertNotEquals(nextId, restored.state.currentOrderId)
        val before = restored.state
        assertThrows(IllegalStateException::class.java) {
            restored.transaction({ throw IllegalStateException("disk full") }) { startNewOrder() }
        }
        assertEquals(before, restored.state)
    }

    @Test fun checkoutRestoresDefaultsAndReplayPreservesNextOrder() {
        val engine = engine()
        engine.add("americano", 1)
        engine.setAllergens(setOf(Allergen.MILK))
        engine.setDining("打包", 5)
        val receipt = CommandReceipt("previous-request", "fingerprint", "{}")
        engine.recordReceipt(receipt)
        val quote = engine.quote(1000)
        val previousVersion = engine.state.version
        val order = engine.submit(quote.id, "现金", 2000)
        assertEquals("打包", order.serviceMode)
        assertEquals(5, order.partySize)
        assertEquals(setOf(Allergen.MILK), order.allergens)
        assertTrue(engine.state.cart.isEmpty())
        assertTrue(engine.state.allergens.isEmpty())
        assertEquals("堂食", engine.state.serviceMode)
        assertEquals(2, engine.state.partySize)
        assertEquals(previousVersion + 1, engine.state.version)
        assertEquals(listOf(order), engine.state.orders)
        assertEquals(listOf(receipt), engine.state.receipts)
        assertEquals("SUMMARY_NOT_FOUND", assertThrows(OrderingException::class.java) { engine.checkout(quote.id, 2100) }.code)
        val restarted = OrderingEngine(DemoMenu.items, engine.state)
        assertEquals("堂食", restarted.state.serviceMode)
        assertEquals(2, restarted.state.partySize)
        restarted.add("fries", 1)
        restarted.setDining("打包", 3)
        val nextOrderState = restarted.state
        assertEquals(order, restarted.submit(quote.id, "现金", 2200))
        assertEquals(nextOrderState, restarted.state)
    }

    @Test fun newOrderRestoresDefaultsAndInvalidatesSummaryWithoutDeletingHistory() {
        val engine = engine()
        engine.add("fries", 1)
        engine.submit(engine.quote().id, "现金")
        engine.add("americano", 1)
        engine.setAllergens(setOf(Allergen.MILK))
        engine.setDining("打包", 5)
        val quote = engine.quote()
        val receipt = CommandReceipt("old-request", "fingerprint", "{}")
        engine.recordReceipt(receipt)
        val previous = engine.state
        engine.startNewOrder()
        assertTrue(engine.state.cart.isEmpty())
        assertTrue(engine.state.allergens.isEmpty())
        assertEquals("堂食", engine.state.serviceMode)
        assertEquals(2, engine.state.partySize)
        assertEquals(previous.version + 1, engine.state.version)
        assertEquals(previous.orders, engine.state.orders)
        assertEquals(listOf(receipt), engine.state.receipts)
        assertEquals(19, engine.remainingStock("fries"))
        assertEquals("SUMMARY_NOT_FOUND", assertThrows(OrderingException::class.java) { engine.checkout(quote.id) }.code)
    }

    @Test fun submittedSummaryRemainsIdempotentAfterEngineRestart() {
        val engine = engine()
        engine.add("fries", 1)
        val quote = engine.quote(1000)
        val order = engine.submit(quote.id, "现金", 2000)
        val restarted = OrderingEngine(DemoMenu.items, engine.state)
        assertEquals(order, restarted.submit(quote.id, "现金", 900000))
        assertEquals(1, restarted.state.orders.size)
        assertEquals(19, restarted.remainingStock("fries"))
    }

    @Test fun receiptRecordingDoesNotChangeCartVersionAndRollsBackOnSaveFailure() {
        val engine = engine()
        val receipt = CommandReceipt("request-1", "fingerprint", "{}")
        assertThrows(IllegalStateException::class.java) {
            engine.transaction({ throw IllegalStateException("disk full") }) {
                add("fries", 1)
                recordReceipt(receipt)
            }
        }
        assertTrue(engine.state.cart.isEmpty())
        assertTrue(engine.state.receipts.isEmpty())
        assertEquals(0L, engine.state.version)
        engine.recordReceipt(receipt)
        assertEquals(0L, engine.state.version)
        assertEquals(listOf(receipt), engine.state.receipts)
    }

    @Test fun preparingCheckoutIsReadOnlyAndExpiryHasStableError() {
        val engine = engine()
        engine.add("fries", 1)
        val quote = engine.quote(1000)
        val before = engine.state
        assertEquals(quote, engine.checkout(quote.id, 2000))
        assertEquals(before, engine.state)
        val error = assertThrows(OrderingException::class.java) { engine.checkout(quote.id, 302000) }
        assertEquals("SUMMARY_EXPIRED", error.code)
        assertEquals(before, engine.state)
    }

    @Test fun expandedCatalogHasUniqueItemsAndValidDefaultOptions() {
        assertEquals(37, DemoMenu.items.size)
        assertEquals(37, DemoMenu.items.map { it.id }.toSet().size)
        assertEquals(37, DemoMenu.items.map { it.image }.toSet().size)
        DemoMenu.items.forEach { product ->
            assertTrue(product.category in DemoMenu.categories)
            assertEquals(product.priceCents, product.unitPrice(product.defaults))
            assertTrue(product.priceCents > 0)
        }
    }

    @Test fun fixedRecipeAndIcedDrinksRejectUnsupportedOptions() {
        val engine = engine()
        fails { engine.add("espresso", 1, mapOf("size" to "large")) }
        fails { engine.add("cold-brew", 1, engine.item("cold-brew").defaults + ("temperature" to "hot")) }
        engine.add("brown-sugar-tea", 2, engine.item("brown-sugar-tea").defaults + ("size" to "large"))
        assertEquals(5600, engine.totalCents())
        engine.setAllergens(setOf(Allergen.MILK))
        assertTrue(engine.blockers().isNotEmpty())
    }

    private fun engine() = OrderingEngine(DemoMenu.items)
    private fun fails(action: () -> Unit) {
        assertThrows(IllegalArgumentException::class.java, action)
    }
    @Test fun identicalOptionsMergeAndDifferentOptionsStaySeparate() {
        val engine = engine()
        val latte = engine.item("latte")
        engine.add(latte.id, 1)
        val originalId = engine.state.cart.single().id
        engine.add(latte.id, 2)
        engine.add(latte.id, 1, latte.defaults + ("size" to "large"))
        assertEquals(2, engine.state.cart.size)
        assertEquals(originalId, engine.state.cart.first().id)
        assertEquals(9200, engine.totalCents())
    }
    @Test fun stockIsSharedAcrossVariants() {
        val engine = engine()
        engine.add("latte", 19)
        fails { engine.add("latte", 2, engine.item("latte").defaults + ("size" to "large")) }
        assertEquals(19, engine.state.cart.single().quantity)
        fails { engine.add("mash", 1) }
        fails { engine.add("fries", 0) }
    }
    @Test fun fullOptionSetIsRequired() {
        val engine = engine()
        fails { engine.add("latte", 1, mapOf("size" to "large")) }
        fails { engine.add("latte", 1, engine.item("latte").defaults + ("size" to "bad")) }
        assertTrue(engine.state.cart.isEmpty())
    }
    @Test fun allergyChangeKeepsCartAndBlocksCheckout() {
        val engine = engine()
        engine.add("latte", 1)
        engine.setAllergens(setOf(Allergen.MILK))
        assertEquals(1, engine.state.cart.size)
        assertTrue(engine.blockers().any { it.contains("含乳制品") })
        fails { engine.quote() }
        fails { engine.add("latte", 1) }
        engine.setAllergens(emptySet())
        assertEquals(2200, engine.quote().totalCents)
    }
    @Test fun possibleAndUnknownAllergensBlock() {
        val engine = engine()
        engine.setAllergens(setOf(Allergen.PEANUT))
        fails { engine.add("beef-burger", 1) }
        fails { engine.add("garden-salad", 1) }
        engine.add("americano", 1)
        assertTrue(engine.blockers().isEmpty())
    }
    @Test fun editCanMergeWithoutLosingQuantities() {
        val engine = engine()
        val latte = engine.item("latte")
        engine.add(latte.id, 2)
        engine.add(latte.id, 3, latte.defaults + ("size" to "large"))
        engine.update(engine.state.cart.last().id, 4, latte.defaults)
        assertEquals(6, engine.state.cart.single().quantity)
        engine.remove(engine.state.cart.single().id)
        assertTrue(engine.state.cart.isEmpty())
    }
    @Test fun staleQuoteCannotSubmit() {
        val engine = engine()
        engine.add("fries", 1)
        val quote = engine.quote()
        engine.setDining("打包", 1)
        fails { engine.submit(quote.id, "现金") }
        assertTrue(engine.state.orders.isEmpty())
    }
    @Test fun submitIsIdempotentAndConsumesStock() {
        val engine = engine()
        engine.add("steak", 2)
        val quote = engine.quote(1000)
        val order = engine.submit(quote.id, "现金", 2000)
        assertEquals(order, engine.submit(quote.id, "现金", 2100))
        assertEquals(1, engine.state.orders.size)
        assertTrue(engine.state.cart.isEmpty())
        assertEquals(6, engine.remainingStock("steak"))
        val restored = OrderingEngine(DemoMenu.items, engine.state)
        assertEquals(6, restored.remainingStock("steak"))
    }
    @Test fun expiredQuoteAndEmptyCartCannotSubmit() {
        val engine = engine()
        fails { engine.quote() }
        engine.add("fries", 1)
        val quote = engine.quote(1000)
        fails { engine.submit(quote.id, "现金", 302000) }
        assertEquals(1, engine.state.cart.size)
    }

    @Test fun failedPersistenceRollsBackAndKeepsCheckoutRetryable() {
        val engine = engine()
        engine.add("fries", 1)
        val before = engine.state
        val quote = engine.quote(1000)
        assertThrows(IllegalStateException::class.java) {
            engine.transaction({ throw IllegalStateException("disk full") }) { submit(quote.id, "现金", 2000) }
        }
        assertEquals(before, engine.state)
        val order = engine.transaction({}) { submit(quote.id, "现金", 2100) }
        assertEquals(1600, order.totalCents)
        assertEquals(1, engine.state.orders.size)
    }
}
