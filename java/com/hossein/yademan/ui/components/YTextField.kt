// مسیر: app/src/main/java/com/hossein/yademan/ui/components/YTextField.kt
package com.hossein.yademan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hossein.yademan.ui.theme.YColors
import com.hossein.yademan.ui.theme.YRadius
import com.hossein.yademan.ui.theme.YText

/** اینپوت تیره (.input): پس‌زمینه card2، گوشه ۱۴، placeholder کم‌رنگ */
@Composable
fun YTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(YRadius.Sm)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = YText.Body.copy(color = YColors.TextPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        // اگر اکشن سفارشی نداریم، رفتار پیش‌فرض (مثلاً Next = رفتن به فیلد بعد) حفظ شود
        keyboardActions = if (onImeAction != null) KeyboardActions(onAny = { onImeAction() }) else KeyboardActions.Default,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(YColors.Gold2),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(YColors.Card2)
                    .border(1.dp, YColors.Line, shape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = YText.Body, color = YColors.TextMuted, maxLines = 1)
                    }
                    inner()
                }
                if (trailing != null) trailing()
            }
        }
    )
}
