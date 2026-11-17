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

object MobileWebUiServer {
    private var server: NettyApplicationEngine? = null

    fun start(service: CampInService, dbInfo: String, port: Int = 8080) {
        val kandidatPort = listOf(port, 8081, 8090)
        for (p in kandidatPort) {
            try {
                server = embeddedServer(Netty, host = "0.0.0.0", port = p) {
                    routing {
                        get("/") {
                            val msg = call.request.queryParameters["msg"]
                            val cuacaKode = call.request.queryParameters["gunung"]
                            val infoCuaca = if (!cuacaKode.isNullOrBlank()) {
                                CuacaGunungApi.ambilCuacaDanRekomendasi(cuacaKode)
                            } else null
                            call.respondText(renderMobileAppHtml(service, dbInfo, msg, infoCuaca), ContentType.Text.Html)
                        }
                        post("/sewa") {
                            val params = call.receiveParameters()
                            val msg = try {
                                val trx = service.sewaAlat(
                                    params["alatId"].orEmpty().trim(),
                                    params["penyewaId"].orEmpty().trim(),
                                    params["durasi"]?.toIntOrNull() ?: 1
                                )
                                "✓ Sewa Berhasil! ID: ${trx.id} | Total: Rp${trx.grandTotalBayar} | QRIS: ${trx.kodePembayaran}"
                            } catch (e: Exception) {
                                "! ${e.message}"
                            }
                            call.respondRedirect("/?msg=" + msg.encodeURLParameter())
                        }
                        post("/kembali") {
                            val params = call.receiveParameters()
                            val msg = try {
                                val trx = service.kembalikanAlat(
                                    params["trxId"].orEmpty().trim(),
                                    params["telat"]?.toIntOrNull() ?: 0
                                )
                                "✓ Alat ${trx.alatId} Dikembalikan! Denda: Rp${trx.dendaTerlambat}. Saldo Mitra cair 90%!"
                            } catch (e: Exception) {
                                "! ${e.message}"
                            }
                            call.respondRedirect("/?msg=" + msg.encodeURLParameter())
                        }
                        post("/tambah-alat") {
                            val params = call.receiveParameters()
                            val msg = try {
                                val kat = params["kategori"].orEmpty()
                                val id = params["id"].orEmpty().trim()
                                val mitra = params["idPenyedia"].orEmpty().trim()
                                val nama = params["nama"].orEmpty().trim()
                                val harga = params["harga"]?.toIntOrNull() ?: 0
                                val spekInt = params["spekInt"]?.toIntOrNull() ?: 2
                                val spekTeks = params["spekTeks"].orEmpty().trim()
                                val alat = when (kat) {
                                    "TENDA" -> Tenda(id, nama, harga, mitra, spekInt, spekTeks)
                                    "CARRIER" -> TasCarrier(id, nama, harga, mitra, spekInt, spekTeks.equals("true", true))
                                    else -> PerlengkapanTidur(id, nama, harga, mitra, spekInt, spekTeks)
                                }
                                service.tambahAlat(alat)
                                "✓ Alat ${alat.nama} berhasil disimpan!"
                            } catch (e: Exception) {
                                "! ${e.message}"
                            }
                            call.respondRedirect("/?msg=" + msg.encodeURLParameter())
                        }
                        post("/verifikasi") {
                            val params = call.receiveParameters()
                            val msg = try {
                                service.verifikasiPenyedia("ADM-01", params["penyediaId"].orEmpty().trim())
                                "✓ Mitra berhasil diverifikasi oleh Admin!"
                            } catch (e: Exception) {
                                "! ${e.message}"
                            }
                            call.respondRedirect("/?msg=" + msg.encodeURLParameter())
                        }
                    }
                }.start(wait = false)
                println("   🌐 [AKTIF] Buka Mobile UI di: http://localhost:$p")
                return
            } catch (e: Exception) {
                println("   ! Port $p tidak tersedia (${e.message}), mencoba port berikutnya...")
            }
        }
    }

    fun stop() {
        server?.stop(500, 1000)
    }

