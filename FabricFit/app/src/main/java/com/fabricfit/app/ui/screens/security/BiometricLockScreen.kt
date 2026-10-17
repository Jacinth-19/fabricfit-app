package com.fabricfit.app.ui.screens.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.fabricfit.app.R
import com.fabricfit.app.security.BiometricManager
import com.fabricfit.app.ui.theme.HeritageGold

/**
 * Biometric lock screen shown when app lock is enabled.
 * Prompts user to authenticate with fingerprint or face.
 */
@Composable
fun BiometricLockScreen(
    onAuthenticated: () -> Unit,
    onFailed: () -> Unit = {}
) {
    val context = LocalContext.current
    val biometricManager = BiometricManager(context)
    val activity = context as? FragmentActivity
    
    LaunchedEffect(Unit) {
        // Automatically show biometric prompt when screen appears
        if (activity != null && biometricManager.isBiometricAvailable()) {
            biometricManager.authenticate(
                activity = activity,
                onSuccess = onAuthenticated,
                onError = { _, _ -> onFailed() },
                onFailed = onFailed
            )
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Fingerprint,
                contentDescription = stringResource(R.string.biometric_title),
                modifier = Modifier.size(120.dp),
                tint = HeritageGold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.biometric_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.biometric_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = {
                    if (activity != null && biometricManager.isBiometricAvailable()) {
                        biometricManager.authenticate(
                            activity = activity,
                            onSuccess = onAuthenticated,
                            onError = { _, _ -> onFailed() },
                            onFailed = onFailed
                        )
                    }
                },
                enabled = biometricManager.isBiometricAvailable()
            ) {
                Text(stringResource(R.string.biometric_title))
            }
            
            if (!biometricManager.isBiometricAvailable()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.biometric_not_available),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
