package model

abstract class AlatCamping(
    val id: String,
    var nama: String,
    var hargaSewaPerHari: Int,
    val idPenyedia: String,
    tersediaAwal: Boolean = true
) {
    var tersedia: Boolean = tersediaAwal
        protected set

    abstract fun kategori(): String
    abstract fun deskripsi(): String
    abstract fun hitungDeposit(): Int

    fun tandaiDisewa() {
        require(tersedia) { "Alat '$nama' sedang tidak tersedia." }
        tersedia = false
    }

    fun tandaiDikembalikan() {
        tersedia = true
    }

    override fun toString(): String = deskripsi()
}

