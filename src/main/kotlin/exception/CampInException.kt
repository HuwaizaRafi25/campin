package exception

sealed class CampInException(message: String) : Exception(message)

class DataTidakDitemukanException(entitas: String, id: String) :
    CampInException("$entitas dengan ID '$id' tidak ditemukan.")

class AlatTidakTersediaException(namaAlat: String) :
    CampInException("Alat '$namaAlat' sedang disewa oleh pengguna lain.")

class DuplikasiIdException(id: String) :
    CampInException("ID '$id' sudah terdaftar di dalam sistem.")

class InputTidakValidException(pesan: String) :
    CampInException(pesan)

class AksesDitolakException(pesan: String) :
    CampInException("Akses ditolak: $pesan")

class AutentikasiGagalException(pesan: String = "ID/Email atau password salah.") :
    CampInException(pesan)

class PembayaranGagalException(pesan: String) :
    CampInException("Kegagalan pembayaran: $pesan")