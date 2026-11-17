package ui

import api.CuacaGunungApi
import exception.CampInException
import model.*
import service.CampInService

class CliApp(private val service: CampInService, private val dbStatus: String) {

    fun jalankan() {
        println("================================================================")
        println("   ⛺ SELAMAT DATANG DI CAMPIN - P2P CAMPING RENTAL PLATFORM    ")
        println("   🗄️ Database Engine : $dbStatus")
        println("   📱 Mobile App UI   : http://localhost:8080 (Buka di HP/Web)  ")
        println("================================================================")

        while (true) {
            tampilkanMenu()
            when (bacaInput("Pilih menu [0-10]: ")) {
                "1" -> lihatSemuaAlat()
                "2" -> cariAlatMenu()
                "3" -> tambahAlatMenu()
                "4" -> registrasiPenggunaMenu()
                "5" -> verifikasiPenyediaMenu()
                "6" -> sewaAlatMenu()
                "7" -> kembalikanAlatMenu()
                "8" -> lihatTransaksiDanSaldoMenu()
                "9" -> cekCuacaGunungMenu()
                "10" -> lihatSemuaPengguna()
                "0" -> {
                    service.simpanKeCsv()
                    println("✓ Backup CSV tersimpan di data/campin_backup.csv")
                    println("Terima kasih telah menggunakan CampIn!")
                    return
                }
                else -> println("! Pilihan menu tidak dikenali, silakan ketik angka 0-10.")
            }
            println()
        }
    }

    private fun tampilkanMenu() {
        println(
            """
            --------------------- MENU UTAMA CAMPIN ---------------------
            [KATALOG & PENCARIAN]
              1. Lihat Semua Katalog Alat Camping
              2. Cari Alat Camping (Keyword / Kategori)
            [ROLE PENYEDIA / MITRA]
              3. Tambah Alat Sewaan Baru (Tenda / Carrier / Sleeping Gear)
            [ROLE PENYEWA / CAMPER]
              6. Sewa Alat Camping (Midtrans QRIS Checkout)
              7. Kembalikan Alat & Hitung Bagi Hasil Escrow
              9. Cek Cuaca Gunung & Rekomendasi Alat (Live Open-Meteo API)
            [ROLE ADMIN & MANAJEMEN AKUN]
              4. Registrasi Pengguna Baru (Penyewa / Penyedia / Admin)
              5. Verifikasi Akun Mitra Penyedia (Khusus Admin)
              8. Laporan Transaksi Aktif, Saldo Mitra & Komisi Platform
             10. Lihat Daftar Semua Pengguna (3 Role)
              0. Keluar & Simpan Backup CSV
            -------------------------------------------------------------
            """.trimIndent()
        )
    }

    private fun bacaInput(prompt: String): String {
        print(prompt)
        return readlnOrNull()?.trim().orEmpty()
    }

    private fun bacaAngka(prompt: String): Int {
        while (true) {
            val angka = bacaInput(prompt).toIntOrNull()
            if (angka != null && angka >= 0) return angka
            println("! Harap masukkan angka bulat positif yang valid.")
        }
    }

    private inline fun jalankanAman(block: () -> Unit) {
        try {
            block()
        } catch (e: CampInException) {
            println("! [ATURAN BISNIS] ${e.message}")
        } catch (e: Exception) {
            println("! [KESALAHAN SISTEM] ${e.message}")
        }
    }

    private fun lihatSemuaAlat() {
        val list = service.semuaAlat()
        if (list.isEmpty()) {
            println("Belum ada alat camping yang terdaftar.")
            return
        }
        println("\n--- DAFTAR KATALOG ALAT CAMPING (${list.size} Unit) ---")
        list.forEach {
            val status = if (it.tersedia) "✓ TERSEDIA" else "✗ DISEWA"
            println("[$status] ID: ${it.id} (Mitra: ${it.idPenyedia}) -> ${it.deskripsi()}")
        }
    }

    private fun cariAlatMenu() {
        val kw = bacaInput("Masukkan kata kunci pencarian: ")
        val hasil = service.cariAlat(kw)
        if (hasil.isEmpty()) {
            println("Tidak ditemukan alat untuk kata kunci '$kw'.")
        } else {
            hasil.forEachIndexed { idx, alat ->
                println("${idx + 1}. [${alat.id}] ${alat.deskripsi()}")
            }
        }
    }

    private fun tambahAlatMenu() {
        println("\nPilih Kategori Alat: 1) Tenda  2) Tas Carrier  3) Perlengkapan Tidur")
        val jenis = bacaInput("Pilihan [1-3]: ")
        if (jenis !in listOf("1", "2", "3")) {
            println("! Kategori tidak valid. Pilih 1, 2, atau 3.")
            return
        }
        val id = bacaInput("ID Alat (misal ALT-05)     : ")
        val idPenyedia = bacaInput("ID Penyedia (misal MTR-01) : ")
        val nama = bacaInput("Nama Alat                  : ")
        val harga = bacaAngka("Harga Sewa per Hari (Rp)   : ")

        val alat: AlatCamping = when (jenis) {
            "1" -> Tenda(id, nama, harga, idPenyedia, bacaAngka("Kapasitas (Orang)          : "), bacaInput("Tipe Layer (Single/Double) : "))
            "2" -> TasCarrier(id, nama, harga, idPenyedia, bacaAngka("Kapasitas Volume (Liter)   : "), bacaInput("Ada Raincover? (y/n)       : ").equals("y", true))
            else -> PerlengkapanTidur(id, nama, harga, idPenyedia, bacaAngka("Batas Suhu Dingin (°C)     : "), bacaInput("Bahan Isian (Polar/Dacron) : "))
        }

        jalankanAman {
            service.tambahAlat(alat)
            println("✓ Alat berhasil ditambahkan ke katalog: ${alat.deskripsi()}")
        }
    }

