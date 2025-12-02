package com.hifnawy.alquran.view.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.hifnawy.alquran.shared.QuranApplication
import com.hifnawy.alquran.shared.utils.LogDebugTree.Companion.debug
import com.hifnawy.alquran.view.navigation.NavGraph
import com.hifnawy.alquran.view.theme.AppTheme
import timber.log.Timber

/**
 * The main activity of the app.
 *
 * @author AbdElMoniem ElHifnawy
 */
class MainActivity : ComponentActivity() {

    /**
     * Called when the activity is created.
     *
     * @param savedInstanceState [Bundle] If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in [onSaveInstanceState].
     *     Note: Otherwise it is null.
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            var layoutDirection by rememberSaveable {
                mutableStateOf(
                        when {
                            QuranApplication.currentLocale.isRTL -> LayoutDirection.Rtl
                            else  -> LayoutDirection.Ltr
                        }
                )
            }

            LaunchedEffect(QuranApplication.currentLocale.isRTL) {
                Timber.debug("language: ${QuranApplication.currentLocale.language}, isRTL: ${QuranApplication.currentLocale.isRTL}")
                layoutDirection = when {
                    QuranApplication.currentLocale.isRTL -> LayoutDirection.Rtl
                    else  -> LayoutDirection.Ltr
                }
            }

            AppTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    NavGraph()
                }
            }
        }
    }
}
