package com.matterofchoice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matterofchoice.ui.theme.MyColor
import com.matterofchoice.ui.theme.titleFont

@Composable
fun CustomRadioButton(
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Color.Green else Color.Gray

    Box(
        modifier = Modifier
            .height(50.dp)
            .width(150.dp)// Customize the size
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(20) // Rounded shape
            )
            .background(
                color = if (selected) Color.LightGray else Color.Transparent,
                shape = RoundedCornerShape(20)
            )
            .clickable { onClick() },
    ){
        Text("This is the first test on custom button", modifier = Modifier.padding(10.dp))
    }
}

@Composable
fun RadioGroup() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp)

    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Scenario",
                fontFamily = titleFont,
                textAlign = TextAlign.Justify,
                fontSize = 28.sp,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold
            )
            Image(painter = painterResource(R.drawable.fire),
                modifier = Modifier.size(38.dp),
                contentDescription = null)
            Text(
                text = "10/20",
                fontFamily = titleFont,
                textAlign = TextAlign.Justify,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold

            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCustomRadioButton() {
    RadioGroup()
}