package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.ui.theme.OpalCardBg
import com.example.ui.theme.OpalCardBorder
import com.example.ui.theme.OpalMint
import com.example.ui.theme.OpalSurfaceDark
import com.example.ui.theme.OpalTextPrimary
import com.example.ui.theme.OpalTextSecondary
import com.example.util.FaviconManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWebsiteDialog(
    existingWebsites: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onAddWebsite: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var domainInput by remember { mutableStateOf("") }

    val cleanDomain = remember(domainInput) {
        FaviconManager.cleanDomain(domainInput)
    }

    val isValid = remember(cleanDomain) {
        FaviconManager.isValidDomain(cleanDomain)
    }

    val isAlreadyAdded = remember(cleanDomain, existingWebsites) {
        cleanDomain.isNotBlank() && existingWebsites.any { FaviconManager.cleanDomain(it).equals(cleanDomain, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = OpalSurfaceDark,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3A3F45))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add website to block",
                    color = OpalTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = OpalTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text input with real domain validation
            OutlinedTextField(
                value = domainInput,
                onValueChange = { domainInput = it },
                placeholder = {
                    Text(
                        text = "e.g. reddit.com or youtube.com",
                        color = OpalTextSecondary,
                        fontSize = 15.sp
                    )
                },
                leadingIcon = {
                    if (isValid && !isAlreadyAdded) {
                        SubcomposeAsyncImage(
                            model = FaviconManager.getFaviconUrl(cleanDomain),
                            contentDescription = cleanDomain,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            error = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = OpalMint,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = OpalTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                trailingIcon = {
                    if (cleanDomain.isNotBlank()) {
                        if (isValid && !isAlreadyAdded) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid domain",
                                tint = OpalMint,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = "Invalid domain",
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                isError = domainInput.isNotBlank() && (!isValid || isAlreadyAdded),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OpalMint,
                    unfocusedBorderColor = OpalCardBorder,
                    errorBorderColor = Color(0xFFFF5252),
                    focusedContainerColor = OpalCardBg,
                    unfocusedContainerColor = OpalCardBg,
                    focusedTextColor = OpalTextPrimary,
                    unfocusedTextColor = OpalTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("website_domain_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Live validation feedback
            when {
                cleanDomain.isBlank() -> {
                    Text(
                        text = "Enter any domain name. Its favicon will be fetched and saved locally.",
                        color = OpalTextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                isAlreadyAdded -> {
                    Text(
                        text = "This website is already in your list.",
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                !isValid -> {
                    Text(
                        text = "Please enter a valid domain format (e.g., example.com or twitter.com).",
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> {
                    Text(
                        text = "Valid domain: $cleanDomain (Favicon will be saved to local storage)",
                        color = OpalMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    if (isValid && !isAlreadyAdded) {
                        onAddWebsite(cleanDomain)
                    }
                },
                enabled = isValid && !isAlreadyAdded,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF262B32),
                    disabledContentColor = OpalTextSecondary
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("add_website_confirm_button")
            ) {
                Text(
                    text = "Add Website & Save Favicon",
                    color = if (isValid && !isAlreadyAdded) Color.Black else OpalTextSecondary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
