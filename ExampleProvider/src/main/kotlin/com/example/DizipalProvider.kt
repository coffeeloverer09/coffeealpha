package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class DizipalProvider : MainAPI() {
    override var mainUrl = "https://dizipal1542.com"
    override var name = "Dizipal"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries, TvType.Anime)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val document = app.get(mainUrl).document
        val sections = listOf(
            "Son Eklenenler" to "div.post-item",
            "Popüler Diziler" to "div.popular-series div.post-item"
        )
        
        val home = sections.map { (title, selector) ->
            val items = document.select(selector).mapNotNull { it.toSearchResult() }
            HomePageList(title, items)
        }
        return HomePageResponse(home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"
        val document = app.get(url).document
        
        return document.select("div.post-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.entry-title")?.text()?.trim() ?: return null
        val poster = document.selectFirst("div.poster img")?.attr("src")
        val description = document.selectFirst("div.description-content")?.text()
        val year = document.selectFirst("span.year")?.text()?.toIntOrNull()
        
        val isMovie = url.contains("/film/")

        return if (isMovie) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        } else {
            val episodes = document.select("div.episode-list a").map {
                Episode(
                    data = it.attr("href"),
                    name = it.text().trim()
                )
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // Dizipal often uses multiple video players (alternatives)
        document.select("div.video-player-options a").forEach {
            val playerUrl = it.attr("data-url") // Adjust selector based on actual site structure
            if (playerUrl.isNotEmpty()) {
                loadExtractor(playerUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst(".post-title")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("src")

        val type = if (href.contains("/film/")) TvType.Movie else TvType.TvSeries

        return if (type == TvType.Movie) {
            newMovieSearchResponse(title, href, TvType.Movie) {
                this.posterUrl = posterUrl
            }
        } else {
            newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
                this.posterUrl = posterUrl
            }
        }
    }
}
