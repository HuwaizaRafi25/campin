package data

import model.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate

class MySqlCampInRepository(
    private val dbUrl: String = System.getenv("DB_URL")
        ?: "jdbc:mysql://localhost:3306/campin?allowPublicKeyRetrieval=true&useSSL=false",
    private val dbUser: String = System.getenv("DB_USER") ?: "root",
    private val dbPass: String = System.getenv("DB_PASS") ?: ""
) : CampInRepository {

    var modeDatabaseAktif: String = "MySQL 8.0"
        private set

    private val conn: Connection by lazy {
        try {
            DriverManager.getConnection(dbUrl, dbUser, dbPass)
        } catch (e: Exception) {
            modeDatabaseAktif = "SQLite Local (Fallback MySQL Offline)"
            File("data").mkdirs()
            DriverManager.getConnection("jdbc:sqlite:data/campin_v2.db")
        }
    }

    override fun initSchema() {
        conn.createStatement().use { st ->
            // Auto-upgrade jika mendeteksi tabel versi lama tanpa kolom password_hash
            try {
                st.executeQuery("SELECT password_hash FROM pengguna LIMIT 1").close()
            } catch (_: Exception) {
                runCatching { st.execute("DROP TABLE IF EXISTS transaksi_sewa") }
                runCatching { st.execute("DROP TABLE IF EXISTS alat_camping") }
                runCatching { st.execute("DROP TABLE IF EXISTS pengguna") }
            }

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS pengguna (
                    id VARCHAR(32) PRIMARY KEY,
                    peran VARCHAR(20) NOT NULL,
                    nama VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL,
                    atribut_ekstra VARCHAR(100) NOT NULL,
                    terverifikasi INT DEFAULT 0,
                    saldo INT DEFAULT 0,
                    password_hash VARCHAR(128) DEFAULT ''
                )
                """.trimIndent()
            )
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS alat_camping (
                    id VARCHAR(32) PRIMARY KEY,
                    kategori VARCHAR(20) NOT NULL,
                    nama VARCHAR(100) NOT NULL,
                    harga_per_hari INT NOT NULL,
                    id_penyedia VARCHAR(32) NOT NULL,
                    tersedia INT NOT NULL,
                    spek_int INT NOT NULL,
                    spek_teks VARCHAR(100) NOT NULL,
                    stok INT DEFAULT 1
                )
                """.trimIndent()
            )
            // Migrasi penambahan kolom stok jika tabel sudah ada sebelumnya
            try {
                st.execute("ALTER TABLE alat_camping ADD COLUMN stok INT DEFAULT 1")
            } catch (_: Exception) {}

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS transaksi_sewa (
                    id VARCHAR(32) PRIMARY KEY,
                    alat_id VARCHAR(32) NOT NULL,
                    penyewa_id VARCHAR(32) NOT NULL,
                    durasi_hari INT NOT NULL,
                    total_sewa INT NOT NULL,
                    total_deposit INT NOT NULL,
                    kode_pembayaran VARCHAR(160) NOT NULL,
                    tanggal_pinjam VARCHAR(20) NOT NULL,
                    tanggal_kembali VARCHAR(20),
                    denda INT DEFAULT 0,
                    deposit_dikembalikan INT DEFAULT 0,
                    status_sewa VARCHAR(32) DEFAULT 'AKTIF_DISEWA',
                    jumlah_unit INT DEFAULT 1
                )
                """.trimIndent()
            )
            // Migrasi penambahan kolom jumlah_unit jika tabel sudah ada sebelumnya
            try {
                st.execute("ALTER TABLE transaksi_sewa ADD COLUMN jumlah_unit INT DEFAULT 1")
            } catch (_: Exception) {}
        }
    }

    override fun simpanPengguna(pengguna: Pengguna) {
        val sql = "INSERT INTO pengguna (id, peran, nama, email, atribut_ekstra, terverifikasi, saldo, password_hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, pengguna.id)
            ps.setString(2, pengguna.peran())
            ps.setString(3, pengguna.nama)
            ps.setString(4, pengguna.email)
            when (pengguna) {
                is Penyewa -> {
                    ps.setString(5, pengguna.noTelepon)
                    ps.setInt(6, 1)
                    ps.setInt(7, pengguna.saldoRefundDeposit)
                }
                is Penyedia -> {
                    ps.setString(5, pengguna.namaToko)
                    ps.setInt(6, if (pengguna.terverifikasi) 1 else 0)
                    ps.setInt(7, pengguna.saldoDompet)
                }
                is AdminPlatform -> {
                    ps.setString(5, pengguna.levelAkses)
                    ps.setInt(6, 1)
                    ps.setInt(7, 0)
                }
            }
            ps.setString(8, pengguna.passwordHash)
            ps.executeUpdate()
        }
    }

    override fun updatePenyedia(id: String, terverifikasi: Boolean, saldo: Int) {
        conn.prepareStatement("UPDATE pengguna SET terverifikasi = ?, saldo = ? WHERE id = ?").use { ps ->
            ps.setInt(1, if (terverifikasi) 1 else 0)
            ps.setInt(2, saldo)
            ps.setString(3, id)
            ps.executeUpdate()
        }
    }

    override fun updateSaldoRefundPenyewa(id: String, saldoRefund: Int) {
        conn.prepareStatement("UPDATE pengguna SET saldo = ? WHERE id = ?").use { ps ->
            ps.setInt(1, saldoRefund)
            ps.setString(2, id)
            ps.executeUpdate()
        }
    }

    override fun ambilSemuaPengguna(): List<Pengguna> {
        val list = mutableListOf<Pengguna>()
        conn.createStatement().use { st ->
            val rs = st.executeQuery("SELECT * FROM pengguna")
            while (rs.next()) {
                val id = rs.getString("id")
                val peran = rs.getString("peran")
                val nama = rs.getString("nama")
                val email = rs.getString("email")
                val ekstra = rs.getString("atribut_ekstra")
                val verif = rs.getInt("terverifikasi") == 1
                val saldo = rs.getInt("saldo")
                val passHash = rs.getString("password_hash").orEmpty()

                val user = when (peran) {
                    "PENYEWA" -> Penyewa(id, nama, email, ekstra, passHash, saldo)
                    "PENYEDIA" -> Penyedia(id, nama, email, ekstra, verif, saldo, passHash)
                    else -> AdminPlatform(id, nama, email, ekstra, passHash)
                }
                list.add(user)
            }
        }
        return list
    }

    override fun simpanAlat(alat: AlatCamping) {
        val sql = "INSERT INTO alat_camping (id, kategori, nama, harga_per_hari, id_penyedia, tersedia, spek_int, spek_teks, stok) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, alat.id)
            ps.setString(2, alat.kategori())
            ps.setString(3, alat.nama)
            ps.setInt(4, alat.hargaSewaPerHari)
            ps.setString(5, alat.idPenyedia)
            ps.setInt(6, if (alat.tersedia) 1 else 0)
            when (alat) {
                is Tenda -> {
                    ps.setInt(7, alat.kapasitasOrang)
                    ps.setString(8, alat.tipeLayer)
                }
                is TasCarrier -> {
                    ps.setInt(7, alat.kapasitasLiter)
                    ps.setString(8, alat.adaRaincover.toString())
                }
                is PerlengkapanTidur -> {
                    ps.setInt(7, alat.suhuMinimalDerajat)
                    ps.setString(8, alat.bahanIsian)
                }
            }
            ps.setInt(9, alat.stok)
            ps.executeUpdate()
        }
    }

    override fun updateStatusAlat(id: String, tersedia: Boolean) {
        conn.prepareStatement("UPDATE alat_camping SET tersedia = ? WHERE id = ?").use { ps ->
            ps.setInt(1, if (tersedia) 1 else 0)
            ps.setString(2, id)
            ps.executeUpdate()
        }
    }

    override fun updateStokAlat(id: String, stok: Int) {
        conn.prepareStatement("UPDATE alat_camping SET stok = ?, tersedia = ? WHERE id = ?").use { ps ->
            ps.setInt(1, stok)
            ps.setInt(2, if (stok > 0) 1 else 0)
            ps.setString(3, id)
            ps.executeUpdate()
        }
    }

    override fun ambilSemuaAlat(): List<AlatCamping> {
        val list = mutableListOf<AlatCamping>()
        conn.createStatement().use { st ->
            val rs = st.executeQuery("SELECT * FROM alat_camping")
            while (rs.next()) {
                val id = rs.getString("id")
                val kat = rs.getString("kategori")
                val nama = rs.getString("nama")
                val harga = rs.getInt("harga_per_hari")
                val penyedia = rs.getString("id_penyedia")
                val spekInt = rs.getInt("spek_int")
                val spekTeks = rs.getString("spek_teks")
                val stok = runCatching { rs.getInt("stok") }.getOrDefault(if (rs.getInt("tersedia") == 1) 1 else 0)

                val alat = when (kat) {
                    "TENDA" -> Tenda(id, nama, harga, penyedia, spekInt, spekTeks, stok)
                    "CARRIER" -> TasCarrier(id, nama, harga, penyedia, spekInt, spekTeks.toBoolean(), stok)
                    else -> PerlengkapanTidur(id, nama, harga, penyedia, spekInt, spekTeks, stok)
                }
                list.add(alat)
            }
        }
        return list
    }

    override fun simpanTransaksi(trx: TransaksiSewa) {
        val sql = "INSERT INTO transaksi_sewa (id, alat_id, penyewa_id, durasi_hari, total_sewa, total_deposit, kode_pembayaran, tanggal_pinjam, tanggal_kembali, denda, deposit_dikembalikan, status_sewa, jumlah_unit) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, trx.id)
            ps.setString(2, trx.alatId)
            ps.setString(3, trx.penyewaId)
            ps.setInt(4, trx.durasiHari)
            ps.setInt(5, trx.totalBiayaSewa)
            ps.setInt(6, trx.totalDeposit)
            ps.setString(7, trx.kodePembayaran)
            ps.setString(8, trx.tanggalPinjam.toString())
            ps.setString(9, trx.tanggalKembali?.toString())
            ps.setInt(10, trx.dendaTerlambat)
            ps.setInt(11, trx.depositDikembalikan)
            ps.setString(12, trx.status.name)
            ps.setInt(13, trx.jumlahUnit)
            ps.executeUpdate()
        }
    }

    override fun updatePengembalianTransaksi(
        id: String,
        tanggalKembali: String,
        denda: Int,
        depositDikembalikan: Int,
        status: String
    ) {
        val sql = "UPDATE transaksi_sewa SET tanggal_kembali = ?, denda = ?, deposit_dikembalikan = ?, status_sewa = ? WHERE id = ?"
        conn.prepareStatement(sql).use { ps ->
            ps.setString(1, tanggalKembali)
            ps.setInt(2, denda)
            ps.setInt(3, depositDikembalikan)
            ps.setString(4, status)
            ps.setString(5, id)
            ps.executeUpdate()
        }
    }

    override fun ambilSemuaTransaksi(): List<TransaksiSewa> {
        val list = mutableListOf<TransaksiSewa>()
        conn.createStatement().use { st ->
            val rs = st.executeQuery("SELECT * FROM transaksi_sewa")
            while (rs.next()) {
                val tglKembaliStr = rs.getString("tanggal_kembali")
                val statusStr = rs.getString("status_sewa").orEmpty()
                val statusEnum = runCatching { StatusSewa.valueOf(statusStr) }.getOrDefault(
                    if (tglKembaliStr.isNullOrBlank()) StatusSewa.AKTIF_DISEWA else StatusSewa.SELESAI_DIKEMBALIKAN
                )
                val jumlahUnit = runCatching { rs.getInt("jumlah_unit") }.getOrDefault(1).let { if (it <= 0) 1 else it }
                list.add(
                    TransaksiSewa(
                        id = rs.getString("id"),
                        alatId = rs.getString("alat_id"),
                        penyewaId = rs.getString("penyewa_id"),
                        durasiHari = rs.getInt("durasi_hari"),
                        totalBiayaSewa = rs.getInt("total_sewa"),
                        totalDeposit = rs.getInt("total_deposit"),
                        kodePembayaran = rs.getString("kode_pembayaran"),
                        jumlahUnit = jumlahUnit,
                        tanggalPinjam = LocalDate.parse(rs.getString("tanggal_pinjam")),
                        tanggalKembali = if (tglKembaliStr.isNullOrBlank()) null else LocalDate.parse(tglKembaliStr),
                        dendaTerlambat = rs.getInt("denda"),
                        depositDikembalikan = rs.getInt("deposit_dikembalikan"),
                        status = statusEnum
                    )
                )
            }
        }
        return list
    }
}
