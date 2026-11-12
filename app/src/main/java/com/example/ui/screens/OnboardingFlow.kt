package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.OnboardingStep
import com.example.model.OnboardingInputValidator
import com.example.ui.components.AppIconBadge
import com.example.ui.theme.OpalBlack
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalMintLight
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import com.example.viewmodel.OpalUiState

@Composable
fun OnboardingFlow(
    uiState: OpalUiState,
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit,
    onSetName: (String) -> Unit,
    onSetGoal: (String) -> Unit,
    onSetScreenTime: (String) -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestScreenTimePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onToggleAppBlocked: (String) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OpalBlack)
            .statusBarsPadding()
    ) {
        when (uiState.onboardingStep) {
            OnboardingStep.WELCOME_ROCK -> {
                WelcomeRockStep(onNext = onNextStep)
            }
            OnboardingStep.NAME_INPUT -> {
                NameInputStep(
                    currentName = uiState.onboardingName,
                    onNameChanged = onSetName,
                    onNext = onNextStep,
                    onBack = onPrevStep
                )
            }
            OnboardingStep.AUTH_CHECK -> {
                AuthCheckStep(
                    userName = uiState.onboardingName,
                    onNext = onNextStep,
                    onBack = onPrevStep
                )
            }
            OnboardingStep.AUTH_OPTIONS -> {
                AuthOptionsStep(
                    onNext = onNextStep,
                    onBack = onPrevStep
                )
            }
            OnboardingStep.GOAL_SELECTION -> {
                GoalSelectionStep(
                    selectedGoal = uiState.onboardingGoal,
                    onSelectGoal = {
                        onSetGoal(it)
                        onNextStep()
                    },
                    onBack = onPrevStep
                )
            }
            OnboardingStep.SCREEN_TIME_ESTIMATE -> {
                ScreenTimeEstimateStep(
                    goal = uiState.onboardingGoal,
                    selectedEstimate = uiState.onboardingScreenTimeEst,
                    onSelectEstimate = {
                        onSetScreenTime(it)
                        onNextStep()
                    },
                    onBack = onPrevStep
                )
            }
            OnboardingStep.BAD_NEWS_STAT -> {
                BadNewsStatStep(onNext = onNextStep)
            }
            OnboardingStep.GOOD_NEWS_STAT -> {
                GoodNewsStatStep(onNext = onNextStep)
            }
            OnboardingStep.PERMISSIONS_INTRO -> {
                PermissionsIntroStep(
                    userName = uiState.onboardingName,
                    overlayGranted = uiState.overlayPermissionGranted,
                    screenTimeGranted = uiState.screenTimePermissionGranted,
                    onRequestOverlay = onRequestOverlayPermission,
                    onRequestScreenTime = onRequestScreenTimePermission,
                    onNext = onNextStep
                )
            }
            OnboardingStep.HOW_IT_WORKS_1 -> {
                HowItWorks1Step(onNext = onNextStep)
            }
            OnboardingStep.HOW_IT_WORKS_2 -> {
                HowItWorks2Step(onNext = onNextStep)
            }
            OnboardingStep.HOW_IT_WORKS_3 -> {
                HowItWorks3Step(onNext = onNextStep)
            }
            OnboardingStep.NOTIFICATIONS_PROMPT -> {
                NotificationsPromptStep(
                    onRequestNotification = onRequestNotificationPermission,
                    onSkip = onNextStep
                )
            }
            OnboardingStep.ACCESSIBILITY_GUIDE -> {
                AccessibilityGuideStep(
                    onRequestAccessibility = onRequestAccessibilityPermission,
                    isGranted = uiState.accessibilityPermissionGranted,
                    onNext = onNextStep
                )
            }
            OnboardingStep.SELECT_DISTRACTIONS -> {
                SelectDistractionsStep(
                    apps = uiState.apps,
                    onToggleBlock = onToggleAppBlocked,
                    onNext = onNextStep
                )
            }
            OnboardingStep.FINAL_FIST_BUMP -> {
                FinalFistBumpStep(
                    userName = uiState.onboardingName,
                    onComplete = onComplete
                )
            }
        }
    }
}

@Composable
private fun WelcomeRockStep(onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .testTag("onboarding_welcome_step"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Signature PRIME Emblem Hero
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFF000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.prime_logo),
                        contentDescription = "Prime Logo",
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(32.dp))
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Prime",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.2).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "From distraction to your ",
                        color = Color(0xFFBCBBC0),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "prime.",
                        color = Color(0xFF00E59A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF00E59A))
                        .clickable { onNext() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Get Started",
                        color = Color.Black,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "YOUR JOURNEY STARTS HERE",
                    color = Color(0xFF75757A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp
                )
            }
        }
    }
}

