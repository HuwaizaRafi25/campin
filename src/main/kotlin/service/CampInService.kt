package service

import api.PaymentGateway
import data.CampInRepository
import exception.*
import model.*
import java.io.File
import java.time.LocalDate

class CampInService(
    private val repository: CampInRepository? = null,
    private val paymentGateway: PaymentGateway? = null
) : PencarianAlat {

    private val alatMap = mutableMapOf<String, AlatCamping>()
    private val penggunaMap = mutableMapOf<String, Pengguna>()
    private val transaksiList = mutableListOf<TransaksiSewa>()

    var totalKomisiPlatform: Int = 0
        private set

    init {
        repository?.let { repo ->
            repo.initSchema()
            repo.ambilSemuaPengguna().forEach { penggunaMap[it.id] = it }
            repo.ambilSemuaAlat().forEach { alatMap[it.id] = it }
            transaksiList.addAll(repo.ambilSemuaTransaksi())
        }
    }

    fun registrasiPengguna(pengguna: Pengguna) {
        if (pengguna.id.isBlank() || pengguna.nama.isBlank()) {
            throw InputTidakValidException("ID dan nama pengguna wajib diisi.")
        }
        if (!pengguna.email.contains("@") || !pengguna.email.contains(".")) {
            throw InputTidakValidException("Format email tidak valid: '${pengguna.email}'.")
        }
        if (penggunaMap.containsKey(pengguna.id)) {
            throw DuplikasiIdException(pengguna.id)
        }
        penggunaMap[pengguna.id] = pengguna
        repository?.simpanPengguna(pengguna)
    }

    fun verifikasiPenyedia(adminId: String, penyediaId: String) {
        val admin = penggunaMap[adminId]
            ?: throw DataTidakDitemukanException("Admin", adminId)
        if (admin !is AdminPlatform) {
            throw AksesDitolakException("Hanya AdminPlatform yang berhak memverifikasi mitra Penyedia.")
        }
        val penyedia = penggunaMap[penyediaId]
            ?: throw DataTidakDitemukanException("Penyedia", penyediaId)
        if (penyedia !is Penyedia) {
            throw InputTidakValidException("Pengguna '${penyedia.nama}' bukan berstatus Penyedia.")
        }
        penyedia.verifikasiAkun()
        repository?.updatePenyedia(penyedia.id, penyedia.terverifikasi, penyedia.saldoDompet)
    }

    fun semuaPengguna(): List<Pengguna> = penggunaMap.values.toList()

    fun cariPengguna(id: String): Pengguna =
        penggunaMap[id] ?: throw DataTidakDitemukanException("Pengguna", id)

    fun tambahAlat(alat: AlatCamping) {
        if (alat.id.isBlank() || alat.nama.isBlank()) {
            throw InputTidakValidException("ID dan nama alat camping tidak boleh kosong.")
        }
        if (alat.hargaSewaPerHari <= 0) {
            throw InputTidakValidException("Harga sewa per hari harus lebih dari Rp0.")
        }
        if (alatMap.containsKey(alat.id)) {
            throw DuplikasiIdException(alat.id)
        }
        val pemilik = penggunaMap[alat.idPenyedia]
            ?: throw DataTidakDitemukanException("Penyedia", alat.idPenyedia)
        if (pemilik !is Penyedia) {
            throw AksesDitolakException("Hanya akun berstatus Penyedia yang boleh menyewakan alat.")
        }
        if (!pemilik.terverifikasi) {
            throw AksesDitolakException("Toko '${pemilik.namaToko}' belum diverifikasi oleh Admin Platform.")
        }

        alatMap[alat.id] = alat
        repository?.simpanAlat(alat)
    }

    override fun cariAlat(keyword: String): List<AlatCamping> =
        alatMap.values.filter {
            it.nama.contains(keyword, ignoreCase = true) ||
                    it.deskripsi().contains(keyword, ignoreCase = true) ||
                    it.kategori().contains(keyword, ignoreCase = true)
        }

    fun semuaAlat(): List<AlatCamping> = alatMap.values.toList()

    fun kategoriUnik(): Set<String> = alatMap.values.map { it.kategori() }.toSet()

    fun sewaAlat(alatId: String, penyewaId: String, durasiHari: Int): TransaksiSewa {
        if (durasiHari <= 0 || durasiHari > 30) {
            throw InputTidakValidException("Durasi sewa harus antara 1 hingga 30 hari.")
        }
        val alat = alatMap[alatId]
            ?: throw DataTidakDitemukanException("Alat Camping", alatId)
        val penyewa = penggunaMap[penyewaId]
            ?: throw DataTidakDitemukanException("Penyewa", penyewaId)

        if (penyewa !is Penyewa) {
            throw AksesDitolakException("Hanya akun berstatus Penyewa yang dapat melakukan penyewaan.")
        }
        if (alat.idPenyedia == penyewaId) {
            throw AksesDitolakException("Anda tidak dapat menyewa alat milik sendiri.")
        }
        if (!alat.tersedia) {
            throw AlatTidakTersediaException(alat.nama)
        }

        val totalSewa = alat.hargaSewaPerHari * durasiHari
        val deposit = alat.hitungDeposit()
        val idTransaksi = "TRX-" + (transaksiList.size + 101)

        val kodeQris = paymentGateway?.buatTagihanQris(idTransaksi, totalSewa + deposit)
            ?: "QRIS-SIMULASI-$idTransaksi"

        alat.tandaiDisewa()
        repository?.updateStatusAlat(alat.id, false)

        val trx = TransaksiSewa(
            id = idTransaksi,
            alatId = alat.id,
            penyewaId = penyewa.id,
            durasiHari = durasiHari,
            totalBiayaSewa = totalSewa,
            totalDeposit = deposit,
            kodePembayaran = kodeQris
        )
        transaksiList.add(trx)
        repository?.simpanTransaksi(trx)
        return trx
    }

    fun kembalikanAlat(transaksiId: String, hariTerlambat: Int = 0): TransaksiSewa {
        if (hariTerlambat < 0) {
            throw InputTidakValidException("Jumlah hari keterlambatan tidak boleh negatif.")
        }
        val trx = transaksiList.firstOrNull { it.id.equals(transaksiId, ignoreCase = true) && it.sedangDisewa }
            ?: throw DataTidakDitemukanException("Transaksi Sewa Aktif", transaksiId)

        val alat = alatMap[trx.alatId]
            ?: throw DataTidakDitemukanException("Alat Camping", trx.alatId)

        val denda = hariTerlambat * 25000
        trx.dendaTerlambat = denda
        trx.tanggalKembali = LocalDate.now()

        alat.tandaiDikembalikan()
        repository?.updateStatusAlat(alat.id, true)
        repository?.updatePengembalianTransaksi(trx.id, trx.tanggalKembali.toString(), denda)

        val komisi = (trx.totalBiayaSewa * 10) / 100
        val pendapatanPenyedia = (trx.totalBiayaSewa - komisi) + denda
        totalKomisiPlatform += komisi

        val penyedia = penggunaMap[alat.idPenyedia]
        if (penyedia is Penyedia) {
            penyedia.tambahSaldo(pendapatanPenyedia)
            repository?.updatePenyedia(penyedia.id, penyedia.terverifikasi, penyedia.saldoDompet)
        }

        return trx
    }

    fun transaksiAktif(): List<TransaksiSewa> = transaksiList.filter { it.sedangDisewa }

    fun semuaTransaksi(): List<TransaksiSewa> = transaksiList.toList()

    fun simpanKeCsv(path: String = "data/campin_backup.csv") {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(buildString {
            appendLine("# PENGGUNA")
            penggunaMap.values.forEach { u ->
                appendLine("USER|${u.peran()}|${u.id}|${u.nama}|${u.email}")
            }
            appendLine("# ALAT_CAMPING")
            alatMap.values.forEach { a ->
                appendLine("ALAT|${a.kategori()}|${a.id}|${a.nama}|${a.hargaSewaPerHari}|${a.idPenyedia}|${a.tersedia}")
            }
            appendLine("# TRANSAKSI_SEWA")
            transaksiList.forEach { t ->
                appendLine("TRX|${t.id}|${t.alatId}|${t.penyewaId}|${t.durasiHari}|${t.totalBiayaSewa}|${t.tanggalPinjam}|${t.tanggalKembali ?: "-"}")
            }
        })
    }
}

