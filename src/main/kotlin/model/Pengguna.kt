package model

sealed class Pengguna(
    val id: String,
    val nama: String,
    val email: String
) {
    abstract fun peran(): String
    abstract fun infoSingkat(): String
}

class Penyewa(
    id: String,
    nama: String,
    email: String,
    val noTelepon: String
) : Pengguna(id, nama, email) {
    override fun peran(): String = "PENYEWA"
    override fun infoSingkat(): String = "[PENYEWA] $id - $nama ($email | Telp: $noTelepon)"
}

class Penyedia(
    id: String,
    nama: String,
    email: String,
    val namaToko: String,
    terverifikasiAwal: Boolean = false,
    saldoAwal: Int = 0
) : Pengguna(id, nama, email) {
    var terverifikasi: Boolean = terverifikasiAwal
        private set

    var saldoDompet: Int = saldoAwal
        private set

    fun verifikasiAkun() {
        terverifikasi = true
    }

    fun tambahSaldo(nominal: Int) {
        require(nominal >= 0) { "Nominal penambahan saldo tidak boleh negatif." }
        saldoDompet += nominal
    }

    override fun peran(): String = "PENYEDIA"
    override fun infoSingkat(): String {
        val status = if (terverifikasi) "Terverifikasi" else "Belum Verifikasi"
        return "[PENYEDIA] $id - $namaToko oleh $nama ($status | Saldo: Rp$saldoDompet)"
    }
}

class AdminPlatform(
    id: String,
    nama: String,
    email: String,
    val levelAkses: String = "SUPER_ADMIN"
) : Pengguna(id, nama, email) {
    override fun peran(): String = "ADMIN"
    override fun infoSingkat(): String = "[ADMIN] $id - $nama (Akses: $levelAkses)"
}

