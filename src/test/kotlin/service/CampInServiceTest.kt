package service

import api.MidtransPaymentApi
import exception.*
import model.*
import kotlin.test.*

class CampInServiceTest {
    private lateinit var service: CampInService

    @BeforeTest
    fun setUp() {
        service = CampInService()
        service.registrasiPengguna(AdminPlatform("ADM-01", "Rafi Admin", "rafi@campin.id"), "admin123")
        service.registrasiPengguna(Penyedia("MTR-01", "Budi Outdoor", "budi@outdoor.id", "Eigerindo Camp", terverifikasiAwal = true), "mitra123")
        service.registrasiPengguna(Penyedia("MTR-02", "Toko Baru", "baru@outdoor.id", "Unverified Camp", terverifikasiAwal = false), "mitra123")
        service.registrasiPengguna(Penyewa("USR-01", "Andi Pendaki", "andi@gmail.com", "08123456789"), "user123")

        service.tambahAlat(Tenda("ALT-01", "Tenda Eiger Guardian 4P", 65000, "MTR-01", 4, "Double Layer", stokAwal = 3))
        service.tambahAlat(TasCarrier("ALT-02", "Carrier Osprey Atmos 65L", 55000, "MTR-01", 65, true, stokAwal = 1))
    }

    @Test
    fun `1 - menambah alat oleh penyedia yang belum diverifikasi harus ditolak`() {
        assertFailsWith<AksesDitolakException> {
            service.tambahAlat(Tenda("ALT-99", "Tenda Ilegal", 40000, "MTR-02", 2, "Single Layer"))
        }
    }

    @Test
    fun `2 - verifikasi penyedia oleh admin membuat penyedia bisa menambah alat`() {
        service.verifikasiPenyedia("ADM-01", "MTR-02")
        val alatBaru = PerlengkapanTidur("ALT-03", "SB Consina Polar", 25000, "MTR-02", 5, "Polar", stokAwal = 3)
        service.tambahAlat(alatBaru)
        assertEquals(3, service.semuaAlat().size)
    }

    @Test
    fun `3 - perhitungan deposit polimorfik pada setiap subclass harus akurat`() {
        val tenda = service.cariAlat("Guardian").first()
        val carrier = service.cariAlat("Osprey").first()
        assertEquals(100000, tenda.hitungDeposit())
        assertEquals(75000, carrier.hitungDeposit())
    }

    @Test
    fun `4 - menyewa alat mengubah status tersedia menjadi false dan menghitung jatuh tempo`() {
        val tenda = service.cariAlat("Guardian").first()
        assertEquals(3, tenda.stok)

        val trx = service.sewaAlat("ALT-01", "USR-01", 2, jumlahUnit = 1)
        assertEquals(130000, trx.totalBiayaSewa)
        assertEquals(100000, trx.totalDeposit)
        assertEquals(trx.tanggalPinjam.plusDays(2), trx.tanggalJatuhTempo)
        assertEquals(2, tenda.stok)
        assertTrue(tenda.tersedia)
        assertEquals(1, service.transaksiAktif().size)
    }

    @Test
    fun `5 - menyewa alat yang sedang disewa harus melempar AlatTidakTersediaException`() {
        val carrier = service.cariAlat("Osprey").first()
        assertEquals(1, carrier.stok)

        service.sewaAlat("ALT-02", "USR-01", 1)
        assertEquals(0, carrier.stok)
        assertFalse(carrier.tersedia)

        assertFailsWith<AlatTidakTersediaException> {
            service.sewaAlat("ALT-02", "USR-01", 2)
        }
    }

    @Test
    fun `6 - mengembalikan alat mencairkan 90 persen ke mitra, 10 persen ke admin, dan refund sisa deposit ke penyewa`() {
        val trx = service.sewaAlat("ALT-01", "USR-01", 2, jumlahUnit = 1) // Sewa: 130.000, Deposit: 100.000
        service.kembalikanAlat(trx.id, hariTerlambat = 1) // Denda: 25.000

        val penyedia = service.cariPengguna("MTR-01") as Penyedia
        val penyewa = service.cariPengguna("USR-01") as Penyewa
        val tenda = service.cariAlat("Guardian").first()

        assertEquals(13000, service.totalKomisiPlatform)      // 10% dari 130.000
        assertEquals(142000, penyedia.saldoDompet)            // 117.000 (90%) + 25.000 (denda)
        assertEquals(75000, penyewa.saldoRefundDeposit)       // Deposit 100.000 - denda 25.000
        assertEquals(3, tenda.stok)                           // Stok kembali pulih ke 3
        assertTrue(tenda.tersedia)
    }

