package model
import java.time.LocalDate
import java.util.UUID
enum class StatusSewa {
    MENUNGGU_PEMBAYARAN,
    AKTIF_DISEWA,
    SELESAI_DIKEMBALIKAN
}
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
    var dendaTerlambat: Int = 0,
    var depositDikembalikan: Int = 0,
    var status: StatusSewa = StatusSewa.AKTIF_DISEWA
) { val tanggalJatuhTempo: LocalDate get() = tanggalPinjam.plusDays(durasiHari.toLong())
    val sedangDisewa: Boolean
        get() = status != StatusSewa.SELESAI_DIKEMBALIKAN && tanggalKembali ==
                null
    val grandTotalBayar: Int
        get() = totalBiayaSewa + totalDeposit
}