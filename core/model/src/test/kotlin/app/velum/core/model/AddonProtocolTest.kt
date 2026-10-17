package app.velum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddonProtocolTest {
    @Test
    fun acceptsManifestUrlAndStremioScheme() {
        assertEquals(
            "https://addons.example.com/manifest.json",
            addonManifestUrl("https://addons.example.com"),
        )
        assertEquals(
            "https://addons.example.com/v1/manifest.json",
            addonManifestUrl("stremio://addons.example.com/v1/manifest.json"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLocalhostAddon() {
        addonManifestUrl("https://localhost/manifest.json")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPrivateAddonHost() {
        addonManifestUrl("https://192.168.0.8/manifest.json")
    }

    @Test
    fun readsCatalogAndIgnoresTorrentStreams() {
        val manifest = parseAddonManifest(
            """
            {"id":"com.example.livre","name":"Livre","version":"1.0.0","catalogs":[{"type":"movie","id":"top","name":"Top"}]}
            """.trimIndent(),
            "https://addons.example.com/manifest.json",
        )
        assertEquals("Livre", manifest.name)
        assertEquals(
            "https://addons.example.com/catalog/movie/top.json",
            catalogRequestUrl(manifest.transportUrl, "movie", "top"),
        )
        val titles = parseAddonMetas(
            """
            {"metas":[{"id":"cc-1","type":"movie","name":"Rio","year":2011,"description":"Curta livre."}]}
            """.trimIndent(),
            manifest.transportUrl,
        )
        assertEquals(1, titles.size)
        assertEquals("Rio", titles.first().name)
        val streams = parseAddonStreams(
            """
            {"streams":[
              {"infoHash":"aabb","title":"torrent"},
              {"url":"http://cdn.example.com/a.mp4","title":"claro"},
              {"url":"https://cdn.example.com/a.mp4?token=1","title":"720p"}
            ]}
            """.trimIndent(),
        )
        assertEquals(1, streams.size)
        assertEquals("720p", streams.first().label)
        assertTrue(streams.first().url.startsWith("https://cdn.example.com/"))
    }
}
