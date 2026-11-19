package ui

import model.*
import java.time.LocalDate

/**
 * Lapisan tampilan CAMPIN (UI saja).
 * Signature publik lama TIDAK berubah: escapeHtml, renderQrCodeSvg, pageShell.
 * Tidak lagi memakai Tailwind CDN -> semua gaya ada di themeCss (jalan tanpa internet,
 * kecuali foto latar & font Google yang punya fallback).
 */
object WebLayout {

    // Foto latar. Ganti URL ini kalau mau foto lain (fallback gradien otomatis tampil jika gagal dimuat).
    const val IMG_LOGIN = "https://images.unsplash.com/photo-1478131143081-80f7f84ca84d?auto=format&fit=crop&w=1000&q=80"
    const val IMG_HOME = "https://images.unsplash.com/photo-1448375240586-882707db888b?auto=format&fit=crop&w=1000&q=80"

    fun escapeHtml(text: String?): String {
        if (text == null) return ""
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
    }

    /** Format Rupiah ala mockup: Rp130.000 */
    fun rupiah(n: Int): String {
        val s = kotlin.math.abs(n).toString().reversed().chunked(3).joinToString(".").reversed()
        return (if (n < 0) "-Rp" else "Rp") + s
    }

    private val BULAN = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")

    /** Format tanggal ala mockup: 18 Nov 2026 */
    fun tanggalId(d: LocalDate): String = "${d.dayOfMonth} ${BULAN[d.monthValue - 1]} ${d.year}"

    // ------------------------------------------------------------------ IKON
    private val ICONS = mapOf(
        "home" to """<path d="m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/>""",
        "bell" to """<path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>""",
        "user" to """<path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>""",
        "search" to """<circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/>""",
        "sliders" to """<line x1="21" x2="14" y1="4" y2="4"/><line x1="10" x2="3" y1="4" y2="4"/><line x1="21" x2="12" y1="12" y2="12"/><line x1="8" x2="3" y1="12" y2="12"/><line x1="21" x2="16" y1="20" y2="20"/><line x1="12" x2="3" y1="20" y2="20"/><line x1="14" x2="14" y1="2" y2="6"/><line x1="8" x2="8" y1="10" y2="14"/><line x1="16" x2="16" y1="18" y2="22"/>""",
        "chevron-right" to """<path d="m9 18 6-6-6-6"/>""",
        "chevron-left" to """<path d="m15 18-6-6 6-6"/>""",
        "cart" to """<circle cx="8" cy="21" r="1"/><circle cx="19" cy="21" r="1"/><path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12"/>""",
        "pin" to """<path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/>""",
        "wallet" to """<path d="M19 7V4a1 1 0 0 0-1-1H5a2 2 0 0 0 0 4h15a1 1 0 0 1 1 1v4h-3a2 2 0 0 0 0 4h3a1 1 0 0 0 1-1v-2a1 1 0 0 0-1-1"/><path d="M3 5v14a2 2 0 0 0 2 2h15a1 1 0 0 0 1-1v-4"/>""",
        "file" to """<path d="M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M10 9H8"/><path d="M16 13H8"/><path d="M16 17H8"/>""",
        "calendar" to """<path d="M8 2v4"/><path d="M16 2v4"/><rect width="18" height="18" x="3" y="4" rx="2"/><path d="M3 10h18"/>""",
        "clock" to """<circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>""",
        "receipt" to """<path d="M4 2v20l2-1 2 1 2-1 2 1 2-1 2 1 2-1 2 1V2l-2 1-2-1-2 1-2-1-2 1-2-1-2 1Z"/><path d="M16 8h-6a2 2 0 1 0 0 4h4a2 2 0 1 1 0 4H8"/><path d="M12 17.5v-11"/>""",
        "card" to """<rect width="20" height="14" x="2" y="5" rx="2"/><line x1="2" x2="22" y1="10" y2="10"/>""",
        "shield" to """<path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z"/><path d="m9 12 2 2 4-4"/>""",
        "mail" to """<rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/>""",
        "lock" to """<rect width="18" height="11" x="3" y="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>""",
        "eye-off" to """<path d="M9.88 9.88a3 3 0 1 0 4.24 4.24"/><path d="M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68"/><path d="M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61"/><line x1="2" x2="22" y1="2" y2="22"/>""",
        "zap" to """<path d="M4 14a1 1 0 0 1-.78-1.63l9.9-10.2a.5.5 0 0 1 .86.46l-1.92 6.02A1 1 0 0 0 13 10h7a1 1 0 0 1 .78 1.63l-9.9 10.2a.5.5 0 0 1-.86-.46l1.92-6.02A1 1 0 0 0 11 14z"/>""",
        "leaf" to """<path d="M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z"/><path d="M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12"/>""",
        "cloud-sun" to """<path d="M12 2v2"/><path d="m4.93 4.93 1.41 1.41"/><path d="M20 12h2"/><path d="m19.07 4.93-1.41 1.41"/><path d="M15.947 12.65a4 4 0 0 0-5.925-4.128"/><path d="M13 22H7a5 5 0 1 1 4.9-6H13a3 3 0 0 1 0 6Z"/>""",
        "info" to """<circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>""",
        "grid" to """<rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/>""",
        "tent" to """<path d="M3.5 21 14 3"/><path d="M20.5 21 10 3"/><path d="M15.5 21 12 15l-3.5 6"/><path d="M2 21h20"/>""",
        "backpack" to """<path d="M4 10a4 4 0 0 1 4-4h8a4 4 0 0 1 4 4v10a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2z"/><path d="M9 6V4a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/><path d="M8 21v-5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v5"/><path d="M8 10h8"/>""",
        "bed" to """<path d="M2 4v16"/><path d="M2 8h18a2 2 0 0 1 2 2v10"/><path d="M2 17h20"/><path d="M6 8v9"/>""",
        "clipboard" to """<rect width="8" height="4" x="8" y="2" rx="1" ry="1"/><path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"/><path d="M12 11h4"/><path d="M12 16h4"/><path d="M8 11h.01"/><path d="M8 16h.01"/>""",
        "plus" to """<path d="M5 12h14"/><path d="M12 5v14"/>"""
    )

