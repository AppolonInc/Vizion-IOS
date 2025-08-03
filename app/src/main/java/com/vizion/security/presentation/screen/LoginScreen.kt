@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.vizion.security.presentation.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vizion.security.presentation.component.ModernCard
import com.vizion.security.presentation.theme.VizionColors
import com.vizion.security.presentation.viewmodel.LoginViewModel

/**
 * Écran de connexion de l'application Vizion Security
 * 
 * Cet écran permet aux utilisateurs de :
 * - Se connecter avec leurs identifiants du site web
 * - Accéder à un lien vers la création de compte
 * - Voir le statut de connexion en temps réel
 * - Bénéficier d'une interface moderne et sécurisée
 * 
 * Fonctionnalités :
 * - Validation en temps réel des champs
 * - Gestion des erreurs avec messages explicites
 * - Animation de chargement pendant l'authentification
 * - Redirection automatique après connexion réussie
 * - Lien vers le site web pour création de compte
 * 
 * Sécurité :
 * - Masquage du mot de passe avec option d'affichage
 * - Validation côté client avant envoi
 * - Gestion sécurisée des tokens d'authentification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    // États locaux pour l'interface utilisateur
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isFormValid by remember { mutableStateOf(false) }
    
    // États du ViewModel observés par l'UI
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Utilitaires pour la gestion du focus et du clavier
    val keyboardController = LocalSoftwareKeyboardController.current
    val passwordFocusRequester = remember { FocusRequester() }
    
    // Animation pour l'opacité du formulaire pendant le chargement
    val formAlpha by animateFloatAsState(
        targetValue = if (isLoading) 0.6f else 1f,
        animationSpec = tween(300),
        label = "form_alpha"
    )
    
    // Validation du formulaire en temps réel
    LaunchedEffect(email, password) {
        isFormValid = email.isNotBlank() && 
                     android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                     password.length >= 6
    }
    
    // Gestion du succès de connexion
    LaunchedEffect(uiState) {
        if (uiState is LoginViewModel.LoginUiState.Success) {
            onLoginSuccess()
        }
    }
    
    // Interface utilisateur principale
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        VizionColors.PrimaryBlue.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .alpha(formAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // === EN-TÊTE AVEC LOGO ET TITRE ===
            LoginHeader()
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // === FORMULAIRE DE CONNEXION ===
            ModernCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    // Titre du formulaire
                    Text(
                        text = "Connexion",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = "Connectez-vous avec votre compte Vizion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                    )
                    
                    // === CHAMP EMAIL ===
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trim() },
                        label = { Text("Adresse email") },
                        placeholder = { Text("votre@email.com") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email",
                                tint = VizionColors.PrimaryOrange
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { passwordFocusRequester.requestFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VizionColors.PrimaryBlue,
                            focusedLabelColor = VizionColors.PrimaryBlue
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // === CHAMP MOT DE PASSE ===
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        placeholder = { Text("Votre mot de passe") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Mot de passe",
                                tint = VizionColors.PrimaryBlue
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (isFormValid && !isLoading) {
                                    viewModel.login(email, password)
                                }
                            }
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VizionColors.PrimaryBlue,
                            focusedLabelColor = VizionColors.PrimaryBlue
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // === MESSAGE D'ERREUR ===
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = VizionColors.AccentRed.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = errorMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = VizionColors.AccentRed,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // === BOUTON DE CONNEXION ===
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.login(email, password)
                        },
                        enabled = isFormValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VizionColors.PrimaryBlue,
                            disabledContainerColor = VizionColors.PrimaryBlue.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Connexion...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = "Se connecter",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // === LIEN VERS CRÉATION DE COMPTE ===
            CreateAccountSection()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // === INFORMATIONS DE SÉCURITÉ ===
            SecurityInfoSection()
        }
    }
}

/**
 * En-tête de l'écran de connexion avec logo et titre
 */
@Composable
private fun LoginHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo officiel VizionSecurity
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Utilisation d'une icône temporaire en attendant le logo
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Logo VizionSecurity",
                modifier = Modifier.size(80.dp),
                tint = VizionColors.PrimaryOrange
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Vizion Security",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = VizionColors.PrimaryOrange // Couleur signature du logo
        )
        
        Text(
            text = "Sécurité Mobile Professionnelle",
            style = MaterialTheme.typography.bodyLarge,
            color = VizionColors.PrimaryBlack.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Section pour la création de compte
 */
@Composable
private fun CreateAccountSection() {
    ModernCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pas encore de compte ?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = {
                    // TODO: Ouvrir le navigateur vers la page de création de compte
                    // val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://appollon-inc.com/accounts/register/"))
                    // context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VizionColors.PrimaryOrange
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            VizionColors.PrimaryOrange,
                            VizionColors.PrimaryOrangeLight
                        )
                    )
                )
            ) {
                Text(
                    text = "Créer un compte sur le site web",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Créez votre compte sur appollon-inc.com\npuis connectez-vous ici",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Section d'informations de sécurité
 */
@Composable
private fun SecurityInfoSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Sécurisé",
            tint = VizionColors.AccentGreen,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = "Connexion sécurisée avec chiffrement AES-256",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}