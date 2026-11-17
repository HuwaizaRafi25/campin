package model

class Tenda(
    id: String,
    nama: String,
    hargaSewaPerHari: Int,
    idPenyedia: String,
    val kapasitasOrang: Int,
    val tipeLayer: String,
    tersediaAwal: Boolean = true
) : AlatCamping(id, nama, hargaSewaPerHari, idPenyedia, tersediaAwal) {

    override fun kategori(): String = "TENDA"

    override fun hitungDeposit(): Int = kapasitasOrang * 25000

    override fun deskripsi(): String =
        "[TENDA] $nama | Kapasitas: ${kapasitasOrang}P ($tipeLayer) | Rp$hargaSewaPerHari/hari | Deposit: Rp${hitungDeposit()}"
}