@Composable
private fun NameInputStep(
    currentName: String,
    onNameChanged: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var nameText by remember(currentName) { mutableStateOf(currentName) }
    val normalizedName = OnboardingInputValidator.normalizeName(nameText)
    val isNameValid = OnboardingInputValidator.isValidName(normalizedName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)

        Spacer(modifier = Modifier.height(20.dp))

        // Glowing Prime Gemstone
        Image(
            painter = painterResource(id = R.drawable.prime_logo),
            contentDescription = "Prime Gemstone",
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = "I'll help protect your focus.",
            color = OpalTextSecondary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "First, what's your name?",
            color = OpalTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = nameText,
            onValueChange = {
                val safeValue = it.filter { char -> char.isLetter() || char.isWhitespace() || char == '-' || char.code == 39 || char == '.' }.take(30)
                nameText = safeValue
                onNameChanged(safeValue)
            },
            placeholder = { Text("John", color = OpalTextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OpalMint,
                unfocusedBorderColor = OpalCardBorder,
                focusedContainerColor = OpalCardBg,
                unfocusedContainerColor = OpalCardBg,
                focusedTextColor = OpalTextPrimary,
                unfocusedTextColor = OpalTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name_input")
        )

        if (nameText.isNotBlank() && !isNameValid) {
            Text(
                text = "Enter a name between 2 and 30 characters.",
                color = Color(0xFFFF5B66),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                if (isNameValid) {
                    onNameChanged(normalizedName)
                    onNext()
                }
            },
            enabled = isNameValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E59A),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF242424),
                disabledContentColor = Color(0xFF777777)
            ),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("onboarding_name_continue_button")
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AuthCheckStep(
    userName: String,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)
        Spacer(Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.prime_logo),
            contentDescription = "PRIME logo",
            modifier = Modifier.size(140.dp)
        )
        Spacer(Modifier.height(40.dp))
        Text("Hi, $userName", color = OpalMint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(
            "A fresh start, on your terms.",
            color = OpalTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "You can use PRIME without creating an account. Your profile, tasks and routines are stored locally on this device.",
            color = OpalTextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = OpalMint, contentColor = Color.Black),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AuthOptionsStep(onNext: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)
        Spacer(Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.prime_logo),
            contentDescription = "PRIME logo",
            modifier = Modifier.size(140.dp)
        )
        Spacer(Modifier.height(40.dp))
        Text(
            "Your data stays on this device",
            color = OpalTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Account sign-in and cloud sync are not available yet. You can still use your focus tools, shields, planner and wellbeing features locally.",
            color = OpalTextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = OpalMint, contentColor = Color.Black),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text("Continue on this device", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun GoalSelectionStep(
    selectedGoal: String,
    onSelectGoal: (String) -> Unit,
    onBack: () -> Unit
) {
    val goals = listOf(
        "Focus better",
        "Sleep better",
        "Be more present",
        "Improve mental health",
        "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.prime_logo),
            contentDescription = "Prime Gemstone",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(text = "All set.", color = OpalMint, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "I'm going to ask you a few questions, no need to overthink it. Then I'll build your setup.",
            color = OpalTextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "What matters most to you?",
            color = OpalTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Options pills in centered flow
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalPill(goals[0], selectedGoal == goals[0]) { onSelectGoal(goals[0]) }
                GoalPill(goals[1], selectedGoal == goals[1]) { onSelectGoal(goals[1]) }
            }
            GoalPill(goals[2], selectedGoal == goals[2]) { onSelectGoal(goals[2]) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalPill(goals[3], selectedGoal == goals[3]) { onSelectGoal(goals[3]) }
                GoalPill(goals[4], selectedGoal == goals[4]) { onSelectGoal(goals[4]) }
            }
        }
    }
}

@Composable
private fun GoalPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) OpalMint.copy(alpha = 0.25f) else Color(0xFF22262B))
            .border(1.dp, if (isSelected) OpalMint else OpalCardBorder, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) OpalMint else OpalTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ScreenTimeEstimateStep(
    goal: String,
    selectedEstimate: String,
    onSelectEstimate: (String) -> Unit,
    onBack: () -> Unit
) {
    val estimates = listOf(
        "< 1 hour",
        "1–3 hours",
        "3–4 hours",
        "4–5 hours",
        "5–7 hours",
        "7+ hours",
        "I don't know"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.prime_logo),
            contentDescription = "Prime Gemstone",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(text = goal, color = OpalMint, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "What's your average screen time per day? Best guess is okay.",
            color = OpalTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalPill(estimates[0], selectedEstimate == estimates[0]) { onSelectEstimate(estimates[0]) }
                GoalPill(estimates[1], selectedEstimate == estimates[1]) { onSelectEstimate(estimates[1]) }
                GoalPill(estimates[2], selectedEstimate == estimates[2]) { onSelectEstimate(estimates[2]) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GoalPill(estimates[3], selectedEstimate == estimates[3]) { onSelectEstimate(estimates[3]) }
                GoalPill(estimates[4], selectedEstimate == estimates[4]) { onSelectEstimate(estimates[4]) }
                GoalPill(estimates[5], selectedEstimate == estimates[5]) { onSelectEstimate(estimates[5]) }
            }
            GoalPill(estimates[6], selectedEstimate == estimates[6]) { onSelectEstimate(estimates[6]) }
        }
    }
}

