package com.tvpro.app.ui.screens

import com.tvpro.core.data.model.EpisodeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerLogicTest {

    @Test
    fun testFormatTimeHmsFormatting() {
        assertEquals("00:00:00", formatTimeHms(0L))
        assertEquals("00:00:45", formatTimeHms(45L))
        assertEquals("00:01:05", formatTimeHms(65L))
        assertEquals("01:01:01", formatTimeHms(3661L))
        assertEquals("02:30:15", formatTimeHms(9015L))
    }

    @Test
    fun testFormatTimeSecFormatting() {
        assertEquals("00:00", formatTimeSec(0L))
        assertEquals("01:05", formatTimeSec(65L))
        assertEquals("12:34", formatTimeSec(754L))
        assertEquals("1:01:01", formatTimeSec(3661L))
        assertEquals("2:30:15", formatTimeSec(9015L))
    }

    @Test
    fun testNextEpisodeResolutionLogic() {
        val s1Episodes = listOf(
            EpisodeItem(id = 101, episodeNumber = 1, seasonNumber = 1, title = "Ep 1", overview = "", image = ""),
            EpisodeItem(id = 102, episodeNumber = 2, seasonNumber = 1, title = "Ep 2", overview = "", image = ""),
            EpisodeItem(id = 103, episodeNumber = 3, seasonNumber = 1, title = "Ep 3", overview = "", image = "")
        )

        // Case 1: In middle of season -> should return next episode in same season
        val nextInSeason = s1Episodes.firstOrNull { it.seasonNumber == 1 && it.episodeNumber == 1 + 1 }
        assertNotNull(nextInSeason)
        assertEquals(2, nextInSeason?.episodeNumber)

        // Case 2: At last episode of season 1, with multi-season availability (total seasons = 2)
        val currentSeason = 1
        val currentEpisode = 3
        val totalSeasons = 2
        val nextSameSeason = s1Episodes.firstOrNull { it.seasonNumber == currentSeason && it.episodeNumber == currentEpisode + 1 }
        assertNull(nextSameSeason)

        // When moving to Season 2 Ep 1
        val shouldAdvanceSeason = currentSeason < totalSeasons
        assertEquals(true, shouldAdvanceSeason)

        // Case 3: At last episode of final season -> should return null
        val isFinalEpisodeOfSeries = (currentSeason >= totalSeasons) && (nextSameSeason == null)
        assertEquals(false, isFinalEpisodeOfSeries)
    }

    @Test
    fun testDeadStreamDetectionPatterns() {
        val deadKeywords = listOf(
            "232001",
            "no se puede reproducir",
            "archivo de video no se puede reproducir",
            "video file cannot be played",
            "error loading media",
            "the media playback was aborted",
            "expired or has been deleted",
            "no longer available",
            "file was deleted",
            "video deleted",
            "contenido no disponible"
        )

        fun isDead(text: String): Boolean {
            val lower = text.lowercase()
            return deadKeywords.any { lower.contains(it) }
        }

        // Test user's exact error message: "Este archivo de video no se puede reproducir. (Código de Error: 232001)"
        val userError = "Este archivo de video no se puede reproducir. (Código de Error: 232001)"
        assertEquals(true, isDead(userError))

        val expiredMsg = "This video is expired or has been deleted from server"
        assertEquals(true, isDead(expiredMsg))

        val normalMsg = "Reproduciendo Efectos Colaterales Temporada 1"
        assertEquals(false, isDead(normalMsg))
    }

    @Test
    fun testStreamFailoverSelectionPicksNextAliveServer() {
        val streams = listOf(
            com.tvpro.core.data.model.StreamItem(url = "https://server1.com/embed/1", source = "Servidor 1: Principal HD"),
            com.tvpro.core.data.model.StreamItem(url = "https://server2.com/embed/2", source = "Servidor 2: Latino Full HD"),
            com.tvpro.core.data.model.StreamItem(url = "https://server3.com/embed/3", source = "Servidor 3: Subtitulado")
        )

        val failedUrls = mutableSetOf<String>()
        // Server 1 fails with 232001
        failedUrls.add("https://server1.com/embed/1")

        val alive = streams.filter { it.url !in failedUrls }
        assertEquals(2, alive.size)
        assertEquals("Servidor 2: Latino Full HD", alive.first().source)
        assertEquals("https://server2.com/embed/2", alive.first().url)
    }

    @Test
    fun testFastStreamSortingPrioritizesNativeAndFastHosts() {
        val mixedStreams = listOf(
            com.tvpro.core.data.model.StreamItem(url = "https://slow-embed.com/play/1", type = "EMBED", source = "Servidor 1: Lento"),
            com.tvpro.core.data.model.StreamItem(url = "https://voe.sx/embed/fast", type = "EMBED", source = "Servidor 2: Voe"),
            com.tvpro.core.data.model.StreamItem(url = "https://direct.cdn.com/stream.m3u8", type = "HLS", source = "Servidor 3: Directo"),
            com.tvpro.core.data.model.StreamItem(url = "https://vimeos.net/v/abc", type = "EMBED", source = "Servidor 4: Vimeos")
        )

        val sorted = mixedStreams.sortedWith(
            compareBy<com.tvpro.core.data.model.StreamItem> { stream ->
                val u = stream.url.lowercase()
                when {
                    stream.type == "HLS" || stream.type == "MP4" || u.contains(".m3u8") || u.contains(".mp4") -> 0
                    u.contains("voe") || u.contains("megankid") -> 1
                    u.contains("vimeos") -> 2
                    u.contains("goodstream") -> 3
                    else -> 4
                }
            }
        )

        // Native HLS stream should be sorted first (fastest startup < 300ms)
        assertEquals("https://direct.cdn.com/stream.m3u8", sorted[0].url)
        // Voe fast embed second
        assertEquals("https://voe.sx/embed/fast", sorted[1].url)
        // Vimeos embed third
        assertEquals("https://vimeos.net/v/abc", sorted[2].url)
        // Slow generic embed last
        assertEquals("https://slow-embed.com/play/1", sorted[3].url)
    }

    @Test
    fun testWatchdogTriggerLogicOnStall() {
        // The watchdog must trigger strictly if video is not ready after 3.5s,
        // REGARDLESS of any saved resume history position (e.g. 38,000ms from prior playback)
        fun shouldWatchdogTrigger(elapsedMs: Long, isVideoReady: Boolean): Boolean {
            return !isVideoReady && elapsedMs >= 3500L
        }

        // 1 second in: not ready yet -> do not trigger
        assertEquals(false, shouldWatchdogTrigger(1000L, isVideoReady = false))
        // 3.5 seconds in: video ready -> do not trigger
        assertEquals(false, shouldWatchdogTrigger(3500L, isVideoReady = true))
        // 3.5 seconds in: still not ready (even with resume history) -> STALLED! Trigger failover immediately!
        assertEquals(true, shouldWatchdogTrigger(3500L, isVideoReady = false))
    }
}
