package app.halftone.desktop.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.halftone.desktop.data.userMessage
import app.halftone.desktop.i18n.S
import app.halftone.desktop.ui.common.HalftoneField
import app.halftone.desktop.ui.common.HalftoneMark
import app.halftone.desktop.ui.common.LocalApp
import app.halftone.desktop.ui.theme.Ink
import app.halftone.desktop.ui.theme.Ink2
import app.halftone.desktop.ui.theme.MistDim
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LoginScreen() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var server by remember { mutableStateOf(app.settings.state.value.server.orEmpty()) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (busy) return
        if (server.isBlank() || username.isBlank() || password.isEmpty()) {
            error = S.loginFillAll
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                app.logIn(server, username, password)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                error = e.userMessage()
            } finally {
                busy = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Ink)) {
        HalftoneField(Modifier.fillMaxSize(), step = 26.dp, maxRadius = 8.dp, alpha = 0.5f)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Ink, 0.45f to Ink.copy(alpha = 0.92f), 1f to Ink.copy(alpha = 0.2f)),
            ),
        )

        Column(
            Modifier.align(Alignment.CenterStart).padding(start = 96.dp).width(400.dp),
        ) {
            HalftoneMark(size = 72.dp, tile = true)
            Spacer(Modifier.height(20.dp))
            Text("halftone", style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp, letterSpacing = (-2.6).sp))
            Spacer(Modifier.height(6.dp))
            Text(S.tagline, style = MaterialTheme.typography.titleMedium, color = MistDim)
            Spacer(Modifier.height(36.dp))

            Field(server, { server = it }, S.serverAddress, Icons.Rounded.Dns, placeholder = S.serverPlaceholder, keyboardType = KeyboardType.Uri)
            Spacer(Modifier.height(12.dp))
            Field(username, { username = it }, S.username, Icons.Rounded.Person)
            Spacer(Modifier.height(12.dp))
            Field(
                password, { password = it }, S.password, Icons.Rounded.Lock,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onDone = ::submit,
                visual = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            if (showPassword) S.hidePassword else S.showPassword,
                        )
                    }
                },
            )

            AnimatedVisibility(error != null) {
                Row(
                    Modifier.padding(top = 14.dp).fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(10.dp))
                    Text(error.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = ::submit,
                enabled = !busy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(S.connect, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(S.worksWith, style = MaterialTheme.typography.bodySmall, color = MistDim)
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    val app = LocalApp.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        leadingIcon = { Icon(icon, null) },
        trailingIcon = trailing,
        singleLine = true,
        visualTransformation = visual,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Ink2.copy(alpha = 0.8f),
            unfocusedContainerColor = Ink2.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = Modifier.fillMaxWidth().onFocusChanged { app.typing = it.isFocused },
    )
}
