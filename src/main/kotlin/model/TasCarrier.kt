package model

class TasCarrier(
    id: String,
    nama: String,
    hargaSewaPerHari: Int,
    idPenyedia: String,
    val kapasitasLiter: Int,
    val adaRaincover: Boolean,
    tersediaAwal: Boolean = true
) : AlatCamping(id, nama, hargaSewaPerHari, idPenyedia, tersediaAwal) {

    override fun kategori(): String = "CARRIER"

    override fun hitungDeposit(): Int =
        if (kapasitasLiter >= 60) 75000 else 50000

    override fun deskripsi(): String {
        val infoCover = if (adaRaincover) "Termasuk Raincover" else "Tanpa Raincover"
        return "[CARRIER] $nama | ${kapasitasLiter}L ($infoCover) | Rp$hargaSewaPerHari/hari | Deposit: Rp${hitungDeposit()}"
    }
}

