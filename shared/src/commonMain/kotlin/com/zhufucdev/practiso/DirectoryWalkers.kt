package com.zhufucdev.practiso

import com.zhufucdev.practiso.platform.DirectoryWalker
import com.zhufucdev.practiso.platform.DownloadableFile
import com.zhufucdev.practiso.platform.PractisoHeaderPlugin
import com.zhufucdev.practiso.platform.createHttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json

abstract class AbstractHfWalker(
    val repoId: String,
    val revision: String = "main",
    open val path: String? = null,
) : DirectoryWalker {
    override val identifier: String
        get() = "$repoId@$revision"
    protected val httpClient = createHttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(PractisoHeaderPlugin)
    }

    protected fun getDownloadLink(path: String) =
        buildUrl {
            takeFrom(ENDPOINT_URL)
            appendPathSegments(repoId, "resolve", revision, path)
        }

    companion object {
        protected const val ENDPOINT_URL = "https://huggingface.co"
    }
}

class HfDirectoryWalker(
    repoId: String,
    revision: String = "main",
    path: String? = null,
) : AbstractHfWalker(repoId, revision, path) {
    override val files: Flow<DownloadableFile> = flow {
        val response = httpClient.get {
            url {
                takeFrom(ENDPOINT_URL)
                appendPathSegments("api", "models", repoId, "tree", revision)
                if (path != null) {
                    appendPathSegments(path)
                }
            }
            parameter("recursive", "True")
        }
        if (!response.status.isSuccess()) {
            throw IllegalArgumentException("HTTP ${response.status.value}")
        }
        val items: List<Item> = response.body()

        for (item in items) {
            if (item.type == ItemType.File) {
                emit(
                    DownloadableFile(
                        name = item.path,
                        url = getDownloadLink(item.path),
                        size = item.size.takeIf { it > 0 },
                        sha256sum = item.oid
                    )
                )
            }
        }
    }

    @Serializable
    data class Item(
        @Serializable(ItemTypeSerializer::class) val type: ItemType,
        val size: Long,
        val path: String,
        val oid: String,
    )

    enum class ItemType {
        Directory,
        File
    }
}

class HfSingleFileWalker(
    repoId: String,
    revision: String = "main",
    override val path: String
) : AbstractHfWalker(repoId, revision, path) {
    override val identifier: String
        get() = "$repoId@$revision/$path"

    override val files: Flow<DownloadableFile>
        get() = flow {
            emit(getDownloadableFile())
        }

    suspend fun getDownloadableFile(): DownloadableFile {
        val url = getDownloadLink(path)
        val response = httpClient.head {
            url(url)
            header("Accept-Encoding", "identity")
        }.headers
        return DownloadableFile(
            name = path,
            url = url,
            size = response["Content-Length"]?.toLongOrNull(),
            sha256sum = response["ETag"]
        )
    }
}

private class ItemTypeSerializer : KSerializer<HfDirectoryWalker.ItemType> {
    override val descriptor: SerialDescriptor = serialDescriptor<String>()

    override fun serialize(
        encoder: Encoder,
        value: HfDirectoryWalker.ItemType,
    ) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): HfDirectoryWalker.ItemType =
        decoder.decodeString().takeIf { it.isNotBlank() }
            ?.let { it[0].uppercaseChar() + it.slice(1 until it.length) }
            ?.let(HfDirectoryWalker.ItemType::valueOf)
            ?: error("Empty or blank value for HfDirectoryWalker.ItemType")
}

/**
 * A [DirectoryWalker] that "moves" files emitted by [inner] to root from [baseDir].
 */
class MovingDirectoryWalker(private val inner: DirectoryWalker, baseDir: String) : DirectoryWalker {
    val baseDir: String = baseDir.trim().removeSuffix("/")

    override val identifier: String
        get() = inner.identifier

    override val files: Flow<DownloadableFile> =
        inner.files.map { f -> f.copy(name = f.name.removePrefix(this.baseDir + "/")) }
}

fun DirectoryWalker.moved(baseDir: String) = MovingDirectoryWalker(this, baseDir)

class ListedDirectoryWalker(files: List<DownloadableFile>, override val identifier: String) : DirectoryWalker {
    override val files: Flow<DownloadableFile> = flow { files.forEach { emit(it) } }
}

fun directoryWalkerOf(identifier: String, vararg files: DownloadableFile): DirectoryWalker {
    return ListedDirectoryWalker(files.toList(), identifier)
}