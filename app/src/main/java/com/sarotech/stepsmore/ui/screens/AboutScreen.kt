package com.sarotech.stepsmore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.stepsmore.BuildConfig
import com.sarotech.stepsmore.ui.components.PreferenceRow
import com.sarotech.stepsmore.ui.components.SubScreenTopBar
import com.sarotech.stepsmore.ui.theme.StepsMoreColors

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StepsMoreColors.BgPrimary)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(title = "About", onBack = onBack)

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(StepsMoreColors.SurfaceCard)
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "StepsMore",
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                color = StepsMoreColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Minimal AMOLED Step Tracker with Hourly Insights",
                fontSize = 13.sp,
                color = StepsMoreColors.TextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PreferenceRow(
            icon = Icons.Outlined.Person,
            title = "Developer",
            subtitle = "Prashant Latta",
            showChevron = false
        )
        PreferenceRow(
            icon = Icons.Outlined.Business,
            title = "Company",
            subtitle = "SAROTECH",
            showChevron = false
        )
        PreferenceRow(
            icon = Icons.Outlined.Verified,
            title = "Version",
            subtitle = BuildConfig.VERSION_NAME,
            showChevron = false,
            showBottomDivider = false
        )
    }
}
