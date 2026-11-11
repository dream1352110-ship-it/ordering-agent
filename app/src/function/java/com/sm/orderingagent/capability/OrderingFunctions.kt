package com.sm.orderingagent.capability

import android.app.Application
import android.content.Intent
import com.sm.orderingagent.operations.OrderingOperations
import com.sm.osagent.capability.provider.CapabilityFunction
import com.sm.osagent.capability.provider.CapabilityFunctionHandler
import com.sm.osagent.capability.provider.CapabilityResult
import com.sm.osagent.capability.provider.ConfirmationMessage

class OrderingFunctions(
    application: Application,
    launchActivity: (Intent) -> Unit = { application.startActivity(it) },
) {
    private val operations = OrderingOperations(application, launchActivity)

    fun handlers(): Map<String, CapabilityFunctionHandler> = linkedMapOf(
        "list-menu" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("list-menu", raw) }),
        "get-menu-item" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("get-menu-item", raw) }),
        "search-menu" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("search-menu", raw) }),
        "apply-cart-changes" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("apply-cart-changes", raw) }),
        "get-cart" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("get-cart", raw) }),
        "start-new-order" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("start-new-order", raw) }),
        "set-dining-settings" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("set-dining-settings", raw) }),
        "add-cart-item" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("add-cart-item", raw) }),
        "update-cart-item" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("update-cart-item", raw) }),
        "remove-cart-item" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("remove-cart-item", raw) }),
        "set-allergy-requirements" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("set-allergy-requirements", raw) }),
        "calculate-price" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("calculate-price", raw) }),
        "open-checkout" to CapabilityFunctionHandler(CapabilityFunction { raw -> execute("open-checkout", raw) }),
        "submit-demo-order" to CapabilityFunctionHandler(
            CapabilityFunction { raw -> execute("submit-demo-order", raw) },
            ConfirmationMessage { raw -> operations.prepareConfirmation(raw) },
        ),
    )

    private fun execute(id: String, raw: String): CapabilityResult {
        val result = operations.execute(id, raw)
        return CapabilityResult(
            result.success,
            result.resultJson,
            result.errorCode,
            result.errorMessage,
        )
    }

    companion object {
        val functionIds: List<String> = OrderingOperations.operationIds
    }
}
