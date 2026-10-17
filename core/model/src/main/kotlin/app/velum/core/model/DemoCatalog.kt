package app.velum.core.model

data class StreamSource(
    val id: String,
    val label: String,
    val url: String,
    val bytes: Long,
)

data class MediaTitle(
    val id: String,
    val name: String,
    val year: Int,
    val kindLabel: String,
    val synopsis: String,
    val license: String,
    val credit: String,
    val streams: List<StreamSource>,
    val featured: Boolean = false,
)

object DemoCatalog {
    val titles: List<MediaTitle> = listOf(
        MediaTitle(
            id = "sintel-trailer",
            name = "Sintel",
            year = 2010,
            kindLabel = "Trailer",
            synopsis = "Trailer oficial do curta Sintel, da Blender Foundation. Uma jovem atravessa uma paisagem gelada atrás de um dragão.",
            license = "Creative Commons Attribution 3.0",
            credit = "Blender Foundation",
            featured = true,
            streams = listOf(
                StreamSource(
                    id = "sintel-720",
                    label = "720p",
                    url = "https://download.blender.org/durian/trailer/sintel_trailer-720p.mp4",
                    bytes = 7_608_204L,
                ),
                StreamSource(
                    id = "sintel-480",
                    label = "480p",
                    url = "https://download.blender.org/durian/trailer/sintel_trailer-480p.mp4",
                    bytes = 4_372_373L,
                ),
            ),
        ),
        MediaTitle(
            id = "big-buck-bunny-trailer",
            name = "Big Buck Bunny",
            year = 2008,
            kindLabel = "Trailer",
            synopsis = "Trailer oficial do curta Big Buck Bunny, da Blender Foundation. Um coelho enorme reage a três roedores que perturbam o dia dele.",
            license = "Creative Commons Attribution 3.0",
            credit = "Blender Foundation",
            streams = listOf(
                StreamSource(
                    id = "bbb-m4v",
                    label = "M4V",
                    url = "https://download.blender.org/peach/trailer/trailer_iphone.m4v",
                    bytes = 3_889_885L,
                ),
                StreamSource(
                    id = "bbb-480",
                    label = "480p",
                    url = "https://download.blender.org/peach/trailer/trailer_480p.mov",
                    bytes = 11_061_011L,
                ),
            ),
        ),
        MediaTitle(
            id = "tears-of-steel",
            name = "Tears of Steel",
            year = 2012,
            kindLabel = "Longa-metragem",
            synopsis = "Filme da Blender Foundation. Num futuro em que máquinas e pessoas se enfrentam, um grupo tenta conter o conflito. O arquivo oficial é grande.",
            license = "Creative Commons Attribution 3.0",
            credit = "Blender Foundation",
            streams = listOf(
                StreamSource(
                    id = "tos-720",
                    label = "720p",
                    url = "https://download.blender.org/demo/movies/ToS/tears_of_steel_720p.mov",
                    bytes = 372_178_639L,
                ),
            ),
        ),
    )

    val featured: MediaTitle = titles.first { it.featured }

    fun find(id: String): MediaTitle? = titles.find { it.id == id }

    init {
        require(titles.isNotEmpty())
        require(titles.count { it.featured } == 1)
        val titleIds = titles.map { it.id }
        require(titleIds.distinct().size == titleIds.size)
        titles.forEach { title ->
            requireTitleId(title.id)
            require(title.name.isNotBlank())
            require(title.synopsis.isNotBlank())
            require(title.streams.isNotEmpty())
            title.streams.forEach { stream ->
                requireTitleId(stream.id)
                require(stream.label.isNotBlank())
                require(stream.bytes > 0L)
                validateStreamUrl(stream.url)
            }
        }
        val streamIds = titles.flatMap { title -> title.streams.map { it.id } }
        require(streamIds.distinct().size == streamIds.size)
    }
}
