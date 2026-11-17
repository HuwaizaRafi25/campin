package model

import java.time.LocalDate
import java.util.UUID

data class TransaksiSewa(
    val id: String = "TRX-" + UUID.randomUUID().toString().take(6).uppercase(),
    val alatId: String,
    val penyewaId: String,
    val durasiHari: Int,
    val totalBiayaSewa: Int,
    val totalDeposit: Int,
    val kodePembayaran: String,
    val tanggalPinjam: LocalDate = LocalDate.now(),
    var tanggalKembali: LocalDate? = null,
    var dendaTerlambat: Int = 0
) {
    val sedangDisewa: Boolean
        get() = tanggalKembali == null

    val grandTotalBayar: Int
        get() = totalBiayaSewa + totalDeposit
}

