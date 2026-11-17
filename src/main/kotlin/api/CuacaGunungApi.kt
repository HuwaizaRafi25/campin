package api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current_weather") val currentWeather: CurrentWeather
)

@Serializable
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double
)

data class DestinasiGunung(val nama: String, val lat: Double, val lon: Double, val mdpl: Int)

object CuacaGunungApi {
    val daftarGunung = mapOf(
        "1" to DestinasiGunung("Gunung Cikuray (Garut)", -7.3225, 107.8600, 2821),
        "2" to DestinasiGunung("Gunung Burangrang (Bandung)", -6.7744, 107.5556, 2050),
        "3" to DestinasiGunung("Gunung Ciremai (Kuningan)", -6.8920, 108.4070, 3078),
        "4" to DestinasiGunung("Gunung Rinjani (Lombok)", -8.4113, 116.4573, 3726)
    )

    suspend fun ambilCuacaDanRekomendasi(kodeGunung: String): String {
        val gunung = daftarGunung[kodeGunung] ?: daftarGunung["1"]!!
        return try {
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }.use { client ->
                val resp: OpenMeteoResponse = client.get("https://api.open-meteo.com/v1/forecast") {
                    parameter("latitude", gunung.lat)
                    parameter("longitude", gunung.lon)
                    parameter("current_weather", true)
                }.body()

                val suhu = resp.currentWeather.temperature
                val angin = resp.currentWeather.windspeed
                val saranGear = when {
                    suhu <= 14.0 || angin >= 20.0 ->
                        "Wajib sewa Tenda Double Layer, Sleeping Bag Polar Limit <= 5°C, dan Carrier ber-Raincover."
                    else ->
                        "Cuaca kondusif. Tenda Standar, Sleeping Bag Dacron, dan Carrier 45-60L sudah memadai."
                }
                """
                [LIVE API OPEN-METEO] ${gunung.nama} (${gunung.mdpl} MDPL)
                • Suhu Saat Ini    : ${suhu}°C
                • Kecepatan Angin  : ${angin} km/jam
                • Rekomendasi Gear : $saranGear
                """.trimIndent()
            }
        } catch (e: Exception) {
            "[MODE OFFLINE] ${gunung.nama} (${gunung.mdpl} MDPL) — Estimasi suhu puncak 12°C. Rekomendasi: Tenda Double Layer & SB Polar."
        }
    }

    fun cekCuacaBlocking(kodeGunung: String): String = runBlocking {
        ambilCuacaDanRekomendasi(kodeGunung)
    }
}