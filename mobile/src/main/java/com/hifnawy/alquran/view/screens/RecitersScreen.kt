package com.hifnawy.alquran.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.hifnawy.alquran.shared.domain.MediaManager
import com.hifnawy.alquran.shared.model.Reciter
import com.hifnawy.alquran.view.composables.PlayerContainer
import com.hifnawy.alquran.view.composables.RecitersList
import com.hifnawy.alquran.viewModel.MediaViewModel

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun RecitersScreen(
        mediaViewModel: MediaViewModel,
        navController: NavController
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val context = LocalContext.current
        var reciters by remember { mutableStateOf(listOf<Reciter>()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            MediaManager.whenRecitersReady(context) {
                reciters = it
                isLoading = false
            }
        }

        Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularWavyProgressIndicator(
                        modifier = Modifier.size(100.dp),
                        stroke = Stroke(width = 10f)
                )

                else      -> {
                    RecitersList(reciters = reciters) { reciter, moshaf ->
                        val reciterJson = Gson().toJson(reciter)
                        val moshafJson = Gson().toJson(moshaf)

                        navController.navigate(Screen.Surahs.route + "?reciter=$reciterJson&moshaf=$moshafJson")
                    }

                    PlayerContainer(mediaViewModel = mediaViewModel)
                }
            }
        }
    }
}
