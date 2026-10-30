package app.halftone.ui.login

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.halftone.AppContainer
import app.halftone.data.userMessage
import app.halftone.ui.common.HalftoneField
import app.halftone.ui.common.HalftoneMark
import app.halftone.ui.common.appViewModel
import app.halftone.ui.theme.Ink
import app.halftone.ui.theme.Ink2
import app.halftone.ui.theme.MistDim
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import app.halftone.R
import app.halftone.Str

class LoginViewModel(private val container: AppContainer) : ViewModel() {
    var server by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun submit() {
        if (busy) return
        if (server.isBlank() || username.isBlank() || password.isEmpty()) {
            error = Str.get(R.string.login_fill_all)
            return
        }
        viewModelScope.launch {
            busy = true
            error = null
            try {
                container.logIn(server, username, password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = e.userMessage()
            } finally {
                busy = false
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val vm = appViewModel { LoginViewModel(it) }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink),
    ) {
        // Het rasterveld uit het logo, van rechtsboven het scherm in, vervagend naar het formulier.
        HalftoneField(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.62f)
                .background(
                    Brush.verticalGradient(
                        0f to Ink.copy(alpha = 0.15f),
                        0.55f to Ink.copy(alpha = 0.6f),
                        1f to Ink,
                    ),
                ),
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 480.dp) // op tablets niet over de hele breedte
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 28.dp),
        ) {
            Spacer(Modifier.height(96.dp))
            HalftoneMark(size = 72.dp, tile = true)
            Spacer(Modifier.height(20.dp))
            Text(
                "halftone",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, letterSpacing = (-2.8).sp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tagline),
                style = MaterialTheme.typography.titleMedium,
                color = MistDim,
            )

            Spacer(Modifier.height(48.dp))

            LoginField(
                value = vm.server,
                onValueChange = { vm.server = it },
                label = stringResource(R.string.server_address),
                placeholder = stringResource(R.string.server_placeholder),
                icon = Icons.Rounded.Dns,
                keyboardType = KeyboardType.Uri,
            )
            Spacer(Modifier.height(14.dp))
            LoginField(
                value = vm.username,
                onValueChange = { vm.username = it },
                label = stringResource(R.string.username),
                icon = Icons.Rounded.Person,
            )
            Spacer(Modifier.height(14.dp))
            LoginField(
                value = vm.password,
                onValueChange = { vm.password = it },
                label = stringResource(R.string.password),
                icon = Icons.Rounded.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onDone = vm::submit,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = stringResource(if (showPassword) R.string.hide_password else R.string.show_password),
                        )
                    }
                },
            )

            AnimatedVisibility(
                visible = vm.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), MaterialTheme.shapes.small)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text(vm.error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = vm::submit,
                enabled = !vm.busy,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                AnimatedContent(
                    targetState = vm.busy,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "busy",
                ) { busy ->
                    if (busy) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text(stringResource(R.string.connect), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.works_with),
                style = MaterialTheme.typography.bodySmall,
                color = MistDim,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = trailing,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            autoCorrectEnabled = false,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Ink2.copy(alpha = 0.7f),
            unfocusedContainerColor = Ink2.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

