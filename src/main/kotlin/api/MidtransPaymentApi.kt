package api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.util.Base64

class MidtransPaymentApi(
    private val serverKey: String = System.getenv("MIDTRANS_SERVER_KEY") ?: ""
) : PaymentGateway {

    override fun buatTagihanQris(orderId: String, nominal: Int): String = runBlocking {
        if (serverKey.isBlank()) {
            return@runBlocking "https://simulator.sandbox.midtrans.com/qris/index?order_id=$orderId&amount=$nominal"
        }
        try {
            HttpClient(CIO).use { client ->
                val authHeader = Base64.getEncoder().encodeToString("$serverKey:".toByteArray())
                val response: HttpResponse = client.post("https://app.sandbox.midtrans.com/snap/v1/transactions") {
                    header(HttpHeaders.Authorization, "Basic $authHeader")
                    contentType(ContentType.Application.Json)
                    setBody("""{"transaction_details":{"order_id":"$orderId","gross_amount":$nominal}}""")
                }
                if (response.status.isSuccess()) {
                    "MIDTRANS-SNAP-$orderId-$nominal"
                } else {
                    "https://simulator.sandbox.midtrans.com/qris/index?order_id=$orderId&amount=$nominal"
                }
            }
        } catch (e: Exception) {
            "https://simulator.sandbox.midtrans.com/qris/index?order_id=$orderId&amount=$nominal"
        }
    }
}