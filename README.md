# ⛺ CampIn — Peer-to-Peer Camping Gear Rental Marketplace

## Deskripsi
**CampIn** adalah aplikasi *Peer-to-Peer* (P2P) *Marketplace* penyewaan peralatan pendakian dan berkemah berbasis **Kotlin 2.0 (JVM)** yang menerapkan prinsip *Object-Oriented Programming* (OOP) secara menyeluruh. Sistem ini mempertemukan tiga peran pengguna (**Penyewa**, **Mitra Penyedia**, dan **Admin Platform**) dalam satu ekosistem penyewaan lengkap dengan verifikasi mitra, perhitungan deposit polimorfik, rekomendasi cuaca puncak gunung secara *real-time*, simulasi pembayaran QRIS Midtrans, serta mekanisme *escrow* bagi hasil dan pengembalian deposit (*refund*).

## Anggota Kelompok
| Nama Lengkap | NIM | Peran |
| :--- | :--- | :--- |
| Muhamad Huwaiza Rafi | 225443044 | Project Lead / System Architect |
| Viseila Azrivania Sumarna | 225443052 | Domain Developer |
| Moch Raflisya’id Marzuki N | 225443041 | Business Logic Developer |
| Naa’ila Zahra Assyifaa | 225443047 | UI / Mobile-Web & CLI Developer |
| Edra Christian D | 225443031 | QA + Data & API Developer |
| Dhiya Nasywa Nafi’ah Salsabila Marjaya | 225443030 | DevOps Engineer |

## Fitur Utama (Versi 2)
- [x] **Autentikasi Multi-Role & Manajemen Sesi**: Hashing password menggunakan **SHA-256** (`PasswordHasher`) dan pemisahan halaman berbasis *HTTP Cookie Session* (`/katalog`, `/sewa-saya`, `/mitra`, `/admin`) disertai tombol *1-Click Demo Login*.
- [x] **Verifikasi KYC Mitra oleh Admin**: Mitra baru wajib diverifikasi oleh Admin Platform sebelum dapat menambahkan katalog peralatan.
- [x] **Katalog Polimorfik & Perhitungan Deposit Otomatis**: Mendukung subclass `Tenda`, `TasCarrier`, dan `PerlengkapanTidur` dengan rumus deposit dan deskripsi spesifik tiap jenis alat.
- [x] **Telemetri Cuaca Gunung Real-Time**: Integrasi Ktor Client ke **Open-Meteo REST API** untuk menampilkan suhu, kecepatan angin, dan rekomendasi gear pendakian.
- [x] **Checkout QRIS & Sistem *Escrow* Otomatis**: Pembuatan tagihan QRIS Midtrans dengan visualisasi SVG QR Code, perhitungan jatuh tempo otomatis, potongan denda keterlambatan dari deposit, pengembalian sisa deposit ke Penyewa, pencairan 90% biaya sewa ke Mitra, dan pencatatan 10% komisi platform.
- [x] **Dual Interface & Dual Persistence**: Mendukung antarmuka **Mobile-First Web UI** dan **CLI Interaktif**, terhubung ke **MySQL 8.0** dengan *auto-fallback* ke **SQLite** lokal dan ekspor cadangan CSV.

## Cara Menjalankan

### 1. Menjalankan Unit Test Otomatis (10 Skenario JUnit 5)
```bash
./gradlew test
```

### 2. Menjalankan Secara Lokal (CLI + Web UI)
```bash
./gradlew run --console=plain
Buka peramban (browser) dan akses http://localhost:8080 (password bawaan seluruh akun demo: 123456).
```

### 3. Menjalankan dengan Docker Compose (Aplikasi + MySQL 8.0)
```bash
docker compose build
docker compose run --rm --service-ports campin
```

## Teknologi & Pustaka Open-Source
- Kotlin 2.0.21 (JVM 17) & Gradle Kotlin DSL
- Ktor Client & Server Netty 2.3.12 + Kotlinx Serialization JSON
- MySQL Connector/J 8.4.0 & SQLite JDBC 3.46.0.0
- Tailwind CSS (CDN) untuk antarmuka responsif mobile-first
- JUnit 5 (kotlin-test), Docker Multi-Stage, dan GitHub Actions CI

## Catatan Arsitektur & Pengembangan Lanjutan (Known Limitations)
1.  Simulasi Pembayaran QRIS & Webhook: Saat berjalan tanpa MIDTRANS_SERVER_KEY produksi, sistem menghasilkan tautan transaksi Sandbox terstruktur beserta visualisasi SVG QR Code agar pengujian lokal maupun di dalam container Docker berjalan mandiri.
2. Manajemen Sesi Web: Menggunakan HTTP Cookie Session ringan (CAMPIN_USER_ID) untuk memisahkan hak akses antar-peran beserta 1-Click Demo Switcher untuk mempercepat evaluasi demonstrasi.
3. Skema Penyewaan Per Unit Fisik: Status ketersediaan alat dikelola secara real-time (tersedia: Boolean) dengan mekanisme thread-safe (@Synchronized dan ConcurrentHashMap) untuk mencegah double booking.

# Lisensi
MIT License
