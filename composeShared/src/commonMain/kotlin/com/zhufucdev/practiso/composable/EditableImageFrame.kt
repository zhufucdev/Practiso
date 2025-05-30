package com.zhufucdev.practiso.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhufucdev.practiso.composition.combineClickable
import com.zhufucdev.practiso.datamodel.Frame
import com.zhufucdev.practiso.datamodel.NamedSource
import com.zhufucdev.practiso.platform.getPlatform
import com.zhufucdev.practiso.service.ImportService
import com.zhufucdev.practiso.style.PaddingNormal
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.clear_para
import resources.new_image_frame_para
import resources.pick_an_image_for_this_frame_para
import resources.remove_para
import resources.replace_para

@Composable
fun EditableImageFrame(
    value: Frame.Image,
    onValueChange: (Frame.Image) -> Unit,
    onDelete: () -> Unit,
    cache: BitmapRepository = remember { BitmapRepository() },
    deleteImageOnRemoval: Boolean = true,
    importService: ImportService = ImportService(),
    modifier: Modifier = Modifier,
) {
    var editingAltText by remember { mutableStateOf(false) }
    var masterMenu by remember { mutableStateOf(false) }
    val platform = getPlatform()
    val imageState = rememberFileImageState()

    val coroutine = rememberCoroutineScope()
    val pickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.Image,
        title = stringResource(Res.string.pick_an_image_for_this_frame_para)
    ) { file ->
        if (file == null) {
            return@rememberFilePickerLauncher
        }

        if (value.imageFrame.filename.isNotBlank()) {
            platform.filesystem.delete(platform.resourcePath.resolve(value.imageFrame.filename))
        }

        coroutine.launch {
            val name = withContext(Dispatchers.IO) {
                importService.importImage(NamedSource.fromFile(file))
            }

            onValueChange(value.copy(imageFrame = value.imageFrame.copy(filename = name)))
        }
    }

    LaunchedEffect(imageState.image) {
        val im = imageState.image
        if (im is ImageLoaderState.Loaded) {
            // update frame model size record
            val frame = value.imageFrame
            if (frame.width.toInt() != im.bitmap.width || frame.height.toInt() != im.bitmap.height) {
                onValueChange(
                    value.copy(
                        imageFrame = frame.copy(
                            width = im.bitmap.width.toLong(),
                            height = im.bitmap.height.toLong()
                        )
                    )
                )
            }
        }
    }

    fun deleteCurrentImage() {
        if (value.imageFrame.filename.isNotBlank()) {
            platform.filesystem.delete(platform.resourcePath.resolve(value.imageFrame.filename))
        }
    }

    ImageFrameSkeleton(
        modifier = Modifier.fillMaxWidth() then modifier,
        image = {
            @Composable
            fun imageContent(modifier: Modifier = Modifier) {
                FileImage(
                    path = value.imageFrame.filename.takeIf(String::isNotBlank)
                        ?.let { platform.resourcePath.resolve(it) },
                    contentDescription = value.imageFrame.altText,
                    state = imageState,
                    cache = cache,
                    modifier = modifier
                )
            }

            if (imageState.image !is ImageLoaderState.Loaded) {
                OutlinedCard(
                    Modifier.width(150.dp)
                        .combineClickable(onSecondaryClick = { masterMenu = true })
                ) {
                    Box(Modifier.padding(PaddingNormal).fillMaxSize()) {
                        imageContent(Modifier.align(Alignment.Center).size(60.dp))
                        SmallFloatingActionButton(
                            onClick = pickerLauncher::launch,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = masterMenu,
                            onDismissRequest = { masterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.remove_para)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    if (deleteImageOnRemoval) {
                                        deleteCurrentImage()
                                    }
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            } else {
                imageContent(
                    Modifier.combineClickable(
                        onClick = { },
                        onSecondaryClick = { masterMenu = true },
                    )
                )
                DropdownMenu(
                    expanded = masterMenu,
                    onDismissRequest = { masterMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.remove_para)) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            if (deleteImageOnRemoval) {
                                deleteCurrentImage()
                            }
                            onDelete()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.replace_para)) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            pickerLauncher.launch()
                            masterMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.clear_para)) },
                        leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null) },
                        onClick = {
                            masterMenu = false
                            deleteCurrentImage()
                            onValueChange(value.copy(imageFrame = value.imageFrame.copy(filename = "")))
                        }
                    )
                }
            }
        },
        altText = {
            var buffer by remember { mutableStateOf(value.imageFrame.altText ?: "") }
            GlowingSurface(
                glow = value.imageFrame.altText.isNullOrEmpty(),
                onClick = { editingAltText = true },
                content = {
                    AnimatedContent(editingAltText) { editing ->
                        if (!editing) {
                            Text(
                                value.imageFrame.altText?.takeIf(String::isNotEmpty)
                                    ?: stringResource(Res.string.new_image_frame_para)
                            )
                        } else {
                            TextField(
                                value = buffer,
                                onValueChange = { buffer = it },
                                placeholder = { Text(stringResource(Res.string.new_image_frame_para)) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            editingAltText = false
                                            onValueChange(
                                                value.copy(
                                                    imageFrame = value.imageFrame.copy(
                                                        altText = buffer
                                                    )
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    )
}
