package app.velum.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRulesTest {
    @Test
    fun catalogUsesOnlyOfficialBlenderFiles() {
        assertEquals(3, DemoCatalog.titles.size)
        assertEquals("sintel-trailer", DemoCatalog.featured.id)
        assertEquals(
            "https://download.blender.org/durian/trailer/sintel_trailer-720p.mp4",
            DemoCatalog.featured.streams.first().url,
        )
        assertTrue(DemoCatalog.titles.all { title -> title.streams.all { it.bytes > 0L } })
    }

    @Test
    fun measuredSizesStayHonest() {
        val sintel = DemoCatalog.find("sintel-trailer")!!
        assertEquals(7_608_204L, sintel.streams.first().bytes)
        assertEquals("7,6 MB", formatByteSize(sintel.streams.first().bytes))
        assertEquals("4,4 MB", formatByteSize(sintel.streams[1].bytes))
        val bunny = DemoCatalog.find("big-buck-bunny-trailer")!!
        assertEquals("3,9 MB", formatByteSize(bunny.streams.first().bytes))
        val tears = DemoCatalog.find("tears-of-steel")!!
        assertEquals("372 MB", formatByteSize(tears.streams.first().bytes))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCleartext() {
        validateStreamUrl("http://download.blender.org/durian/trailer/sintel_trailer-720p.mp4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPrivateAddress() {
        validateStreamUrl("https://127.0.0.1/video.mp4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherHost() {
        validateStreamUrl("https://example.com/video.mp4")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCredentialsInUrl() {
        validateStreamUrl("https://user:secret@download.blender.org/durian/trailer/sintel_trailer-720p.mp4")
    }

    @Test
    fun resumeAndClock() {
        assertEquals(0L, resumePositionMs(0L, 60_000L))
        assertEquals(0L, resumePositionMs(-1L, 60_000L))
        assertEquals(12_000L, resumePositionMs(12_000L, 60_000L))
        assertEquals(0L, resumePositionMs(56_000L, 60_000L))
        assertEquals(12_000L, resumePositionMs(12_000L, null))
        assertEquals("0:05", formatClock(5_000L))
        assertEquals("1:30", formatClock(90_000L))
        assertEquals("1:02:03", formatClock(3_723_000L))
    }
}
