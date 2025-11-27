package com.hifnawy.alquran.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hifnawy.alquran.R
import com.hifnawy.alquran.shared.repository.DataError

@Composable
fun DataErrorScreen(dataError: DataError, errorMessage: String) {
    when (dataError) {
        is DataError.LocalError   -> LocalErrorScreen(localError = dataError, errorMessage = errorMessage)
        is DataError.NetworkError -> NetworkErrorScreen(networkError = dataError, errorMessage = errorMessage)
        is DataError.ParseError   -> ParseErrorScreen(parseError = dataError, errorMessage = errorMessage)
    }
}

@Composable
fun LocalErrorScreen(localError: DataError.LocalError, errorMessage: String) {
}

@Composable
fun NetworkErrorScreen(networkError: DataError.NetworkError, errorMessage: String) {
    val scrollState = rememberScrollState()
    Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                painter = painterResource(id = R.drawable.cloud_off_24px),
                contentDescription = "Network Error",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
        )

        Text(
                text = errorMessage,
                textAlign = TextAlign.Center,
                lineHeight = 65.sp,
                maxLines = 3,
                fontSize = 50.sp,
                color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ParseErrorScreen(parseError: DataError.ParseError, errorMessage: String) {
}