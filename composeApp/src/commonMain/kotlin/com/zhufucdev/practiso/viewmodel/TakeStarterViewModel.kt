package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.zhufucdev.practiso.Database
import com.zhufucdev.practiso.composable.FlipCardState
import com.zhufucdev.practiso.database.AppDatabase
import com.zhufucdev.practiso.database.TakeStat
import com.zhufucdev.practiso.platform.AppDestination
import com.zhufucdev.practiso.platform.Navigation
import com.zhufucdev.practiso.platform.NavigationOption
import com.zhufucdev.practiso.platform.Navigator
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import com.zhufucdev.practiso.platform.randomUUID
import com.zhufucdev.practiso.protoBufStateListSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration

@OptIn(SavedStateHandleSaveableApi::class)
class TakeStarterViewModel(
    val db: AppDatabase,
    state: SavedStateHandle,
) : ViewModel() {
    val option = MutableStateFlow<PractisoOption.Session?>(null)
    var uiCoroutineScope: CoroutineScope? = null
        private set

    val takeStats by lazy {
        MutableStateFlow<List<TakeStat>?>(null).apply {
            viewModelScope.launch {
                option.filterNotNull().collectLatest {
                    db.sessionQueries.getTakeStatsBySessionId(it.session.id)
                        .asFlow()
                        .mapToList(Dispatchers.IO)
                        .collect(this@apply)
                }
            }
        }
    }

    val flipCardState = FlipCardState()

    @Serializable
    data class Timer(val duration: Duration, val id: String = randomUUID())

    val timers by state.saveable(saver = protoBufStateListSaver()) {
        mutableStateListOf<Timer>()
    }

    var currentTakeId by state.saveable { mutableLongStateOf(-1) }
        private set
    var showHidden by state.saveable { mutableStateOf(false) }
        private set

    data class Events(
        val create: Channel<Unit> = Channel(),
        val start: Channel<Long> = Channel(),
        val tapTake: Channel<Long> = Channel(),
        val flip: Channel<Int> = Channel(),
        val hide: Channel<Long> = Channel(),
        val unhide: Channel<Long> = Channel(),
        val toggleShowHidden: Channel<Unit> = Channel(),
        val createAndStart: Channel<Unit> = Channel(),
    )

    val event = Events()

    private fun createTake(): Long {
        val takeId = db.transactionWithResult {
            db.sessionQueries.updateSessionAccessTime(
                Clock.System.now(),
                option.value!!.session.id
            )
            db.sessionQueries.insertTake(
                sessionId = option.value!!.session.id,
                creationTimeISO = Clock.System.now(),
            )
            db.quizQueries.lastInsertRowId().executeAsOne()
        }

        db.transaction {
            timers.forEach { d ->
                db.sessionQueries.associateTimerWithTake(
                    takeId,
                    durationSeconds = d.duration.inWholeMilliseconds / 1000.0
                )
            }
        }

        timers.clear()

        return takeId
    }

    init {
        viewModelScope.launch {
            while (viewModelScope.isActive) {
                select {
                    event.create.onReceive {
                        createTake()
                    }

                    event.flip.onReceive {
                        uiCoroutineScope?.launch {
                            flipCardState.flip(it)
                        } ?: error("No ui coroutine scope specified")
                    }

                    event.start.onReceive {
                        Navigator.navigate(
                            Navigation.Goto(AppDestination.Answer),
                            options = listOf(NavigationOption.OpenTake(it))
                        )
                    }

                    event.tapTake.onReceive {
                        if (currentTakeId == it) {
                            currentTakeId = -1
                        } else {
                            currentTakeId = it
                        }
                    }

                    event.hide.onReceive {
                        db.transaction {
                            db.sessionQueries.updateTakeVisibility(
                                hidden = 1,
                                id = it
                            )
                        }
                    }

                    event.unhide.onReceive {
                        db.transaction {
                            db.sessionQueries.updateTakeVisibility(
                                hidden = 0,
                                id = it
                            )
                        }
                    }

                    event.toggleShowHidden.onReceive {
                        showHidden = !showHidden
                        Unit
                    }

                    event.createAndStart.onReceive {
                        val id = createTake()
                        currentTakeId = id
                        uiCoroutineScope?.launch {
                            flipCardState.flip(0)
                            Navigator.navigate(
                                Navigation.Goto(AppDestination.Answer),
                                options = listOf(NavigationOption.OpenTake(id))
                            )
                        } ?: error("No ui coroutine scope specified")
                    }
                }
            }
        }
    }

    suspend fun load(option: PractisoOption.Session, coroutineScope: CoroutineScope) {
        uiCoroutineScope = coroutineScope
        this.option.emit(option)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                TakeStarterViewModel(
                    Database.app,
                    createPlatformSavedStateHandle()
                )
            }
        }
    }
}