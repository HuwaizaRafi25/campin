package model

abstract class AlatCamping(
    val id: String,
    var nama: String,
    var hargaSewaPerHari: Int,
    val idPenyedia: String,
    stokAwal: Int = 1
) {
    var stok: Int = if (stokAwal < 0) 0 else stokAwal
        protected set

    val tersedia: Boolean
        get() = stok > 0

    abstract fun kategori(): String
    abstract fun deskripsi(): String
    abstract fun hitungDeposit(): Int

    fun kurangiStok(jumlah: Int = 1) {
        require(jumlah > 0) { "Jumlah sewa harus lebih dari 0." }
        require(stok >= jumlah) { "Stok alat '$nama' tidak mencukupi (tersisa $stok unit)." }
        stok -= jumlah
    }

    fun tambahStok(jumlah: Int = 1) {
        require(jumlah > 0) { "Jumlah pengembalian harus lebih dari 0." }
        stok += jumlah
    }

    // Backward-compatibility untuk kode/test lama
    fun tandaiDisewa() = kurangiStok(1)
    fun tandaiDikembalikan() = tambahStok(1)

    override fun toString(): String = deskripsi()
}
