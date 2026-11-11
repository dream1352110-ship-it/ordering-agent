package com.sm.orderingagent.capability

import android.app.Application
import com.sm.orderingagent.domain.OrderingException
import com.sm.orderingagent.operations.OrderingOperations
import com.sm.osagent.capability.provider.CapabilityCliHandler
import com.sm.osagent.capability.provider.CapabilityCliOutput
import com.sm.osagent.capability.provider.CapabilityCliStatus
import org.json.JSONObject

class OrderingCliHandler(
    application: Application,
    private val operations: OrderingOperations = OrderingOperations(application),
) : CapabilityCliHandler {
    override fun prepareConfirmation(
        arguments: List<String>,
        output: CapabilityCliOutput,
    ): CapabilityCliStatus = handle(arguments, output, prepare = true)

    override fun execute(
        arguments: List<String>,
        output: CapabilityCliOutput,
    ): CapabilityCliStatus = handle(arguments, output, prepare = false)

    private fun handle(
        arguments: List<String>,
        output: CapabilityCliOutput,
        prepare: Boolean,
    ): CapabilityCliStatus {
        try {
            val command = try {
                parse(arguments)
            } catch (failure: Exception) {
                return failure(output, failure)
            }
            if (command == null) {
                output.stdout(help())
                return CapabilityCliStatus.SUCCESS
            }
            if (prepare) {
                val confirmation = try {
                    if (command.id == "submit-demo-order") {
                        operations.prepareConfirmation(command.rawJson)
                    } else null
                } catch (failure: Exception) {
                    return failure(output, failure)
                }
                confirmation?.let(output::stdout)
                return CapabilityCliStatus.SUCCESS
            }
            val result = operations.execute(command.id, command.rawJson)
            return if (result.success) {
                output.stdout(requireNotNull(result.resultJson))
                CapabilityCliStatus.SUCCESS
            } else {
                error(output, result.errorCode ?: "INTERNAL_ERROR", result.errorMessage ?: "Operation failed")
            }
        } finally {
            output.flush()
        }
    }

    private fun parse(arguments: List<String>): Command? {
        require(arguments.size == 1 || arguments.size == 3) {
            "Expected operation or operation --json '<JSON object>'; use --help"
        }
        require(arguments.all { it.length <= MAX_JSON_BYTES && it.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES }) {
            "Argument exceeds 64 KiB"
        }
        val id = arguments[0]
        if (arguments.size == 1 && id in setOf("-h", "--help")) return null
        require(id in OrderingOperations.operationIds) { "Unknown operation: $id" }
        val raw = if (arguments.size == 1) {
            require(id == "get-cart" || id == "list-menu") { "This operation requires --json '<JSON object>'" }
            "{}"
        } else {
            require(arguments[1] == "--json") { "Expected exactly one --json argument" }
            arguments[2]
        }
        OrderingOperations.parseArguments(raw)
        return Command(id, raw)
    }

    private fun failure(output: CapabilityCliOutput, failure: Exception): CapabilityCliStatus = error(
        output,
        if (failure is OrderingException) failure.code else "INVALID_ARGUMENT",
        failure.message ?: "Invalid argument",
    )

    private fun error(output: CapabilityCliOutput, code: String, message: String): CapabilityCliStatus {
        output.stderr(JSONObject().put("success", false).put("errorCode", code)
            .put("errorMessage", message).toString())
        return CapabilityCliStatus.FAILURE
    }

    private fun help(): String = buildString {
        append("Usage: sm-oscap-cli ordering <operation> --json '<JSON object>'\n")
        append("sm-oscap-cli ordering -h | --help\n")
        append("get-cart and list-menu also accept operation only.\n")
        append("Ordinary writes require requestId; omit orderId to use the execution-time current cart.\n")
        append("Explicit orderId is checked; start-new-order and submit-demo-order still require it.\n")
        append("Examples:\n")
        OrderingOperations.operationIds.forEach { id ->
            append("sm-oscap-cli ordering ").append(id).append(" --json '")
                .append(examples.getValue(id)).append("'\n")
        }
    }

    private data class Command(val id: String, val rawJson: String)

    private companion object {
        const val MAX_JSON_BYTES = 65_536
        val examples = mapOf(
            "list-menu" to "{}",
            "get-menu-item" to "{\"itemId\":\"latte\"}",
            "search-menu" to "{\"names\":[\"拿铁\"],\"itemIds\":[\"fries\"]}",
            "apply-cart-changes" to "{\"requestId\":\"batch-1\",\"changes\":[{\"operation\":\"add-cart-item\",\"itemId\":\"fries\",\"quantity\":1,\"options\":{}},{\"operation\":\"set-dining-settings\",\"partySize\":3}]}",
            "get-cart" to "{}",
            "start-new-order" to "{\"requestId\":\"new-1\",\"orderId\":\"<orderId>\"}",
            "set-dining-settings" to "{\"requestId\":\"dining-1\",\"serviceMode\":\"takeout\",\"partySize\":2}",
            "add-cart-item" to "{\"requestId\":\"add-1\",\"itemId\":\"meatballs\",\"quantity\":1,\"options\":{}}",
            "update-cart-item" to "{\"requestId\":\"update-1\",\"cartItemId\":\"<cartItemId>\",\"quantity\":2}",
            "remove-cart-item" to "{\"requestId\":\"remove-1\",\"cartItemId\":\"<cartItemId>\"}",
            "set-allergy-requirements" to "{\"requestId\":\"allergy-1\",\"allergenIds\":[\"milk\"]}",
            "calculate-price" to "{\"items\":[{\"itemId\":\"fries\",\"quantity\":1,\"options\":{}}]}",
            "open-checkout" to "{\"expectedCartVersion\":1}",
            "submit-demo-order" to "{\"requestId\":\"submit-1\",\"orderId\":\"<orderId>\",\"expectedCartVersion\":1,\"summaryId\":\"<summaryId>\"}",
        )
    }
}
