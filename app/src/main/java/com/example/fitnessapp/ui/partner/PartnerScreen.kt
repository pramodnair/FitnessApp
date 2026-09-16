package com.example.fitnessapp.ui.partner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.CloudDone
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
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFF8E1))
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
                                Text(
                                    text = "DAILY LEADERBOARD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF57F17),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (duel.winnerName != null) "${duel.winnerName} is winning today! 🏆" else "It's a tie today! Keep pushing! 🔥",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF4E342E)
                                )
                                Text(
                                    text = "Score based on calorie budget adherence & healthy hydration.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8D6E63)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Head to Head Duel Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserDuelCard(
                            score = duel.primaryUser,
                            isWinning = duel.winnerName == duel.primaryUser.userName,
                            modifier = Modifier.weight(1f)
                        )
                        UserDuelCard(
                            score = duel.partnerUser,
                            isWinning = duel.winnerName == duel.partnerUser.userName,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
    val (bgColor, icon, title, subtitle) = when (status) {
        is SyncConnectionStatus.ConnectedWifi -> {
            Tuple4(
                Color(0xFFE8F5E9),
                Icons.Default.Wifi,
                "Home Wi-Fi Live Sync Active 🟢",
                "Connected to ${status.partnerName} (${status.partnerIp}). Instant peer sync active."
            )
        }
        is SyncConnectionStatus.SearchingWifi -> {
            Tuple4(
                Color(0xFFFFF8E1),
                Icons.Default.Wifi,
                "Wi-Fi Active — Searching for Partner 🟡",
                "Listening for partner with pair code '$pairCode' on home Wi-Fi..."
            )
        }
        is SyncConnectionStatus.CloudFallback -> {
            Tuple4(
                Color(0xFFE1F5FE),
                Icons.Default.CloudDone,
                "Remote Mode (Cloud Sync Ready) 🌐",
                status.statusMessage
            )
        }
        is SyncConnectionStatus.Disconnected -> {
            Tuple4(
                Color(0xFFF5F5F5),
                Icons.Default.WifiOff,
                "Network Offline",
                "Connect both phones to Wi-Fi to sync your fitness duel."
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
private fun UserDuelCard(
    score: UserDailyScore,
    isWinning: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWinning) Color(0xFFF1F8E9) else MaterialTheme.colorScheme.surface
        ),
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
            Spacer(modifier = Modifier.height(8.dp))

            // Score Badge
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

            Spacer(modifier = Modifier.height(12.dp))

            // Stats items
            DuelMetricRow("Calories", "${score.caloriesConsumed} / ${score.calorieBudget}")
            DuelMetricRow("Water", "${score.waterIntakeMl} / ${score.waterTargetMl} ml")
            DuelMetricRow("Weight", "${score.currentWeightKg} kg")
            DuelMetricRow("Lost", "-${score.weightLostKg} kg")

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
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