@Composable
private fun BadNewsStatStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "The bad news is that you'll spend ",
                color = OpalTextPrimary,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "160 days on your phone this year.",
                color = OpalMint,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Meaning that you're on track to spend",
                color = OpalTextPrimary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Ghosted years stack
            Text(text = "20 years", color = Color(0x334AE3B5), fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text(text = "21 years", color = OpalMintLight, fontSize = 60.sp, fontWeight = FontWeight.Bold)
            Text(text = "22 years", color = Color(0x334AE3B5), fontSize = 38.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "of your life looking down at your phone. Yep, you read this right.",
                color = OpalTextPrimary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Projection of your current screen time habits, based on an average 16 waking hours each day.",
                color = OpalTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continue", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GoodNewsStatStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "The good news is that Prime can\nhelp you get back",
                color = OpalTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(text = "6 years", color = Color(0x334AE3B5), fontSize = 38.sp, fontWeight = FontWeight.Bold)
            Text(text = "7 years", color = OpalMintLight, fontSize = 60.sp, fontWeight = FontWeight.Bold)
            Text(text = "8 years", color = Color(0x334AE3B5), fontSize = 38.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "of your life free from distractions, and help you achieve your dreams.",
                color = OpalTextPrimary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "According to your profile combined with Prime's program.",
                color = OpalTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Continue", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PermissionsIntroStep(
    userName: String,
    overlayGranted: Boolean,
    screenTimeGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onRequestScreenTime: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.prime_logo),
            contentDescription = "Prime Gemstone",
            modifier = Modifier.size(110.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Reviewing your responses...", color = OpalTextSecondary, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "I can help with that, $userName.", color = OpalTextSecondary, fontSize = 16.sp)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "But to change your habits, I'm going to need your permission.",
            color = OpalTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display over other apps toggle card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(OpalCardBg)
                .border(
                    1.dp,
                    if (overlayGranted) OpalMint.copy(alpha = 0.5f) else OpalCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .clickable { onRequestOverlay() }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PictureInPicture,
                    contentDescription = null,
                    tint = if (overlayGranted) OpalMint else OpalTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Display over other apps",
                            color = OpalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (overlayGranted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active ✓",
                                color = OpalMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (overlayGranted)
                            "Permission granted in Android Settings."
                        else
                            "Tap to enable. So Prime can gently help you pause before opening distracting apps.",
                        color = OpalTextSecondary,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = overlayGranted,
                    onCheckedChange = { onRequestOverlay() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OpalMint
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Screen time toggle card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(OpalCardBg)
                .border(
                    1.dp,
                    if (screenTimeGranted) OpalMint.copy(alpha = 0.5f) else OpalCardBorder,
                    RoundedCornerShape(22.dp)
                )
                .clickable { onRequestScreenTime() }
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = if (screenTimeGranted) OpalMint else OpalTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Screen time",
                            color = OpalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (screenTimeGranted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active ✓",
                                color = OpalMint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (screenTimeGranted)
                            "Usage access granted in Android Settings."
                        else
                            "Reads app-usage duration on this device to show your daily screen time and progress.",
                        color = OpalTextSecondary,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = screenTimeGranted,
                    onCheckedChange = { onRequestScreenTime() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = OpalMint
                    )
                )
            }
        }

        Text(
            text = "Optional: you can change these permissions anytime in Android Settings.",
            color = OpalTextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = OpalMint, contentColor = Color.Black),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continue", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HowItWorks1Step(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("How it works", color = OpalTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Without noticing, you\nopen Instagram.",
                color = OpalTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Mock phone frame with Instagram app highlighted
        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 340.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF16191D))
                .border(2.dp, Color(0xFF2A2E35), RoundedCornerShape(32.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { rowIndex ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(3) { colIndex ->
                            if (rowIndex == 0 && colIndex == 0) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFE1306C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📷", fontSize = 20.sp)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF23272C))
                                )
                            }
                        }
                    }
                }
            }
        }

        Text("Tap to continue", color = OpalTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun HowItWorks2Step(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onNext() }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("How it works", color = OpalTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Then you start\ndoomscrolling, but this\ntime...",
                color = OpalTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .size(width = 240.dp, height = 340.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF16191D))
                .border(2.dp, Color(0xFF2A2E35), RoundedCornerShape(32.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF23272C))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1D2125))
                )
            }
        }

        Text("Tap to continue", color = OpalTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun HowItWorks3Step(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onNext() }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("How it works", color = OpalTextSecondary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Prime intelligently nudges\nyou to stop you from\nscrolling.",
                color = OpalTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Notification Nudge card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF23272C))
                .border(1.dp, OpalMint.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("◯", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Time to go outside",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = " • now", color = OpalTextSecondary, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "It's sunny outside with a high of 23° today, but you already spent 1h 23m on distracting apps.",
                        color = Color(0xFFD4D8E0),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Text("Tap to continue", color = OpalTextSecondary, fontSize = 14.sp)
    }
}

