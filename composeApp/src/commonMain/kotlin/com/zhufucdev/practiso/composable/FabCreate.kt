package com.zhufucdev.practiso.composable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.create_para

@Composable
fun FabCreate(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit = { Text(stringResource(Res.string.create_para)) },
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
        },
        text = text,
        modifier = modifier
    )
}