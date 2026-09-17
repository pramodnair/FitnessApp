package com.example.fitnessapp.ui.partner

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fitnessapp.data.model.PartnerDuelSummary
import com.example.fitnessapp.data.model.UserDailyScore
import com.example.fitnessapp.data.sync.SyncConnectionStatus

enum class PartnerSubTab(val label: String) {
    DUEL("🏆 Duel Score"),
    SYNC("📶 Wi-Fi Sync"),
    CHEERS("💬 Cheers & Love")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnerScreen(
    modifier: Modifier = Modifier,
    viewModel: PartnerViewModel = viewModel()
) {
    val duel by viewModel.partnerDuel.collectAsStateWithLifecycle()
    val cheer by viewModel.cheerMessage.collectAsStateWithLifecycle()
    val incomingCheer by viewModel.incomingCheer.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val pairCode by viewModel.pairCode.collectAsStateWithLifecycle()
    val syncNotice by viewModel.syncNotice.collectAsStateWithLifecycle()
    val localDeviceIp by viewModel.localDeviceIp.collectAsStateWithLifecycle()
    val localPort by viewModel.localPort.collectAsStateWithLifecycle()
    val isScanningSubnet by viewModel.isScanningSubnet.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    var showEditPairCodeDialog by remember { mutableStateOf(false) }
    var inputPairCode by remember(pairCode) { mutableStateOf(pairCode) }
    var showDirectIpDialog by remember { mutableStateOf(false) }
    var inputDirectIp by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Couples Fitness Duel & Accountability", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                actions = {
                    IconButton(
                        onClick = { viewModel.triggerManualSync() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                        }
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Sub-Menu Segmented Control
            var selectedSubTab by remember { mutableStateOf(PartnerSubTab.DUEL) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PartnerSubTab.entries.forEach { tab ->
                    val isSel = selectedSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { selectedSubTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Notification Banner
            AnimatedVisibility(visible = syncNotice != null, enter = fadeIn(), exit = fadeOut()) {
                syncNotice?.let { notice ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(notice, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.dismissSyncNotice() }) {
                                Text("Dismiss", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Incoming Cheer Notification (over Wi-Fi)
            AnimatedVisibility(visible = incomingCheer != null, enter = fadeIn(), exit = fadeOut()) {
                incomingCheer?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFCE4EC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Cheer from Partner! ❤️", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC2185B))
                                    Text(msg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color(0xFF880E4F))
                                }
                            }
                            OutlinedButton(onClick = { viewModel.dismissCheer() }) {
                                Text("Thanks!", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Sent Cheer Message Toast
            AnimatedVisibility(visible = cheer != null, enter = fadeIn(), exit = fadeOut()) {
                cheer?.let { msg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF6D00))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            TextButton(onClick = { viewModel.dismissCheer() }) {
                                Text("OK", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            when (selectedSubTab) {
                PartnerSubTab.DUEL -> {
                    // Trophy Winner Banner
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFB300),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                val u1 = duel.primaryUser
                                val u2 = duel.partnerUser
                                val isSynced = duel.isPartnerSynced

                                var u1Wins = 0
                                var u2Wins = 0
                                if (u1.adherencePercent > u2.adherencePercent) u1Wins++ else if (u2.adherencePercent > u1.adherencePercent) u2Wins++
                                if (u1.stepsTaken > u2.stepsTaken) u1Wins++ else if (u2.stepsTaken > u1.stepsTaken) u2Wins++
                                val u1WaterRatio = if (u1.waterTargetMl > 0) u1.waterIntakeMl.toFloat() / u1.waterTargetMl else 0f
                                val u2WaterRatio = if (u2.waterTargetMl > 0) u2.waterIntakeMl.toFloat() / u2.waterTargetMl else 0f
                                if (u1WaterRatio > u2WaterRatio) u1Wins++ else if (u2WaterRatio > u1WaterRatio) u2Wins++

                                Text(
                                    text = "DAILY 3-PILLAR LEADERBOARD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (!isSynced) {
                                        "Today's Tri-Duel in Progress ⚔️"
                                    } else if (u1Wins > u2Wins) {
                                        "${u1.userName} leads 3-Pillar Duel ($u1Wins - $u2Wins)! 🏆"
                                    } else if (u2Wins > u1Wins) {
                                        "${u2.userName} leads 3-Pillar Duel ($u2Wins - $u1Wins)! 🏆"
                                    } else {
                                        "Tied across fitness pillars! Keep pushing! 🔥"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isSynced) {
                                        "Pillars: Calories (${if (u1.adherencePercent > u2.adherencePercent) u1.userName else if (u2.adherencePercent > u1.adherencePercent) u2.userName else "Tie"}) • Steps (${if (u1.stepsTaken > u2.stepsTaken) u1.userName else if (u2.stepsTaken > u1.stepsTaken) u2.userName else "Tie"}) • Water (${if (u1WaterRatio > u2WaterRatio) u1.userName else if (u2WaterRatio > u1WaterRatio) u2.userName else "Tie"})"
                                    } else {
                                        "Waiting for partner's Wi-Fi sync to compare Calories, Steps & Water."
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sync Prompt Banner if partner not synced yet
                    if (!duel.isPartnerSynced) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedSubTab = PartnerSubTab.SYNC },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Wi-Fi Sync with Partner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Connect both phones to home Wi-Fi to sync your partner's live stats.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Head to Head Duel Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserDuelCard(
                            score = duel.primaryUser,
                            isWinning = duel.winnerName == duel.primaryUser.userName,
                            isPrimaryUser = true,
                            modifier = Modifier.weight(1f)
                        )
                        UserDuelCard(
                            score = duel.partnerUser,
                            isWinning = duel.winnerName == duel.partnerUser.userName,
                            isPrimaryUser = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Steps Duel Battle Card
                    PartnerStepsBattleCard(duel = duel)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Hydration Duel Battle Card
                    PartnerHydrationBattleCard(duel = duel)

                    Spacer(modifier = Modifier.height(14.dp))

                    // Weekly Trophy Stats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("4 Wins", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                Text("${duel.primaryUser.userName}'s Week", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("VS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("3 Wins", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE91E63))
                                Text("${duel.partnerUser.userName}'s Week", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                PartnerSubTab.SYNC -> {
                    // Live Connection Status Banner
                    WifiSyncStatusCard(
                        status = connectionStatus,
                        pairCode = pairCode,
                        onEditPairCode = { showEditPairCodeDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Independent 2-Phone Sync Controls Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Device Network & Pairing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                OutlinedButton(
                                    onClick = { showEditPairCodeDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Change Code", fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Connect both phones to your home Wi-Fi and verify both have the same Pair Code. If your router blocks auto-discovery, use Subnet Scan or Direct IP.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Local IP & Pair Code Info Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // This device's IP badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("THIS PHONE'S WI-FI IP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            val displayIp = localDeviceIp ?: "Scanning Wi-Fi..."
                                            Text(
                                                text = if (localDeviceIp != null) "$localDeviceIp:$localPort" else displayIp,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (localDeviceIp != null) {
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(localDeviceIp ?: ""))
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy IP",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Pair code badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp)
                                    ) {
                                        Text("PAIR CODE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = pairCode,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons: Sync Now, Scan Subnet, Connect by IP
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.triggerManualSync() },
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isSyncing,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text("Sync", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.scanSubnet() },
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = !isScanningSubnet,
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    if (isScanningSubnet) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text("Scan Subnet", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showDirectIpDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.1f)
                                ) {
                                    Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Direct IP", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Wi-Fi Troubleshooting Guide Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Home Wi-Fi Setup Tips", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• Ensure both phones are on the same Wi-Fi router (or one phone's hotspot).\n" +
                                "• Keep the exact same Pair Code on both devices.\n" +
                                "• If your router blocks multicast (common on Airtel/Jio/mesh routers), tap 'Scan Subnet' to find the partner automatically or tap 'Direct IP' and type the IP displayed on your partner's phone.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                PartnerSubTab.CHEERS -> {
                    Text(
                        text = "QUICK NUDGES & CHEERS (WI-FI DIRECT)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "🔥 You're crushing your calorie goal! Keep it up!",
                            "💧 Don't forget to drink water! Stay hydrated!",
                            "🥗 Making great healthy choices today! Proud of you!",
                            "💪 Fitness duel is on! Let's hit our targets together!"
                        ).forEach { cheerMsg ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.sendCheer(cheerMsg) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cheerMsg, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color(0xFFE91E63),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(90.dp))
        }
    }

    // Pair Code Edit Dialog
    if (showEditPairCodeDialog) {
        AlertDialog(
            onDismissRequest = { showEditPairCodeDialog = false },
            title = { Text("Set 6-Digit Pair Code", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Set the same Pair Code on both your phone and your wife's phone so they discover each other on Wi-Fi:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputPairCode,
                        onValueChange = { inputPairCode = it.uppercase().take(8) },
                        label = { Text("Pair Code (e.g. FIT-8842)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPairCode.isNotBlank()) {
                            viewModel.updatePairCode(inputPairCode.trim())
                        }
                        showEditPairCodeDialog = false
                    }
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPairCodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Direct IP Connect Dialog
    if (showDirectIpDialog) {
        AlertDialog(
            onDismissRequest = { showDirectIpDialog = false },
            title = { Text("Connect by Partner's IP", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Check your wife's phone screen under 'THIS PHONE'S WI-FI IP' and enter that IP address here:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputDirectIp,
                        onValueChange = { inputDirectIp = it.trim() },
                        label = { Text("Partner IP (e.g. 192.168.1.45)") },
                        placeholder = { Text("192.168.1.X") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputDirectIp.isNotBlank()) {
                            viewModel.connectDirectIp(inputDirectIp)
                        }
                        showDirectIpDialog = false
                    }
                ) {
                    Text("Connect & Sync")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectIpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WifiSyncStatusCard(
    status: SyncConnectionStatus,
    pairCode: String,
    onEditPairCode: () -> Unit
) {
    val (cardColor, iconColor, titleColor, subtitleColor, icon, title, subtitle) = when (status) {
        is SyncConnectionStatus.ConnectedWifi -> {
            Tuple7(
                Color(0xFF1B5E20).copy(alpha = 0.18f),
                Color(0xFF4CAF50),
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Default.Wifi,
                "Home Wi-Fi Live Sync Active 🟢",
                "Connected to ${status.partnerName} (${status.partnerIp}). Instant peer sync active."
            )
        }
        is SyncConnectionStatus.SearchingWifi -> {
            Tuple7(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                Color(0xFFFFB300),
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Default.Wifi,
                "Wi-Fi Active — Searching for Partner 🟡",
                "Listening for partner with pair code '$pairCode' on home Wi-Fi..."
            )
        }
        is SyncConnectionStatus.CloudFallback -> {
            Tuple7(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Default.CloudDone,
                "Remote Mode (Cloud Sync Ready) 🌐",
                status.statusMessage
            )
        }
        is SyncConnectionStatus.Disconnected -> {
            Tuple7(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.onSurfaceVariant,
                Icons.Default.WifiOff,
                "Network Offline",
                "Connect both phones to Wi-Fi to sync your fitness duel."
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = titleColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = subtitleColor
                )
            }
        }
    }
}

private data class Tuple7<A, B, C, D, E, F, G>(val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G)

@Composable
private fun UserDuelCard(
    score: UserDailyScore,
    isWinning: Boolean,
    isPrimaryUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val syncTimeStr = remember(score.lastSyncTimestamp) {
        if (score.lastSyncTimestamp > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(score.lastSyncTimestamp))
        } else ""
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinning) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = if (isWinning) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score.userName,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sync Status Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when {
                    isPrimaryUser -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    score.isLiveSynced -> Color(0xFFE8F5E9)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ) {
                Text(
                    text = when {
                        isPrimaryUser -> "📱 This Device"
                        score.isLiveSynced && syncTimeStr.isNotBlank() -> "🟢 Synced $syncTimeStr"
                        score.isLiveSynced -> "🟢 Synced"
                        else -> "⚪ Not Synced"
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isPrimaryUser -> MaterialTheme.colorScheme.onPrimaryContainer
                        score.isLiveSynced -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score Badge
            if (isPrimaryUser || score.isLiveSynced) {
                Text(
                    text = "${score.adherencePercent}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (score.adherencePercent >= 85) Color(0xFF2E7D32) else Color(0xFFE65100)
                )
                Text(
                    text = "Adherence Score",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "—",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Waiting for Sync",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats items
            DuelMetricRow("Calories", if (isPrimaryUser || score.isLiveSynced) "${score.caloriesConsumed} / ${score.calorieBudget}" else "—")
            if (score.proteinTargetG > 0f) {
                DuelMetricRow("Protein", if (isPrimaryUser || score.isLiveSynced) "${score.proteinConsumedG.toInt()} / ${score.proteinTargetG.toInt()}g" else "—")
            }
            DuelMetricRow("Water", if (isPrimaryUser || score.isLiveSynced) "${score.waterIntakeMl} / ${score.waterTargetMl} ml" else "—")
            if (score.currentWeightKg > 0f) {
                DuelMetricRow("Weight", "${score.currentWeightKg} kg")
            }
            if (score.weightLostKg > 0f) {
                DuelMetricRow("Lost", "-${score.weightLostKg} kg")
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Streak Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFE0B2)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${score.streakDays} Day Streak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

@Composable
private fun DuelMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PartnerStepsBattleCard(
    duel: PartnerDuelSummary,
    modifier: Modifier = Modifier
) {
    val u1 = duel.primaryUser
    val u2 = duel.partnerUser
    val isSynced = duel.isPartnerSynced

    val u1Progress = (u1.stepsTaken.toFloat() / u1.stepsTarget.toFloat()).coerceIn(0f, 1f)
    val u2Progress = (u2.stepsTaken.toFloat() / u2.stepsTarget.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF00B4D8).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.DirectionsWalk,
                                contentDescription = null,
                                tint = Color(0xFF00B4D8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DAILY STEPS BATTLE 👟",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00B4D8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isSynced) {
                                when {
                                    u1.stepsTaken > u2.stepsTaken -> "${u1.userName} leads by ${u1.stepsTaken - u2.stepsTaken} steps! 🥇"
                                    u2.stepsTaken > u1.stepsTaken -> "${u2.userName} leads by ${u2.stepsTaken - u1.stepsTaken} steps! 🥇"
                                    else -> "Tied at ${u1.stepsTaken} steps!"
                                }
                            } else {
                                "${u1.userName}: ${u1.stepsTaken} steps • Partner awaiting sync"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User 1 Steps Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${u1.userName} (You)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${u1.stepsTaken} / ${u1.stepsTarget} • ${u1.caloriesBurned} kcal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { u1Progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User 2 Steps Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(u2.userName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isSynced) "${u2.stepsTaken} / ${u2.stepsTarget} • ${u2.caloriesBurned} kcal" else "Not synced yet",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (isSynced) u2Progress else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE91E63),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PartnerHydrationBattleCard(
    duel: PartnerDuelSummary,
    modifier: Modifier = Modifier
) {
    val u1 = duel.primaryUser
    val u2 = duel.partnerUser
    val isSynced = duel.isPartnerSynced

    val u1Progress = if (u1.waterTargetMl > 0) (u1.waterIntakeMl.toFloat() / u1.waterTargetMl.toFloat()).coerceIn(0f, 1.5f) else 0f
    val u2Progress = if (u2.waterTargetMl > 0) (u2.waterIntakeMl.toFloat() / u2.waterTargetMl.toFloat()).coerceIn(0f, 1.5f) else 0f

    val waterBlue = Color(0xFF0288D1)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = waterBlue.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = waterBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DAILY HYDRATION DUEL 💧",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = waterBlue,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isSynced) {
                                when {
                                    u1Progress > u2Progress -> "${u1.userName} leads at ${(u1Progress * 100).toInt()}% of goal! 🥇"
                                    u2Progress > u1Progress -> "${u2.userName} leads at ${(u2Progress * 100).toInt()}% of goal! 🥇"
                                    else -> "Tied at ${(u1Progress * 100).toInt()}% hydration!"
                                }
                            } else {
                                "${u1.userName}: ${u1.waterIntakeMl} ml • Partner awaiting sync"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User 1 Water Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${u1.userName} (You)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${u1.waterIntakeMl} / ${u1.waterTargetMl} ml (${(u1Progress * 100).toInt()}%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { u1Progress.coerceAtMost(1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = waterBlue,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User 2 Water Bar
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(u2.userName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (isSynced) "${u2.waterIntakeMl} / ${u2.waterTargetMl} ml (${(u2Progress * 100).toInt()}%)" else "Not synced yet",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (isSynced) u2Progress.coerceAtMost(1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFE91E63),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