@Composable
private fun NotificationsPromptStep(
    onRequestNotification: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(28.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = OpalMint,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Allow Prime to send you notifications?",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestNotification,
                    colors = ButtonDefaults.buttonColors(containerColor = OpalMint, contentColor = Color.Black),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Don't allow",
                    color = OpalTextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clickable { onSkip() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun AccessibilityGuideStep(
    onRequestAccessibility: () -> Unit,
    isGranted: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "How to give Prime accessibility\npermissions",
                color = OpalTextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Why PRIME asks for this",
                color = OpalMint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "When enabled, PRIME checks which app is in the foreground and reads browser address-bar text to match your blocklist. This check runs on your device. PRIME does not need page contents or keystrokes. You can skip this permission, but app and browser blocking may be limited.",
                color = OpalTextSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            val steps = listOf(
                "Tap \"Installed apps\"",
                "Tap \"Prime\"",
                "Tap the first toggle",
                "Tap \"Allow\""
            )

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22262B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = step, color = OpalTextPrimary, fontSize = 16.sp)
                    }
                }
            }

            if (isGranted) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Accessibility service is active! ✓",
                    color = OpalMint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    if (!isGranted) {
                        onRequestAccessibility()
                    } else {
                        onNext()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (isGranted) "Continue" else "Grant permission",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "I've enabled it / Skip for now",
                    color = OpalTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onNext() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectDistractionsStep(
    apps: List<com.example.model.BlockedAppItem>,
    onToggleBlock: (String) -> Unit,
    onNext: () -> Unit
) {
    val distractionItems = apps.filter { it.category == "Distracting" }
    val blockedAppsCount = distractionItems.count { !it.isWebsite && it.isBlocked }
    val blockedWebsitesCount = distractionItems.count { it.isWebsite && it.isBlocked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "These apps seem to be your\nbiggest distractions",
            color = OpalTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(OpalCardBg)
                .border(1.dp, OpalCardBorder, RoundedCornerShape(24.dp))
                .padding(vertical = 8.dp)
        ) {
            items(distractionItems) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleBlock(item.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconBadge(item = item, size = 38.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = OpalTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(text = item.usageToday, color = OpalTextSecondary, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isBlocked) OpalMint.copy(alpha = 0.2f) else Color(0xFF262B32)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isBlocked) Icons.Default.Close else Icons.Default.Security,
                            contentDescription = if (item.isBlocked) "Blocked" else "Unblocked",
                            tint = if (item.isBlocked) OpalMint else OpalTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "$blockedAppsCount apps, $blockedWebsitesCount websites selected",
            color = OpalTextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Continue", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FinalFistBumpStep(userName: String, onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(bottom = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "$userName,\nyour next chapter starts now.",
                color = OpalTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Become your prime.",
                color = OpalMintLight,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                BenefitItem(
                    icon = Icons.Default.Security,
                    title = "More intentional screen time",
                    desc = "Build healthier screen habits with clear, personal goals."
                )
                BenefitItem(
                    icon = Icons.Default.Schedule,
                    title = "Protected focus sessions",
                    desc = "Create focus windows and block distractions when your shields are enabled."
                )
                BenefitItem(
                    icon = Icons.Default.CalendarToday,
                    title = "A plan that fits your day",
                    desc = "Set realistic goals for screen time, focus and daily routines."
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Let's solidify it with a fist bump",
                color = OpalTextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = OpalMint, contentColor = Color.Black),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_fist_bump_button")
            ) {
                Text("👊 Let's Do It", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BenefitItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF22262B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = OpalMint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, color = OpalTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = desc, color = OpalTextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun TopBackButton(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xFF22262B))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = OpalTextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
