package com.example.myapplication.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.math.cos
import kotlin.math.sin

private const val WEB_CLIENT_ID = "279770745587-2p1cos7h3olim3nrkrksn4b7hs8e8am9.apps.googleusercontent.com"

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = Firebase.auth
    var isLoading by remember { mutableStateOf(false) }

    // Track touch position for the glowing interactive aura
    var touchOffset by remember { mutableStateOf(Offset.Zero) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { signInTask ->
                    if (signInTask.isSuccessful) {
                        onLoginSuccess()
                    } else {
                        Toast.makeText(context, "Sign-In failed: ${signInTask.exception?.message}", Toast.LENGTH_LONG).show()
                        isLoading = false
                    }
                }
            } else {
                isLoading = false
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Sign-In cancelled or failed", Toast.LENGTH_SHORT).show()
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Passively observe touch events to create a trailing glow without blocking clicks
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val firstDown = event.changes.firstOrNull { it.pressed }
                        if (firstDown != null) {
                            touchOffset = firstDown.position
                        } else {
                            touchOffset = Offset.Zero
                        }
                    }
                }
            }
    ) {
        // --- LUMO AMBIENT GLOW BACKGROUND ---
        LumoGlowBackground(targetOffset = touchOffset)

        // --- MAIN CONTENT LAYOUT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_lumo_logo),
                contentDescription = "Lumo App Logo",
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Professional Typography
            Text(
                text = "Lumo",
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF8A5DCF),
                letterSpacing = (-1).sp
            )

            Text(
                text = "SPARK. INSIGHT. CONVERSATION.",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B45B3).copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Google Sign-In Button
            AnimatedVisibility(visible = !isLoading, enter = fadeIn(), exit = fadeOut()) {
                Button(
                    onClick = {
                        isLoading = true
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(WEB_CLIENT_ID)
                            .requestEmail()
                            .build()
                        launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(58.dp)
                        .shadow(15.dp, RoundedCornerShape(29.dp), spotColor = Color(0x338A5DCF))
                        .clip(RoundedCornerShape(29.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .border(1.5.dp, Color.White, RoundedCornerShape(29.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Continue with Google",
                            color = Color(0xFF2D2D2D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF8A5DCF), strokeWidth = 3.dp)
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- VISIBLE FOOTER ---
            Text(
                text = "PREMIUM AI ASSISTANT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp),
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun LumoGlowBackground(targetOffset: Offset) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")

    // Animations for drifting ambient light orbs
    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart), label = "anim1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart), label = "anim2"
    )

    // Smooth physics for the touch interaction glow
    val touchAnimatedX by animateFloatAsState(
        targetValue = targetOffset.x,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "touch_x"
    )
    val touchAnimatedY by animateFloatAsState(
        targetValue = targetOffset.y,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "touch_y"
    )

    // Fade in/out the touch glow smoothly
    val touchAlpha by animateFloatAsState(
        targetValue = if (targetOffset == Offset.Zero) 0f else 0.7f,
        animationSpec = tween(500), label = "touch_alpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFDFBFF))) {

        // Canvas with extreme blur to create realistic light diffusion (The "Lumo" effect)
        Canvas(modifier = Modifier.fillMaxSize().blur(90.dp)) {
            val width = size.width
            val height = size.height

            // --- Ambient Orb 1 (Soft Top Light) ---
            val cx1 = width * (0.5f + 0.3f * cos(anim1.toDouble()).toFloat())
            val cy1 = height * (0.3f + 0.2f * sin(anim1.toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF8A5DCF).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(cx1, cy1)
                ),
                radius = width * 0.85f,
                center = Offset(cx1, cy1)
            )

            // --- Ambient Orb 2 (Deep Bottom Light) ---
            val cx2 = width * (0.5f + 0.4f * sin(anim2.toDouble()).toFloat())
            val cy2 = height * (0.7f + 0.2f * cos(anim2.toDouble()).toFloat())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6B45B3).copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx2, cy2)
                ),
                radius = width * 0.9f,
                center = Offset(cx2, cy2)
            )

            // --- Interactive Touch Orb (Intense Deep Purple Glow) ---
            if (touchAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        // Dark deep purple requested by the user
                        colors = listOf(Color(0xFF7208B1).copy(alpha = touchAlpha), Color.Transparent),
                        center = Offset(touchAnimatedX, touchAnimatedY)
                    ),
                    radius = width * 0.65f, // Spreads out widely
                    center = Offset(touchAnimatedX, touchAnimatedY)
                )
            }
        }
    }
}