package com.example.aiaccounting.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.aiaccounting.data.model.Butler
import com.example.aiaccounting.data.model.ButlerManager
import com.example.aiaccounting.data.model.ButlerPersonality
import com.example.aiaccounting.ui.theme.HorseTheme2026Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButlerSettingsScreen(
    onNavigateBack: () -> Unit,
    currentButlerId: String = ButlerManager.BUTLER_XIAOCAINIANG,
    onButlerSelected: (String) -> Unit = {}
) {
    var showButlerDetail by remember { mutableStateOf<Butler?>(null) }
    val allButlers = remember { ButlerManager.getAllButlers() }
    val currentButler = remember { ButlerManager.getButlerById(currentButlerId) ?: ButlerManager.getDefaultButler() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HorseTheme2026Colors.Background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "🎭 AI管家",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorseTheme2026Colors.GoldenText
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = HorseTheme2026Colors.GoldenText
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = HorseTheme2026Colors.Primary
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                CurrentButlerCard(
                    butler = currentButler,
                    onClick = { showButlerDetail = currentButler }
                )
            }

            item {
                Text(
                    text = "选择管家",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorseTheme2026Colors.GoldenText,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(allButlers) { butler ->
                ButlerCard(
                    butler = butler,
                    isSelected = butler.id == currentButlerId,
                    onClick = {
                        showButlerDetail = butler
                    },
                    onSelect = {
                        onButlerSelected(butler.id)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    showButlerDetail?.let { butler ->
        ButlerDetailDialog(
            butler = butler,
            isSelected = butler.id == currentButlerId,
            onDismiss = { showButlerDetail = null },
            onSelect = {
                onButlerSelected(butler.id)
                showButlerDetail = null
            }
        )
    }
}

@Composable
fun CurrentButlerCard(
    butler: Butler,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = HorseTheme2026Colors.Secondary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = butler.avatarResId),
                contentDescription = butler.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(2.dp, HorseTheme2026Colors.GoldenText, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = butler.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HorseTheme2026Colors.GoldenText
                )
                Text(
                    text = butler.title,
                    fontSize = 14.sp,
                    color = HorseTheme2026Colors.GoldenText.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = butler.description,
                    fontSize = 12.sp,
                    color = HorseTheme2026Colors.GoldenText.copy(alpha = 0.6f),
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "当前选择",
                tint = HorseTheme2026Colors.GoldenText,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun ButlerCard(
    butler: Butler,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) HorseTheme2026Colors.Secondary else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = butler.avatarResId),
                contentDescription = butler.name,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = butler.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PersonalityTag(personality = butler.personality)
                }
                Text(
                    text = butler.title,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = HorseTheme2026Colors.Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PersonalityTag(personality: ButlerPersonality) {
    val (text, color) = when (personality) {
        ButlerPersonality.CUTE -> "可爱" to Color(0xFFFFB6C1)
        ButlerPersonality.ELEGANT -> "优雅" to Color(0xFFE6E6FA)
        ButlerPersonality.PROFESSIONAL -> "专业" to Color(0xFF87CEEB)
        ButlerPersonality.WARM -> "温暖" to Color(0xFFFFE4B5)
        ButlerPersonality.COOL -> "冷酷" to Color(0xFFB0C4DE)
        ButlerPersonality.MYSTERIOUS -> "神秘" to Color(0xFFDDA0DD)
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun ButlerDetailDialog(
    butler: Butler,
    isSelected: Boolean,
    onDismiss: () -> Unit,
    onSelect: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = butler.avatarResId),
                    contentDescription = butler.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, HorseTheme2026Colors.Primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = butler.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Text(
                    text = butler.title,
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(8.dp))

                PersonalityTag(personality = butler.personality)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = butler.description,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "专长",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    butler.specialties.take(3).forEach { specialty ->
                        Box(
                            modifier = Modifier
                                .background(
                                    HorseTheme2026Colors.Secondary,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = specialty,
                                fontSize = 12.sp,
                                color = HorseTheme2026Colors.GoldenText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("取消")
                    }

                    Button(
                        onClick = onSelect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HorseTheme2026Colors.Primary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            if (isSelected) "当前管家" else "选择",
                            color = HorseTheme2026Colors.GoldenText
                        )
                    }
                }
            }
        }
    }
}
