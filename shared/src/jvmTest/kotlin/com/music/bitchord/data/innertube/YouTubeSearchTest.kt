package com.music.bitchord.data.innertube

import com.music.bitchord.data.model.SearchResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Shapes trimmed from youtube.com's own WEB `search` responses. */
class YouTubeSearchTest {

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private fun video(id: String, title: String, length: String?, channel: String = "Marques Brownlee") = """
        { "videoRenderer": {
          "videoId": "$id",
          "thumbnail": { "thumbnails": [
            { "url": "https://i.ytimg.com/vi/$id/small.jpg", "width": 360, "height": 202 },
            { "url": "https://i.ytimg.com/vi/$id/hq720.jpg", "width": 720, "height": 404 }
          ] },
          "title": { "runs": [ { "text": "$title" } ] },
          "longBylineText": { "runs": [ { "text": "$channel" } ] },
          ${length?.let { "\"lengthText\": { \"simpleText\": \"$it\" }," } ?: ""}
          "viewCountText": { "simpleText": "19,275,245 views" }
        } }"""

    @Test
    fun firstPage_readsVideosAsVideoTracksAndSkipsTheRest() {
        val page = InnertubeParser.parseYouTubeSearchPage(
            json(
                """
                { "contents": { "twoColumnSearchResultsRenderer": { "primaryContents": {
                  "sectionListRenderer": { "contents": [
                    { "itemSectionRenderer": { "contents": [
                      ${video("8FpPSMIB4uA", "Reviewing EVERY iPhone Ever!", "33:15")},
                      { "shelfRenderer": { "title": { "simpleText": "Shorts" } } },
                      ${video("LIVE0000000", "Live now", null)},
                      ${video("8FpPSMIB4uA", "Reviewing EVERY iPhone Ever!", "33:15")},
                      ${video("SdLShOCvVeM", "iPhone 16 Review", "12:01")}
                    ] } },
                    { "continuationItemRenderer": { "continuationEndpoint": {
                      "continuationCommand": { "token": "NEXT" }
                    } } }
                  ] }
                } } } }
                """,
            ),
        )

        val songs = page.rows.map { (it as SearchResult.Track).song }
        assertEquals(listOf("8FpPSMIB4uA", "SdLShOCvVeM"), songs.map { it.videoId })
        val first = songs.first()
        assertEquals("Reviewing EVERY iPhone Ever!", first.title)
        assertEquals("Marques Brownlee", first.artist)
        assertEquals("33:15", first.durationText)
        assertEquals("https://i.ytimg.com/vi/8FpPSMIB4uA/hq720.jpg", first.thumbnailUrl)
        assertTrue(songs.all { it.isVideo })
        assertEquals("NEXT", page.continuation)
    }

    @Test
    fun continuation_readsAppendedItems() {
        val page = InnertubeParser.parseYouTubeSearchPage(
            json(
                """
                { "onResponseReceivedCommands": [ { "appendContinuationItemsAction": {
                  "continuationItems": [
                    { "itemSectionRenderer": { "contents": [
                      ${video("Od6M0AXpcxQ", "Old iPhone", "8:40", channel = "Someone")}
                    ] } }
                  ]
                } } ] }
                """,
            ),
        )

        assertEquals(listOf("Od6M0AXpcxQ"), page.rows.map { (it as SearchResult.Track).song.videoId })
        assertNull(page.continuation)
    }
}
