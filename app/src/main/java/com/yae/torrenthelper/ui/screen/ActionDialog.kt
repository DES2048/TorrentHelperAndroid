package com.yae.torrenthelper.ui.screen

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


@Composable
fun TorrentActionDialog(onConfirm:(TorrentActions)->Unit, onDismiss:()->Unit) {

    var needDownloadTar by rememberSaveable { mutableStateOf(true) }
    var needUnpackTar by rememberSaveable { mutableStateOf(true) }
    var needDeleteTorrent by rememberSaveable { mutableStateOf(true)  }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(10.dp)) {
            Column {
                Text(text = "Select torrent actions",
                    modifier = Modifier.fillMaxWidth().padding(5.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
                HorizontalDivider()
                Column(modifier = Modifier.fillMaxWidth().padding(5.dp),) {
                    LabeledCheckbox(needDownloadTar, { needDownloadTar = it }) {
                        Text(text = "Download")
                    }
                    LabeledCheckbox(needUnpackTar, { needUnpackTar = it }) {
                        Text(text = "Unpack")
                    }
                    LabeledCheckbox(needDeleteTorrent, { needDeleteTorrent = it }) {
                        Text(text = "Delete torrent")
                    }

                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        val actions = TorrentActions(needDownloadTar, needUnpackTar, needDeleteTorrent)
                        onConfirm(actions)
                    }) {
                        Text("Ok")
                    }
                    Button(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

data class TorrentActions(val needDownload:Boolean, val needUnpack:Boolean, val needDeleteTorrent:Boolean)

@Composable
fun LabeledCheckbox(checked:Boolean, onCheckedChange: ((Boolean)->Unit)?,
                    modifier:Modifier=Modifier,
                    enabled:Boolean = true, colors: CheckboxColors =  CheckboxDefaults.colors(),
                    interactionSource: MutableInteractionSource? = null,
                    content: @Composable RowScope.()->Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange, enabled = enabled, colors = colors,
            interactionSource = interactionSource)
        Row(modifier = Modifier.padding(start = 10.dp)) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DemoLabeledCheckbox() {
    var checked by remember { mutableStateOf(true) }
    LabeledCheckbox(checked, {checked = it}) {
        Text("Download")
    }
}

@Preview(showBackground = true)
@Composable
fun DemoActionDialog() {
    TorrentActionDialog({}, {})
}