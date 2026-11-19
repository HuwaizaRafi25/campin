package model

class Tenda(
    id: String,
    nama: String,
    hargaSewaPerHari: Int,
    idPenyedia: String,
    val kapasitasOrang: Int,
    val tipeLayer: String,
    stokAwal: Int = 1
) : AlatCamping(id, nama, hargaSewaPerHari, idPenyedia, stokAwal) {

    // Konstruktor kompatibilitas untuk boolean tersediaAwal
    constructor(
        id: String,
        nama: String,
        hargaSewaPerHari: Int,
        idPenyedia: String,
        kapasitasOrang: Int,
        tipeLayer: String,
        tersedia: Boolean
    ) : this(id, nama, hargaSewaPerHari, idPenyedia, kapasitasOrang, tipeLayer, if (tersedia) 1 else 0)

    override fun kategori(): String = "TENDA"

    override fun hitungDeposit(): Int = kapasitasOrang * 25000

    override fun deskripsi(): String =
        "[TENDA] $nama | Kapasitas: ${kapasitasOrang}P ($tipeLayer) | Rp$hargaSewaPerHari/hari | Stok: $stok | Deposit: Rp${hitungDeposit()}"
}
