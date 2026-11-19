import api.MidtransPaymentApi
import data.MySqlCampInRepository
import model.*
import service.CampInService
import ui.CliApp
import ui.MobileWebUiServer

fun main() {
    val repository = MySqlCampInRepository()
    val paymentGateway = MidtransPaymentApi()
    val campInService = CampInService(repository, paymentGateway)

    seedInitialDataIfEmpty(campInService)

    val port = MobileWebUiServer.start(campInService, repository.modeDatabaseAktif, port = 8088)
    val webUrl = "http://localhost:$port"

    Runtime.getRuntime().addShutdownHook(Thread {
        MobileWebUiServer.stop()
    })

    try {
        CliApp(campInService, repository.modeDatabaseAktif, webUrl).jalankan()
    } finally {
        MobileWebUiServer.stop()
    }
}

private fun seedInitialDataIfEmpty(service: CampInService) {
    if (service.semuaPengguna().isNotEmpty()) return

    service.registrasiPengguna(AdminPlatform("ADM-01", "Huwaiza Rafi", "rafi@campin.id"), "123456")
    service.registrasiPengguna(Penyedia("MTR-01", "Dago Outdoor Gear", "dago@campin.id", "Dago Basecamp Rental", terverifikasiAwal = true), "123456")
    service.registrasiPengguna(Penyedia("MTR-02", "Cikuray Adventure", "garut@campin.id", "Garut Summit Gear", terverifikasiAwal = false), "123456")
    service.registrasiPengguna(Penyewa("USR-01", "Zaky Pendaki", "zaky@student.id", "08122334455"), "123456")
    service.registrasiPengguna(Penyewa("USR-02", "Nasywan Explorer", "nasywan@student.id", "08199887766"), "123456")

    service.tambahAlat(Tenda("ALT-01", "Tenda Naturehike Cloud Up 4P", 70000, "MTR-01", 4, "Double Layer", stokAwal = 3))
    service.tambahAlat(TasCarrier("ALT-02", "Carrier Deuter Aircontact 65L", 60000, "MTR-01", 65, true, stokAwal = 5))
    service.tambahAlat(PerlengkapanTidur("ALT-03", "Sleeping Bag Eiger Mummy Polar", 30000, "MTR-01", 5, "Polar Bulu Angsa", stokAwal = 8))
}
