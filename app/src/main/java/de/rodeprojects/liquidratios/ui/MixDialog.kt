package de.rodeprojects.liquidratios.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.rodeprojects.liquidratios.BuildConfig
import de.rodeprojects.liquidratios.R
import de.rodeprojects.liquidratios.domain.EditorAction
import de.rodeprojects.liquidratios.domain.InputError
import de.rodeprojects.liquidratios.domain.InputField
import de.rodeprojects.liquidratios.domain.Mix
import de.rodeprojects.liquidratios.domain.Prompt

@Composable
internal fun AboutDialog(close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        title = {
            Column {
                Text(stringResource(R.string.about_title))
                HorizontalDivider(Modifier.padding(top = 12.dp), color = Divider)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
                Text(stringResource(R.string.about_developer))
            }
        },
        confirmButton = { TextButton(close) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
internal fun MixDialog(prompt: Prompt, mix: Mix, dispatch: (EditorAction) -> Unit) {
    val cancel = { dispatch(EditorAction.Cancel) }
    val confirm = { dispatch(EditorAction.Confirm) }
    when (prompt) {
        is Prompt.Edit -> key(prompt.field) {
            NumberDialog(prompt, mix, { dispatch(EditorAction.Type(it)) }, confirm, cancel)
        }
        is Prompt.Overflow -> AlertDialog(
            onDismissRequest = cancel,
            title = { Text(stringResource(R.string.overflow_title)) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(stringResource(R.string.overflow_total, amountText(prompt.total, mix.unit)))
                    Text(stringResource(R.string.overflow_maximum, amountText(mix.maximum, mix.unit)))
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton({ dispatch(EditorAction.CorrectInput) }, Modifier.testTag("correct-input")) {
                        Text(stringResource(R.string.correct_input))
                    }
                    TextButton({ dispatch(EditorAction.RaiseLimit) }, Modifier.testTag("raise-limit")) {
                        Text(stringResource(R.string.raise_maximum))
                    }
                    CancelButton(cancel)
                }
            },
        )
        is Prompt.SwitchUnit -> {
            val name = unitName(prompt.unit)
            ConfirmDialog(
                stringResource(R.string.switch_unit, name),
                stringResource(R.string.switch_unit_body, name),
                stringResource(R.string.switch_confirm), confirm, cancel,
            )
        }
        Prompt.MenuReset -> ConfirmDialog(
            stringResource(R.string.reset),
            stringResource(R.string.reset_body),
            stringResource(R.string.reset_confirm), confirm, cancel,
        )
    }
}

@Composable
private fun NumberDialog(
    prompt: Prompt.Edit,
    mix: Mix,
    onChange: (String) -> Unit,
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    val label = when (val field = prompt.field) {
        is InputField.Ratio -> stringResource(R.string.ratio_field, letter(field.index))
        is InputField.Component -> stringResource(R.string.component_amount, letter(field.index))
        InputField.Total -> stringResource(R.string.total_amount)
        InputField.Maximum -> stringResource(R.string.maximum)
    }
    val required = prompt.pending?.total ?: mix.amounts.total
    val error = when (prompt.error) {
        InputError.AMOUNT -> stringResource(R.string.error_amount)
        InputError.POSITIVE_AMOUNT -> stringResource(R.string.error_positive_amount)
        InputError.RATIO -> stringResource(R.string.error_ratio)
        InputError.BELOW_TOTAL -> stringResource(R.string.error_below_total, amountText(required, mix.unit))
        null -> null
    }
    val requester = remember { FocusRequester() }
    var selection by remember { mutableStateOf(TextRange(0, prompt.draft.length)) }
    var composition by remember { mutableStateOf<TextRange?>(null) }
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(label) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (prompt.pending != null) {
                    Text(stringResource(R.string.minimum_maximum, amountText(required, mix.unit)))
                }
                OutlinedTextField(
                    value = TextFieldValue(prompt.draft, selection, composition),
                    onValueChange = { value ->
                        selection = value.selection
                        composition = value.composition
                        if (value.text != prompt.draft) onChange(value.text)
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(requester).testTag("number-input"),
                    suffix = if (prompt.field is InputField.Ratio) null else {
                        { Text(mix.unit.symbol) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                )
            }
        },
        confirmButton = {
            TextButton(confirm, Modifier.testTag("confirm")) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = { CancelButton(cancel) },
    )
    LaunchedEffect(Unit) { requester.requestFocus() }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    button: String,
    confirm: () -> Unit,
    cancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(title) },
        text = { Text(body, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(confirm, Modifier.testTag("confirm")) { Text(button) } },
        dismissButton = { CancelButton(cancel) },
    )
}

@Composable
private fun CancelButton(cancel: () -> Unit) {
    TextButton(cancel, Modifier.testTag("cancel")) { Text(stringResource(R.string.cancel)) }
}