    fun icon(name: String, size: Int = 20): String =
        """<svg class="i" width="$size" height="$size" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[name].orEmpty()}</svg>"""

    private val LOGO = """<svg class="logo" viewBox="0 0 44 34" aria-hidden="true"><path d="M0 34 17 3l10 16 5-7 12 22z" fill="#9be45a"/><path d="M17 3l6 10-6-3-6 3z" fill="#ffffff"/></svg>"""

    // ------------------------------------------------------------------ QR (visual simulasi, bukan QR yang bisa discan)
    fun renderQrCodeSvg(code: String): String {
        val n = 25
        var seed = code.hashCode().toLong() xor 0x5DEECE66DL
        fun next(): Boolean {
            seed = seed * 6364136223846793005L + 1442695040888963407L
            return ((seed ushr 33) and 1L) == 1L
        }

        val grid = Array(n) { BooleanArray(n) }
        for (r in 0 until n) for (c in 0 until n) grid[r][c] = next()

        fun finder(r0: Int, c0: Int) {
            for (dr in -1..7) for (dc in -1..7) {
                val r = r0 + dr
                val c = c0 + dc
                if (r !in 0 until n || c !in 0 until n) continue
                grid[r][c] = if (dr in 0..6 && dc in 0..6) {
                    dr == 0 || dr == 6 || dc == 0 || dc == 6 || (dr in 2..4 && dc in 2..4)
                } else false
            }
        }
        finder(0, 0)
        finder(0, n - 7)
        finder(n - 7, 0)

        for (dr in 0..4) for (dc in 0..4) {
            grid[16 + dr][16 + dc] = dr == 0 || dr == 4 || dc == 0 || dc == 4 || (dr == 2 && dc == 2)
        }
        for (i in 8..(n - 9)) {
            grid[6][i] = i % 2 == 0
            grid[i][6] = i % 2 == 0
        }

        val d = StringBuilder()
        for (r in 0 until n) for (c in 0 until n) {
            if (grid[r][c]) d.append("M${c + 2} ${r + 2}h1v1h-1z")
        }
        // Fallback visual (dipakai hanya jika pustaka QR gagal dimuat/offline). QR asli dirender JS dari data-qr.
        return """<div class="campin-qr" data-qr="${escapeHtml(code)}"><svg viewBox="0 0 29 29" shape-rendering="crispEdges" xmlns="http://www.w3.org/2000/svg"><rect width="29" height="29" fill="#ffffff"/><path d="$d" fill="#101a14"/></svg></div>"""
    }

