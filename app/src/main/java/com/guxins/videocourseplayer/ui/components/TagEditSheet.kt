package com.guxins.videocourseplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.guxins.videocourseplayer.data.model.VideoItem
import com.guxins.videocourseplayer.data.prefs.TagRules

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagEditSheet(
    video: VideoItem,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val draft = remember(video.key) {
        mutableStateListOf<String>().apply { addAll(video.tags) }
    }
    var input by remember(video.key) { mutableStateOf("") }
    var error by remember(video.key) { mutableStateOf<String?>(null) }

    fun addCustomTag() {
        val validation = TagRules.validationError(input)
        if (validation != null) {
            error = validation
            return
        }
        val normalized = TagRules.normalize(input)
        if (draft.contains(normalized)) {
            error = "该标签已添加"
            return
        }
        draft.add(normalized)
        input = ""
        error = null
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "设置标签",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = video.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = "已选择 ${draft.size} 个标签；一个视频可以同时拥有多个标签。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val allTags = (availableTags + video.tags + draft).distinct()
            if (allTags.isNotEmpty()) {
                Text(
                    text = "本相册已有标签",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    allTags.forEach { tag ->
                        val selected = draft.contains(tag)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) draft.remove(tag) else draft.add(tag)
                                error = null
                            },
                            label = { Text(tag) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        error = null
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("新建标签") },
                    placeholder = { Text("例如：重点") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message ->
                        { Text(message) }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addCustomTag() }),
                )
                Button(
                    onClick = { addCustomTag() },
                    enabled = input.isNotBlank(),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("添加")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = {
                        draft.clear()
                        error = null
                    },
                    enabled = draft.isNotEmpty(),
                ) {
                    Text("清空全部")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        onSave(TagRules.sanitize(draft))
                        onDismiss()
                    },
                ) {
                    Text("保存")
                }
            }
        }
    }
}
