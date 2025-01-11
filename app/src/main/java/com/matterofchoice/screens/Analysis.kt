package com.matterofchoice.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matterofchoice.viewmodel.AIViewModel


@Composable
fun Analysis(viewModel: AIViewModel = viewModel()) {
    val context = LocalContext.current

    val userChoices by viewModel.analysisChoices.collectAsState()
    val error by viewModel.errorAnalysis.collectAsState()
    val isLoading by viewModel.isLoadingAnalysis.collectAsState()


    if (!isLoading){
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = "Analysis Screen", fontSize = 32.sp, color = Color.Green,

                    )
                Button(
                    onClick = {
                        viewModel.loadAnalysis(context)
                    }
                ) {
                    Text(text = "Analysis")
                }
            }
        }
    }

    else if (userChoices.isNotEmpty()) {
        val scrollState = rememberScrollState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Text(text = userChoices, Modifier.padding(16.dp))
        }
    }
    else if (error.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()
        ) {
            Text(text = error)
        }
    }

}

