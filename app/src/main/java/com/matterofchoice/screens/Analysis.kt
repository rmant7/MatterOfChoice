package com.matterofchoice.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matterofchoice.AnalysisViewModel
import com.matterofchoice.R
import com.matterofchoice.common.GameButton
import com.matterofchoice.common.GameTextField
import com.matterofchoice.ui.theme.myFont
import com.matterofchoice.viewmodel.AIViewModel

@Preview
@Composable
fun Analysis(viewModel: AIViewModel = viewModel()) {
    val context = LocalContext.current


    val state by viewModel.state


    DisposableEffect(Unit) {

        onDispose {

        }
    }

    var userRole by remember { mutableStateOf("") }




    if (!state.isLoading) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(top = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.finish),
                contentDescription = "",
                modifier = Modifier.size(100.dp)
            )
            Text(
                text = stringResource(R.string.analysis_your_selections),
                fontFamily = myFont, fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 15.dp, top = 28.dp)
            )

            GameTextField(
                text = userRole,
                onValueChange = { userRole = it },
                labelTxt = stringResource(R.string.optional_enter_the_role_e_g_student)
            )

            GameButton(
                onClick = {
                    android.util.Log.d("Button", "Clicked")
                    viewModel.performAnalysis()
//                    viewModel.state.value.analysisResult//.loadAnalysis(context = context, role = userRole)
                },
                text = stringResource(R.string.analysis_my_choices)
            )

        }


        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Loader()
            }
        } else {
            state.analysisResult?.let { res ->

                AnalysisUI(res)

                 }
                    state.error?.let {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color = colorScheme.surface)
                        ) {
                            Text(text = state.error.toString())
                        }
                    }
                }
            }
        }