    // ------------------------------------------------------------------ TEMA
    private val themeCss = """
<style>
:root{
  --bg:#0a1f16; --lime:#a3e85a; --lime2:#7ed13c; --ink:#07180f;
  --text:#f1f7ec; --muted:#b9ccbe; --line:rgba(255,255,255,.14);
  --glass:rgba(19,50,37,.58);
}
*{box-sizing:border-box;margin:0;padding:0;-webkit-tap-highlight-color:transparent}
html{background:#06130d}
body{font-family:'Outfit','Plus Jakarta Sans',ui-sans-serif,system-ui,-apple-system,'Segoe UI',sans-serif;color:var(--text);min-height:100vh;display:flex;justify-content:center;background:radial-gradient(ellipse at 20% 0%,#16402d,transparent 45%),#06130d;-webkit-font-smoothing:antialiased}
a{color:inherit;text-decoration:none}
button{font-family:inherit;cursor:pointer;border:0;color:inherit;background:none}
input,select{font-family:inherit}
svg.i{flex:none}
h1,h2,h3{font-weight:600}

.phone{position:relative;width:100%;max-width:430px;min-height:100vh;background:var(--bg);overflow:hidden;box-shadow:0 0 90px rgba(0,0,0,.6)}
.scene{position:absolute;top:0;left:0;right:0;z-index:0;background-size:cover;background-position:center top;background-repeat:no-repeat}
.scene::after{content:"";position:absolute;inset:0;background:linear-gradient(180deg,rgba(6,22,15,.40) 0%,rgba(6,22,15,.10) 30%,rgba(10,31,22,.82) 78%,var(--bg) 100%)}
.scene-login{height:820px;background-image:url($IMG_LOGIN),linear-gradient(160deg,#2b5a3a,#0a1f16)}
.scene-home{height:540px;background-image:url($IMG_HOME),linear-gradient(160deg,#1f4a35,#0a1f16)}
.scene-sewa{height:480px;background-image:url($IMG_HOME),linear-gradient(160deg,#173d2b,#0a1f16);background-position:center}

.top{position:relative;display:flex;align-items:center;justify-content:space-between;padding:26px 20px 8px}
.brand{display:flex;align-items:center;gap:8px;font-weight:700;font-size:22px;letter-spacing:.3px}
.logo{width:40px;height:32px}
.chip-top{display:inline-flex;align-items:center;gap:6px;padding:8px 14px;border-radius:999px;background:rgba(18,48,35,.62);border:1px solid var(--line);font-size:13px;-webkit-backdrop-filter:blur(10px);backdrop-filter:blur(10px)}
.chip-top svg{color:var(--lime)}
.circle{width:44px;height:44px;border-radius:50%;display:grid;place-items:center;background:rgba(18,48,35,.62);border:1px solid var(--line);-webkit-backdrop-filter:blur(10px);backdrop-filter:blur(10px)}
.top-title{font-size:21px;font-weight:600}

.main{position:relative;padding:8px 20px 130px;display:flex;flex-direction:column;gap:16px}
.glass{background:var(--glass);border:1px solid var(--line);border-radius:22px;-webkit-backdrop-filter:blur(16px);backdrop-filter:blur(16px);box-shadow:0 10px 30px rgba(0,0,0,.25)}
.pad{padding:16px}
.stack{display:flex;flex-direction:column;gap:12px}
.muted{color:var(--muted)}
.center{text-align:center}
.vh{position:absolute;opacity:0;pointer-events:none}

.flash{padding:12px 16px;border-radius:16px;font-size:13px;font-weight:500;border:1px solid;line-height:1.45}
.flash.ok{background:rgba(24,80,48,.72);border-color:rgba(163,232,90,.38)}
.flash.err{background:rgba(120,30,30,.72);border-color:rgba(255,120,120,.42)}

/* ---------- form ---------- */
.field{display:flex;align-items:center;gap:12px;padding:0 16px;height:52px;border-radius:16px;background:rgba(255,255,255,.07);border:1px solid var(--line);color:var(--muted)}
.field input,.field select{flex:1;min-width:0;height:100%;background:transparent;border:0;outline:0;color:var(--text);font-size:15px}
.field input::placeholder{color:#9db3a3}
.field select option{background:#12362a;color:#f1f7ec}
.field:focus-within{border-color:var(--lime);box-shadow:0 0 0 3px rgba(163,232,90,.14)}
.field.sm{height:44px;width:92px;flex:none;padding:0 12px}
.eye{display:grid;place-items:center;color:var(--muted)}
.btn-lime{display:flex;align-items:center;justify-content:center;gap:8px;width:100%;height:54px;border-radius:999px;background:linear-gradient(90deg,#86d943,#b2ee63);color:#0a2512;font-weight:600;font-size:17px;box-shadow:0 8px 24px rgba(130,220,60,.25)}
.btn-lime.sm{height:44px;font-size:13px;padding:0 14px;width:auto;flex:1}
.btn-dark{display:flex;align-items:center;justify-content:center;gap:8px;width:100%;height:40px;margin-top:10px;border-radius:999px;background:rgba(10,32,22,.78);border:1px solid var(--line);font-size:14px;font-weight:500}
.btn-dark.off{color:var(--muted);opacity:.7}
.btn-amber{padding:9px 14px;border-radius:999px;background:#f3a82c;color:#2a1a00;font-weight:600;font-size:12px}
.ph{display:flex;align-items:center;gap:10px;font-size:17px;font-weight:600}
.ph svg{color:var(--lime)}
.sec-t{display:flex;align-items:center;gap:10px;font-size:19px;font-weight:600}
.sec-t svg{color:var(--lime)}
.pill{display:inline-block;padding:4px 10px;border-radius:999px;font-size:11px;font-weight:600}
.pill.ok{background:rgba(125,209,60,.22);color:#c6f19c}
.pill.warn{background:rgba(245,165,36,.22);color:#ffd18a}
.pill.bad{background:rgba(255,107,107,.2);color:#ffb3b3}
.li{padding:12px 14px;border-radius:16px;background:rgba(255,255,255,.06);border:1px solid var(--line);font-size:13px;line-height:1.5}
.li b{font-weight:600}
.row-b{display:flex;justify-content:space-between;align-items:center;gap:10px}
.inline{display:flex;gap:10px;align-items:center;margin-top:10px}

/* ---------- login ---------- */
.hero{padding-top:190px}
.hero h1{font-size:54px;line-height:1.02;font-weight:600}
.hero h1 b{color:var(--lime);font-weight:700}
.hero p{margin-top:14px;font-size:17px;line-height:1.5;color:#e5efe3;max-width:300px}
.quick{display:flex;align-items:center;gap:14px;width:100%;padding:12px 14px;border-radius:18px;background:rgba(255,255,255,.08);border:1px solid var(--line);text-align:left;color:var(--text)}
.quick-t{flex:1;min-width:0}
.quick b{display:block;font-size:16px;font-weight:600}
.quick small{display:block;font-size:13px;color:var(--muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.avatar{width:44px;height:44px;border-radius:50%;background:rgba(255,255,255,.1);display:grid;place-items:center;flex:none}
.avatar.big{width:56px;height:56px}
.signup{text-align:center;font-size:14px}
.signup a{color:var(--lime);text-decoration:underline;font-weight:500}
.reg{display:none}
.reg:target{display:flex}
.dbinfo{position:relative;text-align:center;font-size:11px;color:rgba(185,204,190,.6);padding-bottom:110px}

/* ---------- beranda ---------- */
.h-title{font-size:36px;line-height:1.08;font-weight:500;margin-top:4px}
.h-title b{display:block;color:var(--lime);font-weight:700}
.wx{overflow:hidden}
.wx summary{list-style:none;display:flex;align-items:center;gap:14px;padding:16px 18px;cursor:pointer}
.wx summary::-webkit-details-marker{display:none}
.wx-l{display:flex;align-items:center;gap:10px;flex:1}
.wx-l svg{color:#fff}
.wx-t{font-size:26px;font-weight:600;line-height:1}
.wx-s{font-size:13px;color:var(--muted);margin-top:3px}
.wx-div{width:1px;align-self:stretch;background:var(--line)}
.wx-r{display:flex;align-items:center;gap:10px;flex:1;min-width:0}
.wx-r svg{color:var(--lime)}
.wx-r b{display:block;font-size:14px;font-weight:600}
.wx-r small{font-size:12px;color:var(--muted)}
.wx-body{padding:0 18px 16px;border-top:1px solid var(--line)}
.wx-grid{display:grid;grid-template-columns:1fr 1fr;gap:8px;padding-top:14px}
.wx-grid a{text-align:center;padding:9px 6px;border-radius:999px;background:rgba(255,255,255,.08);border:1px solid var(--line);font-size:13px;font-weight:500}
.wx pre{margin-top:12px;padding:12px;border-radius:14px;background:rgba(0,0,0,.28);color:#dff3d5;font-size:12px;white-space:pre-wrap;font-family:ui-monospace,Menlo,Consolas,monospace}
.search{display:flex;gap:12px;align-items:center}
.search .field{flex:1;border-radius:999px;height:54px}
.btn-circ{width:54px;height:54px;border-radius:50%;display:grid;place-items:center;flex:none;background:rgba(28,66,48,.78);border:1px solid var(--line)}
.chips{display:flex;gap:10px;overflow-x:auto;scrollbar-width:none;margin:0 -20px;padding:0 20px}
.chips::-webkit-scrollbar{display:none}
.chip{display:inline-flex;align-items:center;gap:7px;padding:10px 16px;border-radius:999px;background:rgba(18,48,35,.66);border:1px solid var(--line);font-size:14px;white-space:nowrap}
.chip.on{background:var(--lime);color:var(--ink);font-weight:600;border-color:transparent}
.catalog{display:flex;flex-direction:column;gap:14px}
.sec-h{display:flex;justify-content:space-between;align-items:center}
.sec-h h2{display:flex;align-items:center;gap:10px;font-size:18px}
.sec-h h2 svg{color:var(--lime)}
.sec-h label{display:inline-flex;align-items:center;gap:4px;font-size:14px;cursor:pointer}
.sec-h .b{display:none}
#all:checked ~ .sec-h .a{display:none}
#all:checked ~ .sec-h .b{display:inline}
.rail{display:flex;gap:14px;overflow-x:auto;scroll-snap-type:x mandatory;margin:0 -20px;padding:0 20px 6px;scrollbar-width:none}
.rail::-webkit-scrollbar{display:none}
#all:checked ~ .rail{flex-wrap:wrap;overflow:visible;margin:0;padding:0}
.pcard{flex:0 0 172px;scroll-snap-align:start;padding:8px 8px 12px}
#all:checked ~ .rail .pcard{flex:0 0 calc(50% - 7px)}
.pimg{position:relative;height:118px;border-radius:14px;overflow:hidden;background:linear-gradient(145deg,#2a6a43,#0f2e20)}
.pimg img{display:block;width:100%;height:100%;object-fit:cover}
.tag{position:absolute;top:8px;right:8px;display:inline-flex;align-items:center;gap:4px;padding:4px 9px;border-radius:999px;background:rgba(104,200,60,.95);color:#06210f;font-size:11px;font-weight:600}
.tag.off{background:rgba(243,168,44,.95);color:#2a1a00}
.pname{margin:10px 4px 4px;font-size:14.5px;line-height:1.3;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.pprice{margin:0 4px}
.pprice b{font-size:20px;font-weight:600;color:var(--lime)}
.pprice span{font-size:13px;color:var(--muted)}
.pdep{display:flex;align-items:center;gap:6px;margin:4px 4px 0;font-size:12px;color:var(--muted)}
.empty{padding:26px;text-align:center;font-size:14px;color:var(--muted)}

/* ---------- sewaan ---------- */
.wallet{display:flex;align-items:center;gap:16px;padding:20px;overflow:hidden;background:linear-gradient(90deg,rgba(15,45,32,.9),rgba(15,45,32,.5)),url($IMG_HOME) center/cover,rgba(19,50,37,.58)}
.wic{width:72px;height:72px;border-radius:50%;display:grid;place-items:center;flex:none;background:rgba(112,200,70,.32);border:1px solid rgba(163,232,90,.4);color:var(--lime)}
.wallet small{font-size:16px;color:#e8f3e5}
.wallet strong{display:block;font-size:40px;font-weight:600;line-height:1.1}
.trx{position:relative;margin-top:12px;padding:20px 18px 18px}
.trx h3{font-size:16px}
.state{position:absolute;top:-13px;right:16px;padding:6px 16px;border-radius:10px;font-size:13px;font-weight:600;background:#f3a82c;color:#2a1a00}
.state.done{background:var(--lime);color:var(--ink)}
.kv{display:flex;align-items:center;gap:10px;margin-top:10px;font-size:15px;color:#e9f2e6}
.kv svg{color:var(--muted)}
.total{display:flex;justify-content:space-between;align-items:center;margin-top:16px;padding-top:14px;border-top:1px solid var(--line);font-size:15px}
.total b{font-size:28px;font-weight:700;color:var(--lime)}
.qr{margin-top:16px;padding:12px 8px 14px;border-radius:18px;background:#fff;color:#111;text-align:center}
.qr b{display:block;margin-top:10px;font-size:14px}
.qr span{display:block;font-size:14px;font-weight:600;word-break:break-all}
.campin-qr{margin:0 auto;text-align:center}
.campin-qr svg{display:block;width:150px;height:150px;margin:0 auto}
.campin-qr img{display:inline-block;max-width:100%;height:auto;image-rendering:pixelated}
.scan{display:flex;gap:6px;justify-content:center;align-items:flex-start;margin-top:12px;font-size:12px;line-height:1.45;color:var(--muted);text-align:center}
.scan svg{margin-top:1px}

/* ---------- mitra & admin ---------- */
.hl{padding:18px;background:linear-gradient(130deg,rgba(28,80,56,.85),rgba(10,42,30,.85))}
.hl small{display:block;font-size:13px;color:#cfe6d0}
.hl strong{display:block;margin-top:2px;font-size:30px;font-weight:700}
.hl.admin{background:linear-gradient(130deg,rgba(30,60,50,.9),rgba(16,39,31,.9))}

/* ---------- nav bawah ---------- */
.nav{position:fixed;bottom:0;left:50%;transform:translateX(-50%);width:100%;max-width:430px;z-index:50;padding:0 12px calc(10px + env(safe-area-inset-bottom))}
.nav-in{display:flex;align-items:flex-end;height:78px;padding:0 6px 8px;border-radius:26px;background:rgba(8,26,18,.9);border:1px solid var(--line);-webkit-backdrop-filter:blur(18px);backdrop-filter:blur(18px);box-shadow:0 -6px 30px rgba(0,0,0,.35)}
.nav a{flex:1;display:flex;flex-direction:column;align-items:center;gap:5px;font-size:12px;color:#c9d8cc}
.nav .ic{width:28px;height:28px;display:grid;place-items:center}
.nav a.on{color:var(--lime);font-weight:600}
.nav a.on .ic{width:52px;height:52px;margin-top:-26px;border-radius:50%;background:linear-gradient(145deg,#a8ec5f,#6fc233);color:#0a2a12;box-shadow:0 0 0 6px rgba(8,26,18,.92),0 8px 20px rgba(120,210,60,.35)}

/* ---------- sheet (modal bawah, tanpa JS) ---------- */
.sheet{display:none;position:fixed;inset:0;z-index:80;align-items:flex-end;justify-content:center}
.sheet:target{display:flex}
.sheet-bg{position:absolute;inset:0;background:rgba(2,10,6,.72);-webkit-backdrop-filter:blur(4px);backdrop-filter:blur(4px)}
.sheet-box{position:relative;width:100%;max-width:430px;max-height:88vh;overflow:auto;padding:22px 20px calc(26px + env(safe-area-inset-bottom));border-radius:28px 28px 0 0;background:linear-gradient(180deg,#14392a,#0b2218);border:1px solid var(--line);border-bottom:0;animation:up .25s ease;display:flex;flex-direction:column;gap:12px}
@keyframes up{from{transform:translateY(40px);opacity:0}to{transform:none;opacity:1}}
.grab{width:44px;height:5px;border-radius:9px;background:rgba(255,255,255,.25);margin:-8px auto 4px}
.acc{display:flex;align-items:center;gap:14px}
.acc small{display:block;color:var(--muted);font-size:13px}
</style>
""".trimIndent()

