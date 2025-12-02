package com.hifnawy.alquran.view.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.hifnawy.alquran.BuildConfig
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.R as Rs

@Composable
fun SettingsScreen() {
    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceDim)
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
    ) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = stringResource(R.string.navbar_settings),
                maxLines = 1,
                fontSize = 60.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                color = MaterialTheme.colorScheme.onSurface
        )
        Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
        ) {
            AppearanceSection()
            AboutSection()
        }
    }
}

@Composable
private fun AppearanceSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = stringResource(R.string.settings_appearance_section),
                maxLines = 1,
                fontSize = 25.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        ) {
            Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LanguageSettings()
                ThemeSettings()
                DynamicColorsSettings()
            }
        }
    }
}

@Composable
private fun LanguageSettings() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val intent = Intent().apply {
        action = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> Settings.ACTION_APP_LOCALE_SETTINGS
            else                                                  -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        data = "package:${activity?.packageName}".toUri()
    }

    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        isMenuExpanded = !isMenuExpanded
                        activity?.startActivity(intent)
                    }
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.language_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_language_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_language_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box {
                    Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                isMenuExpanded = true
                                activity?.startActivity(intent)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        val localeName = QuranApplication.currentLocale.language
                        val localeCountry = QuranApplication.currentLocale.country
                        val language = when {
                            localeCountry.isBlank() -> localeName
                            else                    -> "$localeName ($localeCountry)"
                        }
                        Text(
                                modifier = Modifier.basicMarquee(),
                                text = language,
                                fontSize = 25.sp,
                                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                                color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSettings() {
    val options = listOf(
            R.drawable.theme_light_mode_outlined_24px to R.drawable.theme_light_mode_filled_24px,
            R.drawable.theme_auto_outlined_24px to R.drawable.theme_auto_filled_24px,
            R.drawable.theme_dark_mode_outlined_24px to R.drawable.theme_dark_mode_filled_24px,
    )
    val haptic = LocalHapticFeedback.current

    var selectedIndex by remember { mutableIntStateOf(1) }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        selectedIndex = (selectedIndex + 1) % options.size
                    }
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.theme_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_theme_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_theme_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { index, icon ->
                        SegmentedButton(
                                selected = selectedIndex == index,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    selectedIndex = index
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = options.size
                                )
                        ) {
                            val icon = when (selectedIndex) {
                                index -> icon.second
                                else  -> icon.first
                            }
                            Icon(
                                    painter = painterResource(icon),
                                    tint = MaterialTheme.colorScheme.primary,
                                    contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DynamicColorsSettings() {
    val haptic = LocalHapticFeedback.current
    var checked by remember { mutableStateOf(true) }

    val onClick = {
        val hapticFeedbackType = when {
            checked -> HapticFeedbackType.ToggleOff
            else    -> HapticFeedbackType.ToggleOn
        }
        haptic.performHapticFeedback(hapticFeedbackType)
        checked = !checked
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick)
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.dynamic_colors_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_dynamic_colors_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_dynamic_colors_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Switch(
                        checked = checked,
                        onCheckedChange = { onClick() },
                        thumbContent = {
                            Icon(
                                    painter = when {
                                        checked -> painterResource(id = R.drawable.check_24px)
                                        else    -> painterResource(id = R.drawable.close_24px)
                                    },
                                    contentDescription = null
                            )
                        }
                )
            }
        }
    }
}

@Composable
private fun AboutSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
                modifier = Modifier
                    .basicMarquee()
                    .padding(10.dp),
                text = stringResource(R.string.settings_about_section),
                maxLines = 1,
                fontSize = 25.sp,
                fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        ) {
            Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NotificationsSettings()
                TranslationCard()
                PrivacyPolicyCard()
                ContactCard()
                AppDetailsCard()
            }
        }
    }
}

@Composable
private fun NotificationsSettings() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(Settings.EXTRA_APP_PACKAGE, activity?.packageName)
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        activity?.startActivity(intent)
                    }
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.notifications_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_notifications_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_notifications_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TranslationCard() {
    val haptic = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = stringResource(R.string.settings_translation_crowdin_url).toUri()
    }
    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        activity?.startActivity(intent)
                    }
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.translate_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_translation_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_translation_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyCard() {
    val haptic = LocalHapticFeedback.current

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.privacy_policy_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_privacy_policy_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_privacy_policy_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactCard() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        val versionName = BuildConfig.VERSION_NAME.split(".")
            .map { stringResource(R.string.locale_int, it.toInt()) }
            .joinToString(separator = ".") { it }

        data = "mailto:".toUri()
        putExtra(Intent.EXTRA_EMAIL, arrayOf(stringResource(R.string.settings_contact_developer_email)))
        putExtra(
                Intent.EXTRA_TEXT,
                """


                            -------------
                            ${stringResource(R.string.settings_about_version_code, BuildConfig.VERSION_CODE)}
                            ${stringResource(R.string.settings_about_version_name, versionName)}
                            ${stringResource(R.string.settings_contact_android_version, Build.VERSION.RELEASE)}
                            ${stringResource(R.string.settings_contact_android_sdk, Build.VERSION.SDK_INT)}
                            ${stringResource(R.string.settings_contact_device_info, "${Build.MANUFACTURER}: ${Build.MODEL}")}
                        """.trimIndent()
        )
    }

    Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp)
    ) {
        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        activity?.startActivity(intent)
                    }
        ) {
            Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                        modifier = Modifier
                            .size(50.dp)
                            .padding(horizontal = 10.dp),
                        painter = painterResource(id = R.drawable.mail_24px),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null
                )

                Column {
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_contact_label),
                            fontSize = 25.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                            modifier = Modifier.basicMarquee(),
                            text = stringResource(R.string.settings_contact_description),
                            fontSize = 20.sp,
                            fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDetailsCard() {
    val activity = LocalActivity.current
    val haptic = LocalHapticFeedback.current
    val versionName = BuildConfig.VERSION_NAME.split(".")
        .map { stringResource(R.string.locale_int, it.toInt()) }
        .joinToString(separator = ".") { it }
    val githubIntent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = activity?.getString(R.string.settings_about_github_url)?.toUri()
    }

    Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                        modifier = Modifier.size(250.dp),
                        painter = painterResource(id = R.drawable.app_icon_monochrome),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                )

                Text(
                        modifier = Modifier.basicMarquee(),
                        text = stringResource(R.string.app_name),
                        fontSize = 30.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                        modifier = Modifier.basicMarquee(),
                        text = stringResource(R.string.settings_about_version_code, BuildConfig.VERSION_CODE),
                        fontSize = 25.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                        modifier = Modifier.basicMarquee(),
                        text = stringResource(R.string.settings_about_version_name, versionName),
                        fontSize = 25.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                        modifier = Modifier.basicMarquee(),
                        text = stringResource(R.string.settings_about_developer_name),
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(Rs.font.aref_ruqaa)),
                        color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            activity?.startActivity(githubIntent)
                        }
                ) {
                    Icon(
                            painter = painterResource(R.drawable.github_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        },
                        enabled = false
                ) {
                    Icon(
                            painter = painterResource(R.drawable.crowdin_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        },
                        enabled = false
                ) {
                    Icon(
                            painter = painterResource(R.drawable.fdroid_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                        modifier = Modifier.size(80.dp),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        },
                        enabled = false
                ) {
                    Icon(
                            painter = painterResource(R.drawable.izzyondroid_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
@Preview(device = Devices.PIXEL_9_PRO_XL, locale = "ar")
fun SettingsScreenPreview() {
    SettingsScreen()
}