    private fun registrasiPenggunaMenu() {
        println("\nPilih Role: 1) Penyewa (Camper)  2) Penyedia (Mitra Toko)  3) Admin Platform")
        val role = bacaInput("Pilihan [1-3]: ")
        if (role !in listOf("1", "2", "3")) {
            println("! Role tidak valid.")
            return
        }
        val id = bacaInput("ID Pengguna   : ")
        val nama = bacaInput("Nama Lengkap  : ")
        val email = bacaInput("Alamat Email  : ")

        val user: Pengguna = when (role) {
            "1" -> Penyewa(id, nama, email, bacaInput("No. Telepon   : "))
            "2" -> Penyedia(id, nama, email, bacaInput("Nama Toko     : "))
            else -> AdminPlatform(id, nama, email)
        }

        jalankanAman {
            service.registrasiPengguna(user)
            println("✓ Pengguna berhasil didaftarkan: ${user.infoSingkat()}")
        }
    }

    private fun verifikasiPenyediaMenu() {
        val adminId = bacaInput("Masukkan ID Admin Anda (contoh ADM-01)      : ")
        val penyediaId = bacaInput("Masukkan ID Penyedia yang akan diverifikasi : ")
        jalankanAman {
            service.verifikasiPenyedia(adminId, penyediaId)
            println("✓ Mitra Penyedia '$penyediaId' berhasil diverifikasi!")
        }
    }

    private fun sewaAlatMenu() {
        val alatId = bacaInput("ID Alat yang ingin disewa (contoh ALT-01) : ")
        val penyewaId = bacaInput("ID Penyewa Anda (contoh USR-01)           : ")
        val durasi = bacaAngka("Durasi Sewa (hari)                        : ")

        jalankanAman {
            val trx = service.sewaAlat(alatId, penyewaId, durasi)
            println(
                """
                ✓ PEMESANAN & TAGIHAN QRIS BERHASIL DIBUAT!
                  • ID Transaksi    : ${trx.id}
                  • Biaya Sewa      : Rp${trx.totalBiayaSewa} (${trx.durasiHari} hari)
                  • Uang Deposit    : Rp${trx.totalDeposit}
                  • Total Tagihan   : Rp${trx.grandTotalBayar}
                  • Link/Kode QRIS  : ${trx.kodePembayaran}
                """.trimIndent()
            )
        }
    }

    private fun kembalikanAlatMenu() {
        val trxId = bacaInput("ID Transaksi Sewa (contoh TRX-101) : ")
        val telat = bacaAngka("Jumlah Hari Keterlambatan (0 jika tepat waktu): ")
        jalankanAman {
            val trx = service.kembalikanAlat(trxId, telat)
            println("✓ Alat '${trx.alatId}' telah dikembalikan! Denda keterlambatan: Rp${trx.dendaTerlambat}.")
        }
    }

    private fun lihatTransaksiDanSaldoMenu() {
        println("\n--- TRANSAKSI SEWA AKTIF ---")
        val aktif = service.transaksiAktif()
        if (aktif.isEmpty()) {
            println("Tidak ada transaksi sewa yang sedang berjalan.")
        } else {
            aktif.forEach {
                println("• [${it.id}] Alat=${it.alatId} | Penyewa=${it.penyewaId} | Durasi=${it.durasiHari} hari | Total=Rp${it.grandTotalBayar}")
            }
        }
        println("\n--- LAPORAN ESCROW & KOMISI PLATFORM ---")
        println("Total Pendapatan Komisi Platform (10%): Rp${service.totalKomisiPlatform}")
        service.semuaPengguna().filterIsInstance<Penyedia>().forEach {
            println("• ${it.infoSingkat()}")
        }
    }

    private fun cekCuacaGunungMenu() {
        println("\nPilih Destinasi Pendakian:")
        CuacaGunungApi.daftarGunung.forEach { (kode, g) ->
            println("  $kode. ${g.nama} (${g.mdpl} MDPL)")
        }
        val kode = bacaInput("Pilih gunung [1-4]: ")
        println("Mengambil data telemetri cuaca dari Open-Meteo API...")
        println(CuacaGunungApi.cekCuacaBlocking(kode))
    }

    private fun lihatSemuaPengguna() {
        println("\n--- DAFTAR PENGGUNA TERDAFTAR ---")
        service.semuaPengguna().forEach { println("• ${it.infoSingkat()}") }
    }
}

