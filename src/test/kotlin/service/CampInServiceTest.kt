package service

import exception.*
import model.*
import kotlin.test.*

class CampInServiceTest {
    private lateinit var service: CampInService

    @BeforeTest
    fun setUp() {
        service = CampInService()
        service.registrasiPengguna(AdminPlatform("ADM-01", "Rafi Admin", "rafi@campin.id"))
        service.registrasiPengguna(Penyedia("MTR-01", "Budi Outdoor", "budi@outdoor.id", "Eigerindo Camp", terverifikasiAwal = true))
        service.registrasiPengguna(Penyedia("MTR-02", "Toko Baru", "baru@outdoor.id", "Unverified Camp", terverifikasiAwal = false))
        service.registrasiPengguna(Penyewa("USR-01", "Andi Pendaki", "andi@gmail.com", "08123456789"))

        service.tambahAlat(Tenda("ALT-01", "Tenda Eiger Guardian 4P", 65000, "MTR-01", 4, "Double Layer"))
        service.tambahAlat(TasCarrier("ALT-02", "Carrier Osprey Atmos 65L", 55000, "MTR-01", 65, true))
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
        val alatBaru = PerlengkapanTidur("ALT-03", "SB Consina Polar", 25000, "MTR-02", 5, "Polar")
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
    fun `4 - menyewa alat mengubah status tersedia menjadi false dan mencatat transaksi`() {
        val trx = service.sewaAlat("ALT-01", "USR-01", 2)
        assertEquals(130000, trx.totalBiayaSewa)
        assertEquals(100000, trx.totalDeposit)
        assertFalse(service.cariAlat("Guardian").first().tersedia)
        assertEquals(1, service.transaksiAktif().size)
    }

    @Test
    fun `5 - menyewa alat yang sedang disewa harus melempar AlatTidakTersediaException`() {
        service.sewaAlat("ALT-01", "USR-01", 1)
        assertFailsWith<AlatTidakTersediaException> {
            service.sewaAlat("ALT-01", "USR-01", 2)
        }
    }

    @Test
    fun `6 - mengembalikan alat mencairkan 90 persen saldo ke penyedia dan 10 persen komisi admin`() {
        val trx = service.sewaAlat("ALT-01", "USR-01", 2)
        service.kembalikanAlat(trx.id, hariTerlambat = 1)

        val penyedia = service.cariPengguna("MTR-01") as Penyedia
        assertEquals(13000, service.totalKomisiPlatform)
        assertEquals(142000, penyedia.saldoDompet)
        assertTrue(service.cariAlat("Guardian").first().tersedia)
    }

    @Test
    fun `7 - registrasi pengguna dengan email tanpa karakter at harus ditolak`() {
        assertFailsWith<InputTidakValidException> {
            service.registrasiPengguna(Penyewa("USR-99", "Citra", "citra-tanpa-at.com", "08111"))
        }
    }

    @Test
    fun `8 - duplikasi ID alat atau pengguna harus melempar DuplikasiIdException`() {
        assertFailsWith<DuplikasiIdException> {
            service.registrasiPengguna(Penyewa("USR-01", "Duplikat", "dup@mail.com", "0812"))
        }
    }
}