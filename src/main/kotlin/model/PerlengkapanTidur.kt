package model

class PerlengkapanTidur(
    id: String,
    nama: String,
    hargaSewaPerHari: Int,
    idPenyedia: String,
    val suhuMinimalDerajat: Int,
    val bahanIsian: String,
    stokAwal: Int = 1
) : AlatCamping(id, nama, hargaSewaPerHari, idPenyedia, stokAwal) {

    // Konstruktor kompatibilitas untuk boolean tersediaAwal
    constructor(
        id: String,
        nama: String,
        hargaSewaPerHari: Int,
        idPenyedia: String,
        suhuMinimalDerajat: Int,
        bahanIsian: String,
        tersedia: Boolean
    ) : this(id, nama, hargaSewaPerHari, idPenyedia, suhuMinimalDerajat, bahanIsian, if (tersedia) 1 else 0)

    override fun kategori(): String = "SLEEPING_GEAR"

    override fun hitungDeposit(): Int =
        if (suhuMinimalDerajat <= 5) 60000 else 35000

    override fun deskripsi(): String =
        "[SLEEPING GEAR] $nama | Limit ${suhuMinimalDerajat}°C ($bahanIsian) | Rp$hargaSewaPerHari/hari | Stok: $stok | Deposit: Rp${hitungDeposit()}"
}
