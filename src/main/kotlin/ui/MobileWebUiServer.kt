package ui

import api.CuacaGunungApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import model.*
import service.CampInService
import ui.WebLayout.escapeHtml
import ui.WebLayout.icon
import ui.WebLayout.rupiah
import ui.WebLayout.tanggalId

object MobileWebUiServer {
    private var server: NettyApplicationEngine? = null
    var activePort: Int = 8088
        private set
    private const val COOKIE_USER = "CAMPIN_USER_ID"

    private fun ApplicationCall.loggedUser(service: CampInService): Pengguna? {
        val id = request.cookies[COOKIE_USER] ?: return null
        return runCatching { service.cariPengguna(id) }.getOrNull()
    }

    private fun homePathFor(user: Pengguna): String = when (user) {
        is Penyewa -> "/katalog"
        is Penyedia -> "/mitra"
        is AdminPlatform -> "/admin"
    }

    fun start(service: CampInService, dbInfo: String, port: Int = 8088): Int {
        val candidatePorts = listOf(port, 8088, 8888, 9090, 8082, 8080)
        for (p in candidatePorts) {
            try {
                server = embeddedServer(Netty, host = "0.0.0.0", port = p) {
                    routing {
                        get("/") {
                            val user = call.loggedUser(service)
                            if (user == null) call.respondRedirect("/login")
                            else call.respondRedirect(homePathFor(user))
                        }

                        // ==================== AUTH: LOGIN & REGISTER ====================
                        get("/login") {
                            val msg = call.request.queryParameters["msg"]
                            call.respondText(
                                WebLayout.pageShell("Login & Demo Role", "login", null, dbInfo, msg, renderLoginPage(service)),
                                ContentType.Text.Html
                            )
                        }

                        post("/login") {
                            val p = call.receiveParameters()
                            try {
                                val user = service.login(p["idOrEmail"].orEmpty(), p["password"].orEmpty())
                                call.response.cookies.append(COOKIE_USER, user.id, path = "/")
                                call.respondRedirect(homePathFor(user) + "?msg=" + "✓ Selamat datang, ${user.nama}!".encodeURLParameter())
                            } catch (e: Exception) {
                                call.respondRedirect("/login?msg=" + "! ${e.message}".encodeURLParameter())
                            }
                        }

                        post("/register") {
                            val p = call.receiveParameters()
                            try {
                                val role = p["role"].orEmpty()
                                val id = p["id"].orEmpty().trim().uppercase()
                                val nama = p["nama"].orEmpty().trim()
                                val email = p["email"].orEmpty().trim()
                                val ekstra = p["ekstra"].orEmpty().trim()
                                val pass = p["password"].orEmpty()

                                val newUser: Pengguna = when (role) {
                                    "PENYEDIA" -> Penyedia(id, nama, email, ekstra.ifBlank { "$nama Outdoor" })
                                    else -> Penyewa(id, nama, email, ekstra.ifBlank { "0812000000" })
                                }
                                service.registrasiPengguna(newUser, pass)
                                call.response.cookies.append(COOKIE_USER, newUser.id, path = "/")
                                call.respondRedirect(homePathFor(newUser) + "?msg=" + "✓ Akun $id berhasil dibuat!".encodeURLParameter())
                            } catch (e: Exception) {
                                call.respondRedirect("/login?msg=" + "! ${e.message}".encodeURLParameter())
                            }
                        }

                        post("/logout") {
                            call.response.cookies.append(COOKIE_USER, "", maxAge = 0L, path = "/")
                            call.respondRedirect("/login?msg=" + "✓ Anda telah keluar dari sesi.".encodeURLParameter())
                        }

                        // ==================== PENYEWA: KATALOG & CUACA ====================
                        get("/katalog") {
                            val user = call.loggedUser(service) ?: return@get call.respondRedirect("/login")
                            val msg = call.request.queryParameters["msg"]
                            val kw = call.request.queryParameters["q"].orEmpty()
                            val kat = call.request.queryParameters["kat"].orEmpty()
                            val gunung = call.request.queryParameters["gunung"]
                            val cuacaText = if (!gunung.isNullOrBlank()) CuacaGunungApi.ambilCuacaDanRekomendasi(gunung) else null

                            val html = renderKatalogPage(service, user, kw, kat, cuacaText, gunung)
                            call.respondText(
                                WebLayout.pageShell("Katalog Alat", "katalog", user, dbInfo, msg, html),
                                ContentType.Text.Html
                            )
                        }

                        post("/sewa") {
                            val user = call.loggedUser(service) ?: return@post call.respondRedirect("/login")
                            val p = call.receiveParameters()
                            try {
                                val durasi = p["durasi"]?.toIntOrNull() ?: 1
                                val unit = p["jumlahUnit"]?.toIntOrNull() ?: 1
                                val trx = service.sewaAlat(
                                    p["alatId"].orEmpty().trim(),
                                    user.id,
                                    durasi,
                                    unit
                                )
                                val sukses = "✓ Booking ${trx.id} (${trx.jumlahUnit} unit) Berhasil! Total ${rupiah(trx.grandTotalBayar)}. Silakan bayar via QRIS / Midtrans Snap."
                                call.respondRedirect("/sewa-saya?msg=" + sukses.encodeURLParameter())
                            } catch (e: Exception) {
                                call.respondRedirect("/katalog?msg=" + "! ${e.message}".encodeURLParameter())
                            }
                        }

                        // ==================== PENYEWA: SEWAAN SAYA & QRIS ====================
                        get("/sewa-saya") {
                            val user = call.loggedUser(service) ?: return@get call.respondRedirect("/login")
                            val msg = call.request.queryParameters["msg"]
                            val html = renderSewaSayaPage(service, user)
                            call.respondText(
                                WebLayout.pageShell("Sewaan & QRIS", "sewa", user, dbInfo, msg, html),
                                ContentType.Text.Html
                            )
                        }

                        post("/konfirmasi-bayar") {
                            val user = call.loggedUser(service) ?: return@post call.respondRedirect("/login")
                            val p = call.receiveParameters()
                            val trxId = p["trxId"].orEmpty().trim()
                            val trx = service.transaksiAktif().find { it.id.equals(trxId, ignoreCase = true) }
                            if (trx != null) {
                                val info = "✓ Transaksi ${trx.id} (${rupiah(trx.grandTotalBayar)}) BERHASIL DIBAYAR! Pembayaran lunas."
                                call.respondRedirect("/sewa-saya?msg=" + info.encodeURLParameter())
                            } else {
                                call.respondRedirect("/sewa-saya?msg=" + "! Transaksi aktif tidak ditemukan.".encodeURLParameter())
                            }
                        }

                        // ==================== MITRA PENYEDIA ====================
                        get("/mitra") {
                            val user = call.loggedUser(service) ?: return@get call.respondRedirect("/login")
                            if (user !is Penyedia) {
                                return@get call.respondRedirect(homePathFor(user) + "?msg=" + "! Halaman khusus Mitra Penyedia.".encodeURLParameter())
                            }
                            val msg = call.request.queryParameters["msg"]
                            val html = renderMitraPage(service, user)
                            call.respondText(
                                WebLayout.pageShell("Dashboard Mitra", "mitra", user, dbInfo, msg, html),
                                ContentType.Text.Html
                            )
                        }

                        post("/tambah-alat") {
                            val user = call.loggedUser(service) ?: return@post call.respondRedirect("/login")
                            val p = call.receiveParameters()
                            val targetRedirect = if (user is Penyedia) "/mitra" else "/katalog"
                            try {
                                val kat = p["kategori"].orEmpty()
                                val id = p["id"].orEmpty().trim().uppercase()
                                val mitraId = if (user is Penyedia) user.id else p["idPenyedia"].orEmpty().trim()
                                val nama = p["nama"].orEmpty().trim()
                                val harga = p["harga"]?.toIntOrNull() ?: 0
                                val spekInt = p["spekInt"]?.toIntOrNull() ?: 2
                                val spekTeks = p["spekTeks"].orEmpty().trim()
                                val stok = (p["stokAwal"]?.toIntOrNull() ?: 1).coerceAtLeast(1)

                                val alat = when (kat) {
                                    "TENDA" -> Tenda(id, nama, harga, mitraId, spekInt, spekTeks.ifBlank { "Double Layer" }, stok)
                                    "CARRIER" -> TasCarrier(id, nama, harga, mitraId, spekInt, spekTeks.equals("true", true), stok)
                                    else -> PerlengkapanTidur(id, nama, harga, mitraId, spekInt, spekTeks.ifBlank { "Polar" }, stok)
                                }
                                service.tambahAlat(alat)
                                call.respondRedirect("$targetRedirect?msg=" + "✓ Alat '${alat.nama}' (${alat.stok} unit) berhasil ditambahkan!".encodeURLParameter())
                            } catch (e: Exception) {
                                call.respondRedirect("$targetRedirect?msg=" + "! ${e.message}".encodeURLParameter())
                            }
                        }

                        post("/kembali") {
                            val user = call.loggedUser(service) ?: return@post call.respondRedirect("/login")
                            val p = call.receiveParameters()
                            val backUrl = if (user is Penyedia) "/mitra" else if (user is AdminPlatform) "/admin" else "/sewa-saya"
                            try {
                                val trx = service.kembalikanAlat(
                                    p["trxId"].orEmpty().trim(),
                                    p["telat"]?.toIntOrNull() ?: 0
                                )
                                val info = "✓ Alat ${trx.alatId} (${trx.jumlahUnit} unit) kembali! Denda: Rp${trx.dendaTerlambat} | Refund Deposit Penyewa: Rp${trx.depositDikembalikan}"
                                call.respondRedirect("$backUrl?msg=" + info.encodeURLParameter())
                            } catch (e: Exception) {
                                call.respondRedirect("$backUrl?msg=" + "! ${e.message}".encodeURLParameter())
                            }
                        }

                        // ==================== ADMIN PLATFORM ====================
                        get("/admin") {
                            val user = call.loggedUser(service) ?: return@get call.respondRedirect("/login")
                            if (user !is AdminPlatform) {
                                return@get call.respondRedirect(homePathFor(user) + "?msg=" + "! Halaman khusus Admin Platform.".encodeURLParameter())
                            }
                            val msg = call.request.queryParameters["msg"]
                            val html = renderAdminPage(service, user)
                            call.respondText(
                                WebLayout.pageShell("Admin Platform", "admin", user, dbInfo, msg, html),
                                ContentType.Text.Html
                            )
                        }

                        post("/verifikasi") {
                            val user = call.loggedUser(service) ?: return@post call.respondRedirect("/login")
                            val p = call.receiveParameters()
                            try {
                                service.verifikasiPenyedia(user.id, p["penyediaId"].orEmpty().trim())
                                call.respondRedirect("/admin?msg=" + "✓ Mitra ${p["penyediaId"]} resmi diverifikasi!".encodeURLParameter())
                            } catch (e: Exception) {
                                call.respondRedirect("/admin?msg=" + "! ${e.message}".encodeURLParameter())
                            }
                        }
                    }
                }.start(wait = false)
                activePort = p
                println("\n=============================================================")
                println("  📱 Web UI CampIn aktif di: http://localhost:$p")
                println("=============================================================\n")
                return p
            } catch (e: Exception) {
                // coba port berikutnya jika port bentrok
            }
        }
        return activePort
    }

