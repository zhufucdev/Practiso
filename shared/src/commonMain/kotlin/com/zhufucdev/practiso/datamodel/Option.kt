package com.zhufucdev.practiso.datamodel

import androidx.compose.runtime.Composable
import com.zhufucdev.practiso.database.Dimension
import com.zhufucdev.practiso.database.GetAllDimensionsWithQuizCount
import com.zhufucdev.practiso.database.Quiz
import com.zhufucdev.practiso.database.QuizQueries
import com.zhufucdev.practiso.database.Session
import com.zhufucdev.practiso.database.Template
import com.zhufucdev.practiso.datamodel.PractisoOption.View
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.empty_span
import resources.n_questions_dot_created_date_para
import resources.n_questions_span
import resources.new_question_para
import resources.new_template_para
import resources.no_description_para
import kotlin.collections.map

private typealias DbQuiz = Quiz
private typealias DbDimension = Dimension
private typealias DbSessionOption = com.zhufucdev.practiso.database.SessionOptionView

sealed interface PractisoOption {
    val id: Long
    val view: View

    data class View(val title: @Composable () -> String, val preview: @Composable () -> String)
}

data class QuizOption(val quiz: DbQuiz, val preview: String?) : PractisoOption {
    override val id: Long
        get() = quiz.id

    override val view: View
        get() = View(
            title = {
                quiz.name?.takeIf(String::isNotEmpty)
                    ?: stringResource(Res.string.new_question_para)
            },
            preview = {
                preview ?: stringResource(Res.string.empty_span)
            }
        )
}

data class DimensionOption(val dimension: DbDimension, val quizCount: Int) : PractisoOption,
    SessionCreator {
    override val id: Long
        get() = dimension.id

    override val view: View
        get() = View(
            title = { dimension.name },
            preview = {
                if (quizCount > 0)
                    pluralStringResource(
                        Res.plurals.n_questions_span,
                        quizCount,
                        quizCount
                    )
                else stringResource(Res.string.empty_span)
            }
        )

    override val selection: Selection
        get() = Selection(dimensionIds = setOf(id))
    override val sessionName: String?
        get() = dimension.name
}

data class SessionOption(val session: Session, val quizCount: Int) : PractisoOption {
    override val id: Long
        get() = session.id

    override val view: View
        get() = View(
            title = { session.name },
            preview = {
                pluralStringResource(
                    Res.plurals.n_questions_dot_created_date_para,
                    quizCount,
                    quizCount,
                    HumanReadable.timeAgo(session.creationTimeISO)
                )
            }
        )
}

data class TemplateOption(val template: Template) : PractisoOption {
    override val id: Long
        get() = template.id

    override val view: View
        get() = View(
            title = {
                template.name.takeIf(String::isNotEmpty)
                    ?: stringResource(Res.string.new_template_para)
            },
            preview = { template.description ?: stringResource(Res.string.no_description_para) }
        )
}

suspend fun QuizFrames.toOption() = coroutineScope {
    QuizOption(
        quiz = quiz,
        preview = frames.map { async { it.frame.getPreviewText() } }.awaitAll()
            .joinToString("  ")
    )
}

fun Flow<List<QuizFrames>>.toQuizOptionFlow(): Flow<List<QuizOption>> =
    map { frames ->
        coroutineScope {
            frames.map {
                async { it.toOption() }
            }.awaitAll()
        }
    }

fun Flow<List<DbDimension>>.toDimensionOptionFlow(db: QuizQueries): Flow<List<DimensionOption>> =
    map { dimensions ->
        coroutineScope {
            dimensions.map {
                async {
                    DimensionOption(
                        dimension = it,
                        quizCount = (db.getQuizCountByDimension(it.id)
                            .executeAsOneOrNull() ?: 0)
                            .toInt()
                    )
                }
            }.awaitAll()
        }
    }

fun Flow<List<GetAllDimensionsWithQuizCount>>.toDimensionOptionFlow(): Flow<List<DimensionOption>> =
    map { data ->
        data.map {
            DimensionOption(
                dimension = Dimension(it.id, it.name),
                quizCount = it.quizCount.toInt()
            )
        }
    }

fun DbSessionOption.toOption() =
    SessionOption(
        session = Session(
            id = id,
            name = name,
            creationTimeISO = creationTimeISO,
            lastAccessTimeISO = lastAccessTimeISO
        ),
        quizCount = quizCount.toInt()
    )

fun Flow<List<DbSessionOption>>.toSessionOptionFlow(): Flow<List<SessionOption>> =
    map { sessions ->
        sessions.map(DbSessionOption::toOption)
    }

fun Flow<List<Template>>.toTemplateOptionFlow() = map { it.map { t -> TemplateOption(t) } }