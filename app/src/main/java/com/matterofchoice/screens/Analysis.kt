package com.matterofchoice.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.matterofchoice.R
import com.matterofchoice.common.GameButton
import com.matterofchoice.common.GameTextField
import com.matterofchoice.ui.theme.myFont
import com.matterofchoice.viewmodel.AIViewModel
import androidx.navigation.NavHostController


@Composable
fun Analysis(viewModel: AIViewModel, navController: NavHostController) {



    val state by viewModel.state



    var userRole by remember { mutableStateOf("") }


    if(state.isLoading) {
        Loader()
    }
    else if (state.analysisResult == null) {
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
                    viewModel.performAnalysis(userRole)

                },
                text = stringResource(R.string.analysis_my_choices)
            )

        }

    }
    else if (state.error != null) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.error_something_went_wrong),
                color = MaterialTheme.colorScheme.error,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.error!!,
                textAlign = TextAlign.Center
            )
        }
    } else {
        AnalysisUI(analysisResult = state.analysisResult!!,
            navController = navController )
    }


}