    private fun renderMobileAppHtml(
        service: CampInService,
        dbInfo: String,
        pesan: String?,
        infoCuaca: String?
    ): String {
        val kartuAlat = service.semuaAlat().joinToString("") { a ->
            val badge = if (a.tersedia)
                "<span class='px-2.5 py-1 text-xs rounded-full bg-emerald-100 text-emerald-800 font-bold'>TERSEDIA</span>"
            else
                "<span class='px-2.5 py-1 text-xs rounded-full bg-amber-100 text-amber-800 font-bold'>DISEWA</span>"
            """
            <div class="bg-white p-4 rounded-2xl shadow-sm border border-stone-200 mb-3">
                <div class="flex justify-between items-center mb-1">
                    <span class="text-xs font-bold text-[#3A5A40]">${a.id} • ${a.kategori()} (Mitra: ${a.idPenyedia})</span>
                    $badge
                </div>
                <h3 class="font-bold text-base text-stone-800">${a.nama}</h3>
                <p class="text-xs text-stone-500 mt-1">${a.deskripsi()}</p>
                <div class="flex justify-between items-center mt-3 pt-2 border-t border-stone-100">
                    <div>
                        <span class="text-sm font-extrabold text-[#233D2C]">Rp${a.hargaSewaPerHari}/hari</span>
                        <span class="block text-[11px] text-stone-500">Deposit: Rp${a.hitungDeposit()}</span>
                    </div>
                </div>
            </div>
            """.trimIndent()
        }

        val kartuMitra = service.semuaPengguna().filterIsInstance<Penyedia>().joinToString("") { m ->
            val tombolVerif = if (!m.terverifikasi) {
                """
                <form action="/verifikasi" method="post">
                    <input type="hidden" name="penyediaId" value="${m.id}">
                    <button class="bg-[#BC6C25] text-white text-xs px-3 py-1.5 rounded-lg font-semibold">Verifikasi Mitra</button>
                </form>
                """.trimIndent()
            } else "<span class='text-xs text-emerald-700 font-bold'>✓ Terverifikasi</span>"

            """
            <div class="bg-white p-3.5 rounded-xl border border-stone-200 flex justify-between items-center mb-2">
                <div>
                    <div class="font-bold text-sm text-stone-800">${m.namaToko} (${m.id})</div>
                    <div class="text-xs text-stone-500">Pemilik: ${m.nama} | Saldo Escrow: <b class="text-emerald-700">Rp${m.saldoDompet}</b></div>
                </div>
                $tombolVerif
            </div>
            """.trimIndent()
        }

        val kartuTransaksi = service.transaksiAktif().joinToString("") { t ->
            """
            <div class="bg-amber-50 p-3.5 rounded-xl border border-amber-200 mb-2">
                <div class="font-bold text-sm text-stone-800">${t.id} • Alat: ${t.alatId} (Penyewa: ${t.penyewaId})</div>
                <div class="text-xs text-stone-600">Total Tagihan: Rp${t.grandTotalBayar} (${t.durasiHari} hari)</div>
                <div class="text-[11px] text-emerald-800 truncate mt-1">QRIS: ${t.kodePembayaran}</div>
                <form action="/kembali" method="post" class="mt-2 flex gap-2">
                    <input type="hidden" name="trxId" value="${t.id}">
                    <input type="number" name="telat" value="0" min="0" placeholder="Hari telat" class="w-24 text-xs p-1.5 border rounded">
                    <button class="bg-[#233D2C] text-white text-xs px-3 py-1.5 rounded-lg font-semibold">Kembalikan Alat</button>
                </form>
            </div>
            """.trimIndent()
        }.ifEmpty { "<p class='text-xs text-stone-500'>Tidak ada transaksi aktif saat ini.</p>" }

        val bannerNotif = if (!pesan.isNullOrBlank()) {
            "<div class='mb-4 p-3.5 rounded-xl bg-[#233D2C] text-[#FEFAE0] text-xs font-semibold shadow'>$pesan</div>"
        } else ""

        val boxCuaca = if (!infoCuaca.isNullOrBlank()) {
            "<pre class='mt-3 p-3 rounded-xl bg-emerald-950 text-emerald-100 text-xs whitespace-pre-wrap'>$infoCuaca</pre>"
        } else ""

        return """
        <!DOCTYPE html>
        <html lang="id">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>CampIn Mobile App</title>
            <script src="https://cdn.tailwindcss.com"></script>
        </head>
        <body class="bg-[#ECE8D9] min-h-screen flex justify-center">
            <div class="w-full max-w-md bg-[#F7F4EB] min-h-screen shadow-2xl flex flex-col justify-between">
                <header class="bg-[#233D2C] text-[#FEFAE0] p-4 sticky top-0 z-10 shadow-md">
                    <div class="flex justify-between items-center">
                        <div>
                            <h1 class="text-lg font-extrabold tracking-tight">⛺ CampIn Marketplace</h1>
                            <p class="text-[11px] text-emerald-200">Engine: $dbInfo</p>
                        </div>
                        <span class="text-xs bg-[#3A5A40] px-2.5 py-1 rounded-full font-bold">Komisi: Rp${service.totalKomisiPlatform}</span>
                    </div>
                </header>

                <main class="p-4 flex-1 overflow-y-auto space-y-5">
                    $bannerNotif

                    <section>
                        <h2 class="text-xs font-extrabold uppercase tracking-wider text-[#3A5A40] mb-2">Cuaca Gunung Real-Time (Open-Meteo API)</h2>
                        <div class="grid grid-cols-2 gap-2">
                            <a href="/?gunung=1" class="bg-white border border-stone-300 text-center py-2 rounded-xl text-xs font-bold hover:bg-emerald-50">⛰️ Cikuray</a>
                            <a href="/?gunung=2" class="bg-white border border-stone-300 text-center py-2 rounded-xl text-xs font-bold hover:bg-emerald-50">⛰️ Burangrang</a>
                            <a href="/?gunung=3" class="bg-white border border-stone-300 text-center py-2 rounded-xl text-xs font-bold hover:bg-emerald-50">⛰️ Ciremai</a>
                            <a href="/?gunung=4" class="bg-white border border-stone-300 text-center py-2 rounded-xl text-xs font-bold hover:bg-emerald-50">⛰️ Rinjani</a>
                        </div>
                        $boxCuaca
                    </section>

                    <section>
                        <h2 class="text-xs font-extrabold uppercase tracking-wider text-[#3A5A40] mb-2">Katalog Alat Camping</h2>
                        $kartuAlat
                    </section>

                    <section class="bg-white p-4 rounded-2xl shadow-sm border border-stone-200">
                        <h2 class="text-sm font-bold text-[#233D2C] mb-3">Checkout Sewa Alat (Midtrans QRIS)</h2>
                        <form action="/sewa" method="post" class="space-y-2.5">
                            <input name="alatId" placeholder="ID Alat (contoh: ALT-01)" required class="w-full text-sm p-2.5 border rounded-xl bg-stone-50">
                            <input name="penyewaId" value="USR-01" placeholder="ID Penyewa (USR-01)" required class="w-full text-sm p-2.5 border rounded-xl bg-stone-50">
                            <input name="durasi" type="number" value="2" min="1" max="30" required class="w-full text-sm p-2.5 border rounded-xl bg-stone-50">
                            <button class="w-full bg-[#3A5A40] text-white font-bold py-2.5 rounded-xl text-sm shadow">Sewa & Bayar via QRIS</button>
                        </form>
                    </section>

                    <section>
                        <h2 class="text-xs font-extrabold uppercase tracking-wider text-[#3A5A40] mb-2">Transaksi Aktif & Pengembalian</h2>
                        $kartuTransaksi
                    </section>

                    <section>
                        <h2 class="text-xs font-extrabold uppercase tracking-wider text-[#3A5A40] mb-2">Mitra Penyedia & Escrow Admin</h2>
                        $kartuMitra
                    </section>

                    <section class="bg-white p-4 rounded-2xl shadow-sm border border-stone-200">
                        <h2 class="text-sm font-bold text-[#233D2C] mb-3">Panel Mitra: Tambah Alat Baru</h2>
                        <form action="/tambah-alat" method="post" class="space-y-2">
                            <select name="kategori" class="w-full text-sm p-2.5 border rounded-xl bg-stone-50">
                                <option value="TENDA">Tenda</option>
                                <option value="CARRIER">Tas Carrier</option>
                                <option value="SLEEPING">Perlengkapan Tidur</option>
                            </select>
                            <input name="id" placeholder="ID Alat Baru (misal: ALT-04)" required class="w-full text-sm p-2 border rounded-xl">
                            <input name="idPenyedia" value="MTR-01" placeholder="ID Mitra (MTR-01 / MTR-02)" required class="w-full text-sm p-2 border rounded-xl">
                            <input name="nama" placeholder="Nama Alat" required class="w-full text-sm p-2 border rounded-xl">
                            <input name="harga" type="number" placeholder="Harga Sewa per Hari (Rp)" required class="w-full text-sm p-2 border rounded-xl">
                            <input name="spekInt" type="number" placeholder="Kapasitas Orang / Liter / Suhu °C" required class="w-full text-sm p-2 border rounded-xl">
                            <input name="spekTeks" placeholder="Double Layer / true / Polar" required class="w-full text-sm p-2 border rounded-xl">
                            <button class="w-full bg-[#233D2C] text-white font-bold py-2.5 rounded-xl text-sm">Simpan Alat ke MySQL</button>
                        </form>
                    </section>
                </main>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}