    @Test
    fun `7 - registrasi pengguna dengan email tanpa karakter at harus ditolak`() {
        assertFailsWith<InputTidakValidException> {
            service.registrasiPengguna(Penyewa("USR-99", "Citra", "citra-tanpa-at.com", "08111"), "pass123")
        }
    }

    @Test
    fun `8 - duplikasi ID alat atau pengguna harus melempar DuplikasiIdException`() {
        assertFailsWith<DuplikasiIdException> {
            service.registrasiPengguna(Penyewa("USR-01", "Duplikat", "dup@mail.com", "0812"), "pass123")
        }
    }

    @Test
    fun `9 - login berhasil dengan ID atau email yang benar dan gagal jika password salah`() {
        val user = service.login("USR-01", "user123")
        assertEquals("Andi Pendaki", user.nama)

        assertFailsWith<AutentikasiGagalException> {
            service.login("USR-01", "password-ngawur")
        }
    }

    @Test
    fun `10 - penyewa yang mencoba memverifikasi mitra harus ditolak dengan AksesDitolakException`() {
        assertFailsWith<AksesDitolakException> {
            service.verifikasiPenyedia("USR-01", "MTR-02")
        }
    }

    @Test
    fun `11 - MidtransPaymentApi berhasil membuat transaksi snap dan mengembalikan redirect url`() {
        val midtrans = MidtransPaymentApi()
        val redirectUrl = midtrans.buatTagihanQris("TRX-TEST-UNIT", 50000)
        println("[TEST RESULT] Midtrans Snap Redirect URL: $redirectUrl")
        assertTrue(
            redirectUrl.startsWith("https://app.sandbox.midtrans.com/snap/"),
            "Redirect URL harus mengarah ke Midtrans Snap Sandbox, aktual: $redirectUrl"
        )
    }

    @Test
    fun `12 - manajemen kuantitas stok alat berkurang saat disewa dan bertambah saat dikembalikan`() {
        val tenda = service.cariAlat("Guardian").first()
        assertEquals(3, tenda.stok)

        val trx1 = service.sewaAlat("ALT-01", "USR-01", 1, jumlahUnit = 1)
        assertEquals(2, tenda.stok)
        assertTrue(tenda.tersedia)

        val trx2 = service.sewaAlat("ALT-01", "USR-01", 2, jumlahUnit = 2)
        assertEquals(0, tenda.stok)
        assertFalse(tenda.tersedia)

        // Sewa ketiga harus ditolak karena stok habis
        assertFailsWith<AlatTidakTersediaException> {
            service.sewaAlat("ALT-01", "USR-01", 1)
        }

        // Pengembalian trx1 menambah stok kembali jadi 1
        service.kembalikanAlat(trx1.id)
        assertEquals(1, tenda.stok)
        assertTrue(tenda.tersedia)

        // Pengembalian trx2 menambah stok kembali jadi 3
        service.kembalikanAlat(trx2.id)
        assertEquals(3, tenda.stok)
        assertTrue(tenda.tersedia)
    }

    @Test
    fun `13 - menyewa multi unit mengalikan total biaya sewa dan deposit secara proporsional`() {
        // Tenda: Rp65.000/hari, deposit: Rp100.000/unit
        // Sewa 2 unit selama 3 hari:
        // Biaya sewa = 65.000 * 3 * 2 = 390.000
        // Total deposit = 100.000 * 2 = 200.000
        // Grand total = 590.000
        val trx = service.sewaAlat("ALT-01", "USR-01", durasiHari = 3, jumlahUnit = 2)
        assertEquals(2, trx.jumlahUnit)
        assertEquals(390000, trx.totalBiayaSewa)
        assertEquals(200000, trx.totalDeposit)
        assertEquals(590000, trx.grandTotalBayar)

        // Kembalikan alat
        service.kembalikanAlat(trx.id)
        assertEquals(3, service.cariAlat("Guardian").first().stok)
    }
}
