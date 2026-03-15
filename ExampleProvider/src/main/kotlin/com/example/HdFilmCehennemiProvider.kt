package com.example

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HdFilmCehennemiProvider : MainAPI() {
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var name = "HDFilmCehennemi"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "tr"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val document = app.get(mainUrl).document
        val sections = listOf(
            "Güncel Filmler" to "div.poster-container div.poster-pop",
            "Popüler Filmler" to "div.most-popular div.poster-pop"
        )
        
        val home = sections.map { (title, selector) ->
            val items = document.select(selector).mapNotNull { it.toSearchResult() }
            HomePageList(title, items)
        }
        return newHomePageResponse(request.name, home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search/$query/"
        val document = app.get(url).document
        
        return document.select("div.poster-pop").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = document.selectFirst("div.poster img")?.attr("src")
        val description = document.selectFirst("div.film-ozeti p")?.text() ?: document.selectFirst("div.storyline")?.text()
        val year = document.selectFirst("div.film-bilgileri span:contains(Yıl) + a")?.text()?.toIntOrNull()
        val rating = document.selectFirst("span.imdb-puan")?.text()?.replace(",", ".")?.toDoubleOrNull()

        val isMovie = !url.contains("/dizi-izle/")

        return if (isMovie) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.rating = rating?.toInt()
            }
        } else {
            val episodes = document.select("div.bolum-listesi a").map {
                newEpisode(it.attr("href")) {
                    this.name = it.text().trim()
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.rating = rating?.toInt()
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

        document.select("nav.video-nav a").forEach {
            val playerUrl = it.attr("data-video")
            if (playerUrl.isNotEmpty()) {
                loadExtractor(playerUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, span.poster-title")?.text() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.attr("data-src") ?: this.selectFirst("img")?.attr("src")

        val type = if (href.contains("/dizi-izle/")) TvType.TvSeries else TvType.Movie

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