    // ------------------------------------------------------------------ KERANGKA HALAMAN
    fun pageShell(
        title: String,
        activeTab: String,
        currentUser: Pengguna?,
        dbInfo: String,
        flashMsg: String?,
        contentHtml: String
    ): String {
        val isError = flashMsg?.startsWith("!") == true
        val bannerHtml = if (!flashMsg.isNullOrBlank()) {
            """<div class="flash ${if (isError) "err" else "ok"}">${escapeHtml(flashMsg)}</div>"""
        } else ""

        val sceneClass = when (activeTab) {
            "login" -> "scene-login"
            "sewa" -> "scene-sewa"
            else -> "scene-home"
        }

        val headerHtml = if (activeTab == "sewa") {
            """
            <header class="top">
                <a class="circle" href="/" onclick="if(history.length>1){history.back();return false;}" aria-label="Kembali">${icon("chevron-left", 22)}</a>
                <div class="top-title">${escapeHtml(title)}</div>
                <a class="circle" href="/" aria-label="Beranda">${icon("home", 22)}</a>
            </header>
            """
        } else {
            val right = if (currentUser == null) {
                """<span class="chip-top">${icon("leaf", 16)} Outdoor P2P</span>"""
            } else {
                """<a class="circle" href="#akun" aria-label="Akun">${icon("user", 22)}</a>"""
            }
            """
            <header class="top">
                <div class="brand">$LOGO<span>CAMPIN</span></div>
                $right
            </header>
            """
        }

        val dbFooter = if (currentUser == null) """<p class="dbinfo">DB: ${escapeHtml(dbInfo)}</p>""" else ""

        return """<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
<meta name="theme-color" content="#0a1f16">
<title>${escapeHtml(title)} — CampIn</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap" rel="stylesheet">
$themeCss
</head>
<body>
<div class="phone">
    <div class="scene $sceneClass"></div>
    $headerHtml
    <main class="main">
        $bannerHtml
        $contentHtml
    </main>
    $dbFooter
    ${renderBottomNav(currentUser, activeTab)}
    ${renderAccountSheets(currentUser, dbInfo, flashMsg)}
</div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/qrcode-generator/1.4.4/qrcode.min.js"></script>
<script>
(function(){
  if (typeof qrcode !== 'function') return;
  document.querySelectorAll('[data-qr]').forEach(function(el){
    try {
      var q = qrcode(0, 'M');
      q.addData(el.getAttribute('data-qr'));
      q.make();
      el.innerHTML = '<img alt="QR pembayaran" src="' + q.createDataURL(5, 16) + '">';
    } catch (e) {}
  });
})();
</script>
</body>
</html>"""
    }

