package com.zhufucdev.practiso.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zhufucdev.practiso.platform.createPlatformSavedStateHandle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

@OptIn(SavedStateHandleSaveableApi::class)
class SharedElementTransitionPopupViewModel(state: SavedStateHandle) : ViewModel() {
    var expanded by state.saveable { mutableStateOf(false) }
        private set
    var visible by state.saveable { mutableStateOf(false) }
        private set

    var transitionStart: Rect by mutableStateOf(Rect.Zero)

    suspend fun expand() {
        visible = true
        delay(50)
        expanded = true
    }

    suspend fun collapse() {
        expanded = false
        delay(500)
        visible = false
    }

    fun hide() {
        expanded = false
    }

    class Events(
        val expand: Channel<Unit> = Channel(),
        val collapse: Channel<Unit> = Channel()
    )
    val event = Events()

    init {
        viewModelScope.launch {
            while (true) {
                select {
                    event.expand.onReceive {
                        expand()
                    }

                    event.collapse.onReceive {
                        collapse()
                    }
                }
            }
        }
    }

    companion object {
        val Factory
            get() = viewModelFactory {
                initializer {
                    SharedElementTransitionPopupViewModel(createPlatformSavedStateHandle())
                }
            }
    }
}