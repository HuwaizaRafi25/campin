package model

class PerlengkapanTidur(
    id: String,
    nama: String,
    hargaSewaPerHari: Int,
    idPenyedia: String,
    val suhuMinimalDerajat: Int,
    val bahanIsian: String,
    tersediaAwal: Boolean = true
) : AlatCamping(id, nama, hargaSewaPerHari, idPenyedia, tersediaAwal) {

    override fun kategori(): String = "SLEEPING_GEAR"

    override fun hitungDeposit(): Int =
        if (suhuMinimalDerajat <= 5) 60000 else 35000

    override fun deskripsi(): String =
        "[SLEEPING GEAR] $nama | Limit ${suhuMinimalDerajat}°C ($bahanIsian) | Rp$hargaSewaPerHari/hari | Deposit: Rp${hitungDeposit()}"
}

