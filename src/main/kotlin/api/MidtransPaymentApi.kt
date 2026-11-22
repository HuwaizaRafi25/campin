package api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.Base64

class MidtransPaymentApi(
    private val serverKey: String = System.getenv("MIDTRANS_SERVER_KEY")
        ?.takeIf { it.isNotBlank() }
        ?: "SB-Mid-server-MwAf4.p_NFf7evozGG1Anqtq"
) : PaymentGateway {

    override fun buatTagihanQris(orderId: String, nominal: Int): String = runBlocking {
        if (serverKey.isBlank()) {
            return@runBlocking "https://simulator.sandbox.midtrans.com/qris/index?order_id=$orderId&amount=$nominal"
        }
        try {
            HttpClient(CIO).use { client ->
                val authHeader = Base64.getEncoder().encodeToString("$serverKey:".toByteArray())
                val timestamp = System.currentTimeMillis() / 1000
                val cleanOrderId = "${orderId.replace(Regex("[^a-zA-Z0-9-_]"), "")}-$timestamp"
                val response: HttpResponse = client.post("https://app.sandbox.midtrans.com/snap/v1/transactions") {
                    header(HttpHeaders.Authorization, "Basic $authHeader")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "transaction_details": {
                                "order_id": "$cleanOrderId",
                                "gross_amount": $nominal
                            },
                            "enabled_payments": ["qris", "gopay", "shopeepay", "other_qris"]
                        }
                        """.trimIndent()
                    )
                }
                val body = response.bodyAsText()
                val redirectUrl = Regex("\"redirect_url\"\\s*:\\s*\"([^\"]+)\"").find(body)
                    ?.groupValues?.get(1)?.replace("\\/", "/")
                println("[Midtrans] status=${response.status} body=$body")
                if (response.status.isSuccess() && redirectUrl != null) {
                    redirectUrl
                } else {
                    System.err.println("[Midtrans ERROR] status=${response.status} body=$body")
                    "https://simulator.sandbox.midtrans.com/qris/index?order_id=$orderId&amount=$nominal"
                }
            }
        } catch (e: Exception) {
            System.err.println("[Midtrans EXCEPTION] ${e.message}")
            e.printStackTrace()
            "https://simulator.sandbox.midtrans.com/qris/index?order_id=$orderId&amount=$nominal"
        }
    }
}
