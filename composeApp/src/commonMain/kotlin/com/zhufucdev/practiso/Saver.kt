package com.zhufucdev.practiso

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> protoBufStateListSaver() = listSaver(
    save = {
        it.map { v -> ProtoBuf.encodeToByteArray(serializer<T>(), v) }
    },
    restore = {
        it.map { b -> ProtoBuf.decodeFromByteArray(serializer<T>(), b) }.toMutableStateList()
    }
)

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> protobufSaver() = Saver<T, ByteArray>(
    save = {
        ProtoBuf.encodeToByteArray(serializer<T>(), it)
    },
    restore = {
        ProtoBuf.decodeFromByteArray(serializer<T>(), it)
    }
)