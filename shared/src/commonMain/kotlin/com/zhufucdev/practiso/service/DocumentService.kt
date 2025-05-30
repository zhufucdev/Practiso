package com.zhufucdev.practiso.service

import com.zhufucdev.practiso.database.ImageFrame
import com.zhufucdev.practiso.database.OptionsFrame
import com.zhufucdev.practiso.database.TextFrame
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.FrameArchive
import com.zhufucdev.practiso.datamodel.KeyedPrioritizedFrame
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.datamodel.QuizDocument
import com.zhufucdev.practiso.datamodel.resources
import com.zhufucdev.practiso.datamodel.unarchive
import nl.adaptivity.xmlutil.serialization.XmlParsingException
import okio.IOException
import okio.buffer
import okio.gzip

object DocumentService {
    private fun createFrameFromArchive(archive: FrameArchive, id: Long): Pair<Frame, Int> =
        when (archive) {
            is FrameArchive.Text -> Frame.Text(id, TextFrame(id, archive.content)) to 1
            is FrameArchive.Image -> Frame.Image(
                id,
                ImageFrame(
                    id,
                    archive.filename,
                    archive.width,
                    archive.height,
                    archive.altText
                )
            ) to 1

            is FrameArchive.Options -> Frame.Options(
                OptionsFrame(id, archive.name),
                archive.content.mapIndexed { index, archive ->
                    KeyedPrioritizedFrame(
                        createFrameFromArchive(
                            archive.content,
                            index + id + 1
                        ).first, archive.isKey, archive.priority
                    )
                }) to archive.content.size + 1
        }

    @Throws(IOException::class, XmlParsingException::class)
    fun unarchive(namedSource: NamedSource): List<QuizDocument> {
        val pack = namedSource.source.gzip().buffer().unarchive()
        return pack.archives.quizzes.map {
            var id = 0L
            val frames = it.frames.map { archive ->
                val (frame, increment) = createFrameFromArchive(archive, id)
                id += increment
                frame
            }
            val requiredResouces = frames.resources()

            QuizDocument(
                name = it.name,
                frames = frames,
                dimensions = it.dimensions,
                resourcePool = pack.resources.filter { (key, _) -> requiredResouces.any { it.name == key } },
                creationTime = it.creationTime,
                modificationTime = it.modificationTime
            )
        }
    }
}
