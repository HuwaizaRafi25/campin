package api

interface PaymentGateway {
    fun buatTagihanQris(orderId: String, nominal: Int): String
}

