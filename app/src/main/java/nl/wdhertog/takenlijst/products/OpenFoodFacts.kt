package nl.wdhertog.takenlijst.products

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.wdhertog.takenlijst.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Productgegevens bij een barcode uit Open Food Facts. Alleen het barcodenummer wordt verstuurd. */
object OpenFoodFacts {
    data class Info(val name: String, val brands: String, val quantity: String, val generic: String)

    sealed interface Lookup {
        data class Found(val info: Info) : Lookup
        data object NotFound : Lookup
        data object Failed : Lookup
    }

    /** Barcodes zijn alleen cijfers (EAN-8, EAN-13, UPC). */
    fun isBarcode(code: String) = code.matches(Regex("""\d{6,14}"""))

    suspend fun lookup(barcode: String): Lookup = withContext(Dispatchers.IO) {
        if (!isBarcode(barcode)) return@withContext Lookup.NotFound
        try {
            val url = URL(
                "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                    "?fields=product_name,product_name_nl,generic_name,generic_name_nl,brands,quantity"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            // Open Food Facts vraagt apps zich met naam en versie te melden.
            conn.setRequestProperty("User-Agent", "Takenlijst/${BuildConfig.VERSION_NAME} (Android; github.com/wdhertog/takenlijst)")
            try {
                when (conn.responseCode) {
                    404 -> return@withContext Lookup.NotFound
                    !in 200..299 -> return@withContext Lookup.Failed
                }
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                val product = json.optJSONObject("product") ?: return@withContext Lookup.NotFound
                fun field(vararg keys: String) = keys.map { product.optString(it).trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
                val info = Info(
                    name = field("product_name_nl", "product_name"),
                    brands = field("brands"),
                    quantity = field("quantity"),
                    generic = field("generic_name_nl", "generic_name"),
                )
                if (info.name.isEmpty() && info.generic.isEmpty()) Lookup.NotFound else Lookup.Found(info)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Lookup.Failed
        }
    }
}
