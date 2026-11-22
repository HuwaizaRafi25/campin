package model

class TasCarrier(
    id: String,
    nama: String,
    hargaSewaPerHari: Int,
    idPenyedia: String,
    val kapasitasLiter: Int,
    val adaRaincover: Boolean,
    stokAwal: Int = 1
) : AlatCamping(id, nama, hargaSewaPerHari, idPenyedia, stokAwal) {

    // Konstruktor kompatibilitas untuk boolean tersediaAwal
    constructor(
        id: String,
        nama: String,
        hargaSewaPerHari: Int,
        idPenyedia: String,
        kapasitasLiter: Int,
        adaRaincover: Boolean,
        tersedia: Boolean
    ) : this(id, nama, hargaSewaPerHari, idPenyedia, kapasitasLiter, adaRaincover, if (tersedia) 1 else 0)

    override fun kategori(): String = "CARRIER"

    override fun hitungDeposit(): Int =
        if (kapasitasLiter >= 60) 75000 else 50000

    override fun deskripsi(): String {
        val infoCover = if (adaRaincover) "Termasuk Raincover" else "Tanpa Raincover"
        return "[CARRIER] $nama | ${kapasitasLiter}L ($infoCover) | Rp$hargaSewaPerHari/hari | Stok: $stok | Deposit: Rp${hitungDeposit()}"
    }
}