    private fun navItem(href: String, iconName: String, label: String, on: Boolean): String =
        """<a href="$href" class="${if (on) "on" else ""}"><span class="ic">${icon(iconName, 24)}</span><span>$label</span></a>"""

    private fun renderBottomNav(user: Pengguna?, activeTab: String): String {
        if (user == null) return ""
        val first = when (user) {
            is Penyewa ->
                navItem("/katalog", "home", "Beranda", activeTab == "katalog") +
                        navItem("/sewa-saya", "clipboard", "Sewaan", activeTab == "sewa")
            is Penyedia ->
                navItem("/mitra", "home", "Toko", activeTab == "mitra") +
                        navItem("/katalog", "search", "Katalog", activeTab == "katalog")
            is AdminPlatform ->
                navItem("/admin", "shield", "Admin", activeTab == "admin") +
                        navItem("/katalog", "search", "Katalog", activeTab == "katalog")
        }
        val items = first +
                navItem("#notifikasi", "bell", "Notifikasi", false) +
                navItem("#akun", "user", "Akun", false)
        return """<nav class="nav"><div class="nav-in">$items</div></nav>"""
    }

    /** Sheet Notifikasi & Akun (Akun berisi tombol Keluar -> POST /logout seperti sebelumnya). */
    private fun renderAccountSheets(user: Pengguna?, dbInfo: String, flashMsg: String?): String {
        if (user == null) return ""
        val notif = if (!flashMsg.isNullOrBlank()) {
            """<div class="li">${escapeHtml(flashMsg)}</div>"""
        } else {
            """<p class="muted center">Belum ada notifikasi baru.</p>"""
        }
        return """
<div id="notifikasi" class="sheet">
    <a href="#" class="sheet-bg" aria-label="Tutup"></a>
    <div class="sheet-box">
        <div class="grab"></div>
        <div class="ph">${icon("bell")} Notifikasi</div>
        $notif
        <a href="#" class="btn-dark">Tutup</a>
    </div>
</div>
<div id="akun" class="sheet">
    <a href="#" class="sheet-bg" aria-label="Tutup"></a>
    <div class="sheet-box">
        <div class="grab"></div>
        <div class="acc">
            <span class="avatar big">${icon("user", 26)}</span>
            <div><b>${escapeHtml(user.nama)}</b><small>${escapeHtml(user.email)}</small></div>
        </div>
        <div class="li">
            <div class="row-b"><span class="muted">Peran</span><b>${escapeHtml(user.peran())}</b></div>
            <div class="row-b"><span class="muted">ID Pengguna</span><b>${escapeHtml(user.id)}</b></div>
            <div class="row-b"><span class="muted">Database</span><b>${escapeHtml(dbInfo)}</b></div>
        </div>
        <form action="/logout" method="post">
            <button class="btn-lime">Ganti Akun / Keluar</button>
        </form>
        <a href="#" class="btn-dark">Tutup</a>
    </div>
</div>
"""
    }
}
