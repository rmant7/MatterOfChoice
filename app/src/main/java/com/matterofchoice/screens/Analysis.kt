package com.matterofchoice.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matterofchoice.R
import com.matterofchoice.ui.theme.MatterofchoiceTheme
import com.matterofchoice.ui.theme.myFont
import com.matterofchoice.viewmodel.AIViewModel


@Composable
fun Analysis(viewModel: AIViewModel = viewModel()) {
    val context = LocalContext.current


    val userChoices by viewModel.analysisChoices.collectAsState()
    val error by viewModel.errorAnalysis.collectAsState()
    val isLoading by viewModel.isLoadingAnalysis.collectAsState()

    DisposableEffect(Unit) {
        viewModel.resetAnalysisState()


        onDispose {

        }
    }

    var userRole by remember { mutableStateOf("") }
    val gradientColors = listOf(Color(0xFFFF00CC), Color(0xFF333399))

    if (!isLoading) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp)
                .padding(top = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.finish),
                contentDescription = "",
                modifier = Modifier.size(100.dp)
            )
            Text(
                text = "Analysis your selections", color = Color.Black,
                fontFamily = myFont, fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 15.dp, top = 28.dp)
            )

            OutlinedTextField(
                value = userRole,
                onValueChange = { userRole = it },
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                label = { Text(text = "Optional: Enter the role, e.g., Student", color = Color.Gray) }

            )

            Button(
                onClick = {
                    viewModel.loadAnalysis(context, userRole)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(top = 20.dp)
                    .background(
                        brush = Brush.horizontalGradient(gradientColors),
                        shape = RoundedCornerShape(16.dp)
                    ),
                colors = ButtonDefaults.buttonColors(Color.Transparent)
            ) {
                Text(
                    "start analysis",
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 5.dp,
                        bottom = 5.dp
                    ),
                    fontSize = 22.sp
                )
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Loader()
        }
    }
    if (userChoices.isNotEmpty()) {
        val scrollState = rememberScrollState()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
                .verticalScroll(scrollState)

        ) {
            Text(text = userChoices, Modifier.padding(16.dp))
        }
    }
    if (error.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Text(text = "Something went wrong")
        }
    }

}

@Preview
@Composable
fun MyPreview2() {
    MatterofchoiceTheme {
        Analysis()
    }


}
