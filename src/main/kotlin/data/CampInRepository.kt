package data

import model.AlatCamping
import model.Pengguna
import model.TransaksiSewa

interface CampInRepository {
    fun initSchema()
    fun simpanPengguna(pengguna: Pengguna)
    fun updatePenyedia(id: String, terverifikasi: Boolean, saldo: Int)
    fun updateSaldoRefundPenyewa(id: String, saldoRefund: Int)
    fun ambilSemuaPengguna(): List<Pengguna>
    fun simpanAlat(alat: AlatCamping)
    fun updateStatusAlat(id: String, tersedia: Boolean)
    fun ambilSemuaAlat(): List<AlatCamping>
    fun simpanTransaksi(trx: TransaksiSewa)
    fun updatePengembalianTransaksi(
        id: String,
        tanggalKembali: String,
        denda: Int,
        depositDikembalikan: Int = 0,
        status: String = "SELESAI_DIKEMBALIKAN"
    )
    fun ambilSemuaTransaksi(): List<TransaksiSewa>
}