    fun stop() {
        server?.stop(500, 1000)
    }

    // ==================== RENDER HALAMAN PER ROLE ====================
    private fun imageForCategory(kategori: String): String = when (kategori.uppercase()) {
        "TENDA" -> "https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?auto=format&fit=crop&w=600&q=80"
        "CARRIER" -> "https://images.unsplash.com/photo-1622260614153-03223fb72052?auto=format&fit=crop&w=600&q=80"
        else -> "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=600&q=80"
    }

    private val daftarGunungUi = mapOf(
        "1" to Pair("Cikuray", 2821),
        "2" to Pair("Burangrang", 2050),
        "3" to Pair("Ciremai", 3078),
        "4" to Pair("Rinjani", 3726)
    )

    private fun sheetId(alatId: String): String =
        "sewa-" + alatId.filter { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun renderLoginPage(service: CampInService): String {
        val demoRows = service.semuaPengguna().joinToString("") { u ->
            val sub = when (u) {
                is Penyewa -> "Penyewa (Camper)"
                is Penyedia -> if (u.terverifikasi) "Mitra Terverifikasi" else "Mitra Belum Verifikasi"
                is AdminPlatform -> "Admin Platform"
            }
            """
            <form action="/login" method="post">
                <input type="hidden" name="idOrEmail" value="${escapeHtml(u.id)}">
                <input type="hidden" name="password" value="123456">
                <button class="quick">
                    <span class="avatar">${icon("user")}</span>
                    <span class="quick-t"><b>$sub</b><small>${escapeHtml(u.id)} • ${escapeHtml(u.nama)}</small></span>
                    ${icon("chevron-right")}
                </button>
            </form>
            """.trimIndent()
        }

        return """
        <section class="hero">
            <h1>Camping<br><b>Rental</b></h1>
            <p>Temukan perlengkapan outdoor untuk perjalanan yang lebih nyaman dan siap dijalani.</p>
        </section>

        <div class="glass pad stack">
            <div class="ph">${icon("zap")} Masuk Cepat Demo</div>
            $demoRows
            <div class="ph" style="margin-top:6px">${icon("lock")} Login Manual</div>
            <form id="loginManual" action="/login" method="post" class="stack">
                <label class="field">${icon("mail")}<input name="idOrEmail" placeholder="ID Pengguna atau Email" required autocomplete="username"></label>
                <label class="field">${icon("lock")}<input name="password" type="password" placeholder="Password" required autocomplete="current-password">
                    <button type="button" class="eye" aria-label="Tampilkan password" onclick="var i=this.parentNode.querySelector('input');i.type=(i.type==='password')?'text':'password';">${icon("eye-off")}</button>
                </label>
            </form>
        </div>

        <button class="btn-lime" form="loginManual">Masuk ke CampIn ${icon("chevron-right")}</button>
        <p class="signup">Belum punya akun? <a href="#registrasi">Registrasi di sini</a></p>

        <section id="registrasi" class="glass pad stack reg">
            <div class="ph">${icon("user")} Registrasi Pengguna Baru</div>
            <form action="/register" method="post" class="stack">
                <label class="field"><select name="role">
                    <option value="PENYEWA">Daftar sebagai Penyewa (Pendaki)</option>
                    <option value="PENYEDIA">Daftar sebagai Mitra Penyedia (Toko Rental)</option>
                </select></label>
                <label class="field"><input name="id" placeholder="ID Unik Baru (contoh: USR-05 atau MTR-05)" required></label>
                <label class="field"><input name="nama" placeholder="Nama Lengkap" required></label>
                <label class="field"><input name="email" placeholder="Email Aktif (wajib ada @)" required></label>
                <label class="field"><input name="ekstra" placeholder="No. Telepon (Penyewa) / Nama Toko (Mitra)" required></label>
                <label class="field"><input name="password" type="password" placeholder="Password (min. 4 karakter)" required></label>
                <button class="btn-lime">Buat Akun Baru</button>
            </form>
        </section>
        """.trimIndent()
    }

    private fun renderKatalogPage(
        service: CampInService,
        user: Pengguna,
        keyword: String,
        kategoriFilter: String,
        infoCuaca: String?,
        gunungKode: String?
    ): String {
        val hasilAlat = service.cariAlat(keyword).filter {
            kategoriFilter.isBlank() || it.kategori().equals(kategoriFilter, ignoreCase = true)
        }

        val gunung = daftarGunungUi[gunungKode] ?: daftarGunungUi.getValue("1")
        val suhu = if (!infoCuaca.isNullOrBlank()) {
            Regex("""(-?\d+(?:[.,]\d+)?)\s*°\s*C""").find(infoCuaca)?.groupValues?.get(1)?.let { "$it°C" } ?: "--°C"
        } else "24°C"
        val kondisi = if (!infoCuaca.isNullOrBlank()) "Data Open-Meteo" else "Cerah Berawan"
        val cuacaBox = if (!infoCuaca.isNullOrBlank()) "<pre>${escapeHtml(infoCuaca)}</pre>" else ""
        val cuacaHtml = """
        <details class="glass wx" ${if (!infoCuaca.isNullOrBlank()) "open" else ""}>
            <summary>
                <div class="wx-l">${icon("cloud-sun", 44)}<div><div class="wx-t">$suhu</div><div class="wx-s">$kondisi</div></div></div>
                <span class="wx-div"></span>
                <div class="wx-r">${icon("pin", 22)}<div><b>Gunung ${gunung.first}</b><small>${gunung.second} mdpl</small></div></div>
                ${icon("chevron-right")}
            </summary>
            <div class="wx-body">
                <div class="wx-grid">
                    <a href="/katalog?gunung=1">⛰️ Cikuray (2821m)</a>
                    <a href="/katalog?gunung=2">⛰️ Burangrang (2050m)</a>
                    <a href="/katalog?gunung=3">⛰️ Ciremai (3078m)</a>
                    <a href="/katalog?gunung=4">⛰️ Rinjani (3726m)</a>
                </div>
                $cuacaBox
            </div>
        </details>
        """.trimIndent()

        val q = keyword.encodeURLParameter()
        fun chip(label: String, iconName: String, kode: String): String {
            val on = if (kode.isBlank()) kategoriFilter.isBlank() else kategoriFilter.equals(kode, ignoreCase = true)
            return """<a href="/katalog?kat=$kode&q=$q" class="chip ${if (on) "on" else ""}">${icon(iconName, 18)} $label</a>"""
        }
        val chips = chip("Semua", "grid", "") + chip("Tenda", "tent", "TENDA") +
                chip("Carrier", "backpack", "CARRIER") + chip("Sleeping Bag", "bed", "SLEEPING_GEAR")

        val kartu = hasilAlat.joinToString("") { a ->
            val sid = sheetId(a.id)
            val tag = if (a.tersedia) """<span class="tag">${icon("leaf", 12)} Stok: ${a.stok}</span>"""
            else """<span class="tag off">Habis</span>"""
            val aksi = when {
                user is Penyewa && a.tersedia -> """<a href="#$sid" class="btn-dark">${icon("cart", 16)} Sewa</a>"""
                !a.tersedia -> """<span class="btn-dark off">Habis</span>"""
                else -> ""
            }
            """
            <article class="glass pcard">
                <div class="pimg"><img src="${imageForCategory(a.kategori())}" alt="${escapeHtml(a.nama)}" loading="lazy" onerror="this.style.display='none'">$tag</div>
                <div class="pname">${escapeHtml(a.nama)}</div>
                <div class="pprice"><b>${rupiah(a.hargaSewaPerHari)}</b><span> /hari</span></div>
                <div class="pdep">${icon("shield", 14)} Deposit: ${rupiah(a.hitungDeposit())}</div>
                $aksi
            </article>
            """.trimIndent()
        }

        val sheets = if (user is Penyewa) hasilAlat.filter { it.tersedia }.joinToString("") { a ->
            """
            <div id="${sheetId(a.id)}" class="sheet">
                <a href="#" class="sheet-bg" aria-label="Tutup"></a>
                <div class="sheet-box">
                    <div class="grab"></div>
                    <div class="ph">${icon("tent")} ${escapeHtml(a.nama)}</div>
                    <p class="muted" style="font-size:13px">${escapeHtml(a.id)} • ${escapeHtml(a.kategori())} (Mitra: ${escapeHtml(a.idPenyedia)}) • Sisa Stok: <b>${a.stok} unit</b></p>
                    <p style="font-size:14px;line-height:1.5">${escapeHtml(a.deskripsi())}</p>
                    <div class="li">
                        <div class="row-b"><span class="muted">Harga sewa</span><b>${rupiah(a.hargaSewaPerHari)} /hari</b></div>
                        <div class="row-b"><span class="muted">Jaminan deposit</span><b>${rupiah(a.hitungDeposit())} /unit</b></div>
                    </div>
                    <form action="/sewa" method="post" class="stack">
                        <input type="hidden" name="alatId" value="${escapeHtml(a.id)}">
                        <label class="field"><span>Jumlah Unit</span><input type="number" name="jumlahUnit" value="1" min="1" max="${a.stok}" style="text-align:right"></label>
                        <label class="field"><span>Durasi (hari)</span><input type="number" name="durasi" value="2" min="1" max="30" style="text-align:right"></label>
                        <button class="btn-lime">Sewa &amp; QRIS ${icon("chevron-right")}</button>
                    </form>
                    <a href="#" class="btn-dark">Batal</a>
                </div>
            </div>
            """.trimIndent()
        } else ""

        val katalogBody = if (hasilAlat.isEmpty())
            """<div class="glass empty">Tidak ada alat yang cocok dengan pencarian Anda.</div>"""
        else """<div class="rail">$kartu</div>"""

        return """
        <h2 class="h-title">Camping Rental<b>Equipment</b></h2>

        $cuacaHtml

        <form action="/katalog" method="get" class="search">
            <label class="field">${icon("search")}<input name="q" value="${escapeHtml(keyword)}" placeholder="Cari perlengkapan..."></label>
            <input type="hidden" name="kat" value="${escapeHtml(kategoriFilter)}">
            <button class="btn-circ" aria-label="Cari">${icon("sliders")}</button>
        </form>

        <div class="chips">$chips</div>

        <section class="catalog">
            <input type="checkbox" id="all" class="vh">
            <div class="sec-h">
                <h2>${icon("tent", 22)} Katalog Peralatan (${hasilAlat.size})</h2>
                <label for="all"><span class="a">Lihat Semua</span><span class="b">Ringkas</span> ${icon("chevron-right", 16)}</label>
            </div>
            $katalogBody
        </section>
        $sheets
        """.trimIndent()
    }

    private fun renderSewaSayaPage(service: CampInService, user: Pengguna): String {
        val saldoRefund = (user as? Penyewa)?.saldoRefundDeposit ?: 0
        val daftarTrx = if (user is Penyewa) service.transaksiByPenyewa(user.id) else service.semuaTransaksi()
        val namaAlat = service.semuaAlat().associate { it.id to it.nama }

        val listHtml = daftarTrx.joinToString("") { t ->
            val aktif = t.sedangDisewa
            val badge = if (aktif) """<span class="state">Aktif Disewa</span>""" else """<span class="state done">Selesai</span>"""
            val extra = if (aktif) "" else """
                <div class="kv">${icon("clock", 18)}<span>Denda : ${rupiah(t.dendaTerlambat)}</span></div>
                <div class="kv">${icon("wallet", 18)}<span>Refund Deposit : ${rupiah(t.depositDikembalikan)}</span></div>
            """

            val payUrl = if (t.kodePembayaran.startsWith("http")) t.kodePembayaran
            else "https://simulator.sandbox.midtrans.com/qris/index?order_id=${t.id}&amount=${t.grandTotalBayar}"

            val qr = if (aktif) """
                <div class="qr">
                    ${WebLayout.renderQrCodeSvg(payUrl)}
                    <b>Kode Pembayaran QRIS</b>
                    <span>${escapeHtml(t.kodePembayaran)}</span>
                    <a class="btn-lime sm" style="margin-top:10px;width:100%;flex:none;text-align:center;display:block" href="${escapeHtml(payUrl)}" target="_blank" rel="noopener">💳 Buka Pembayaran Midtrans Snap</a>
                    <form action="/konfirmasi-bayar" method="post" style="margin-top:8px">
                        <input type="hidden" name="trxId" value="${escapeHtml(t.id)}">
                        <button class="btn-dark sm" style="width:100%">✓ Konfirmasi Bayar Lunas (Simulasi)</button>
                    </form>
                </div>
                <p class="scan">${icon("info", 14)}<span>Scan kode QR di atas atau klik tombol Midtrans untuk membayar.</span></p>
            """ else ""

            """
            <article class="glass trx">
                $badge
                <h3>${escapeHtml(t.id)} • ${escapeHtml(namaAlat[t.alatId] ?: t.alatId)} (${t.jumlahUnit} unit)</h3>
                <div class="kv">${icon("calendar", 18)}<span>Pinjam : ${tanggalId(t.tanggalPinjam)}</span></div>
                <div class="kv">${icon("clock", 18)}<span>Jatuh Tempo : ${tanggalId(t.tanggalJatuhTempo)}</span></div>
                <div class="kv">${icon("receipt", 18)}<span>Biaya Sewa : ${rupiah(t.totalBiayaSewa)} (${t.durasiHari} hr)</span></div>
                <div class="kv">${icon("card", 18)}<span>Deposit : ${rupiah(t.totalDeposit)}</span></div>
                $extra
                <div class="total"><span>Total Tagihan</span><b>${rupiah(t.grandTotalBayar)}</b></div>
                $qr
            </article>
            """.trimIndent()
        }.ifEmpty {
            """<div class="glass empty">Belum ada transaksi sewa. Silakan pilih alat di tab Beranda.</div>"""
        }

        return """
        <section class="glass wallet">
            <div class="wic">${icon("wallet", 36)}</div>
            <div><small>Saldo Refund Deposit</small><strong>${rupiah(saldoRefund)}</strong></div>
        </section>
        <h2 class="sec-t">${icon("file", 24)} Riwayat &amp; Tagihan</h2>
        $listHtml
        """.trimIndent()
    }

    private fun renderMitraPage(service: CampInService, mitra: Penyedia): String {
        val statusVerif = if (mitra.terverifikasi)
            "<span class='pill ok'>✓ Terverifikasi</span>"
        else
            "<span class='pill bad'>✗ Menunggu Verifikasi Admin</span>"

        val pesananMitra = service.transaksiByPenyedia(mitra.id).filter { it.sedangDisewa }.joinToString("") { t ->
            """
            <div class="li">
                <b>${escapeHtml(t.id)} • Alat: ${escapeHtml(t.alatId)} (${t.jumlahUnit} unit)</b>
                <div class="muted">Penyewa: ${escapeHtml(t.penyewaId)} | Jatuh Tempo: ${tanggalId(t.tanggalJatuhTempo)} | Total: ${rupiah(t.grandTotalBayar)}</div>
                <form action="/kembali" method="post" class="inline" style="margin-top:8px">
                    <input type="hidden" name="trxId" value="${escapeHtml(t.id)}">
                    <label class="field sm"><input type="number" name="telat" value="0" min="0" placeholder="Hari telat"></label>
                    <button class="btn-lime sm">Terima Pengembalian &amp; Cairkan Saldo</button>
                </form>
            </div>
            """.trimIndent()
        }.ifEmpty { "<p class='muted' style='font-size:13px'>Belum ada alat toko Anda yang sedang disewa.</p>" }

        val daftarAlatToko = service.alatMilikPenyedia(mitra.id).joinToString("") { a ->
            """<div class="li row-b">
                <span><b>${escapeHtml(a.id)}</b> - ${escapeHtml(a.nama)} (${rupiah(a.hargaSewaPerHari)}/hr)</span>
                <span class="pill ${if (a.tersedia) "ok" else "warn"}">${if (a.tersedia) "Stok: ${a.stok} unit" else "Habis"}</span>
               </div>"""
        }.ifEmpty { "<p class='muted' style='font-size:13px'>Anda belum menambahkan alat.</p>" }

        return """
        <section class="glass hl">
            <div class="row-b"><b>${escapeHtml(mitra.namaToko)}</b>$statusVerif</div>
            <small style="margin-top:10px">Saldo Dompet Mitra (90% Bagi Hasil + Denda)</small>
            <strong>${rupiah(mitra.saldoDompet)}</strong>
        </section>

        <section class="glass pad stack">
            <div class="ph">${icon("clipboard")} Pesanan Aktif &amp; Pengembalian</div>
            $pesananMitra
        </section>

        <section class="glass pad stack">
            <div class="ph">${icon("plus")} Tambah Alat Rental Baru</div>
            <form action="/tambah-alat" method="post" class="stack">
                <label class="field"><select name="kategori">
                    <option value="TENDA">Tenda Camping</option>
                    <option value="CARRIER">Tas Carrier</option>
                    <option value="SLEEPING">Perlengkapan Tidur (SB)</option>
                </select></label>
                <label class="field"><input name="id" placeholder="ID Alat Baru (contoh: ALT-05)" required></label>
                <label class="field"><input name="nama" placeholder="Nama Seri Alat" required></label>
                <label class="field"><input name="stokAwal" type="number" min="1" value="3" placeholder="Jumlah Stok Awal" required></label>
                <label class="field"><input name="harga" type="number" placeholder="Harga Sewa per Hari (Rp)" required></label>
                <label class="field"><input name="spekInt" type="number" placeholder="Kapasitas Orang (Tenda) / Liter (Carrier) / Suhu °C (SB)" required></label>
                <label class="field"><input name="spekTeks" placeholder="Double Layer (Tenda) / true (Raincover) / Polar (SB)" required></label>
                <button class="btn-lime">Simpan Alat ke Database</button>
            </form>
        </section>

        <section class="glass pad stack">
            <div class="ph">${icon("tent")} Inventaris Toko Anda</div>
            $daftarAlatToko
        </section>
        """.trimIndent()
    }

    private fun renderAdminPage(service: CampInService, admin: AdminPlatform): String {
        val daftarMitra = service.semuaPengguna().filterIsInstance<Penyedia>().joinToString("") { m ->
            val aksi = if (!m.terverifikasi) {
                """
                <form action="/verifikasi" method="post">
                    <input type="hidden" name="penyediaId" value="${escapeHtml(m.id)}">
                    <button class="btn-amber">Verifikasi Mitra</button>
                </form>
                """.trimIndent()
            } else "<span class='pill ok'>✓ Terverifikasi</span>"

            """
            <div class="li row-b">
                <div>
                    <b>${escapeHtml(m.namaToko)} (${escapeHtml(m.id)})</b>
                    <div class="muted">Pemilik: ${escapeHtml(m.nama)} | Saldo: <b>${rupiah(m.saldoDompet)}</b></div>
                </div>
                $aksi
            </div>
            """.trimIndent()
        }

        val semuaUserHtml = service.semuaPengguna().joinToString("") { u ->
            """<div class="li">${escapeHtml(u.infoSingkat())}</div>"""
        }

        return """
        <section class="glass hl admin">
            <small>Total Pendapatan Komisi Platform (10% Transaksi Selesai)</small>
            <strong>${rupiah(service.totalKomisiPlatform)}</strong>
            <small style="margin-top:8px">Admin Aktif: ${escapeHtml(admin.nama)} (${escapeHtml(admin.levelAkses)})</small>
        </section>

        <section class="glass pad stack">
            <div class="ph">${icon("shield")} Verifikasi KYC Mitra Penyedia</div>
            $daftarMitra
        </section>

        <section class="glass pad stack">
            <div class="ph">${icon("user")} Audit Seluruh Pengguna (${service.semuaPengguna().size} Akun)</div>
            $semuaUserHtml
        </section>
        """.trimIndent()
    }
}
