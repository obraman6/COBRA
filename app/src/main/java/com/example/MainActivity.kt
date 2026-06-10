package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.BoardDark
import com.example.ui.theme.BoardHighlight
import com.example.ui.theme.BoardLight
import com.example.ui.theme.BoardValidMove
import com.example.ui.theme.KingGold
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PieceRed
import com.example.ui.theme.PieceWhite
import com.example.ui.theme.PaletteCream
import com.example.ui.theme.PaletteNavy
import com.example.ui.theme.PaletteLightBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
            FirebaseAuth.getInstance().signInAnonymously()
        } catch (e: Exception) {
            e.printStackTrace()
            // Needs Firebase credentials in .env
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val language by viewModel.language.collectAsState()
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            
            val locale = if (language == MainViewModel.Language.ENGLISH) java.util.Locale("en") else java.util.Locale("sw")
            val context = androidx.compose.ui.platform.LocalContext.current
            val configuration = android.content.res.Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            val localizedContext = remember(locale) { context.createConfigurationContext(configuration) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Ensure the app doesn't scale text based on system settings and uses standard density
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
                        density = androidx.compose.ui.platform.LocalDensity.current.density,
                        fontScale = 1f
                    ),
                    androidx.compose.ui.platform.LocalContext provides localizedContext,
                    androidx.compose.ui.platform.LocalConfiguration provides configuration
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        val appState by viewModel.appState.collectAsState()
                        val userName by viewModel.userName.collectAsState()

                        if (userName.isBlank()) {
                            NameScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background),
                                viewModel = viewModel
                            )
                        } else if (appState == AppState.MENU) {
                            MenuScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background),
                                viewModel = viewModel
                            )
                        } else if (appState == AppState.DASHBOARD) {
                            DashboardScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background),
                                viewModel = viewModel
                            )
                        } else {
                            GameScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(MaterialTheme.colorScheme.background),
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val language by viewModel.language.collectAsState()
    
    val wins by viewModel.wins.collectAsState()
    val losses by viewModel.losses.collectAsState()
    val totalGames by viewModel.totalGames.collectAsState()
    val totalDurationMs by viewModel.totalDurationMs.collectAsState()
    
    val averageDurationMs = if (totalGames > 0) totalDurationMs / totalGames else 0L
    val averageDurationSeconds = averageDurationMs / 1000
    val averageDurationMins = averageDurationSeconds / 60
    val averageDurationRemainderSecs = averageDurationSeconds % 60
    
    val totalMatches = wins + losses
    val winRatio = if (totalMatches > 0) wins.toFloat() / totalMatches.toFloat() else 0f

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.quitToMenu() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(stringResource(R.string.str_player_dashboard), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Average Duration Card
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.str_average_match_duration), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.str_averagedurationmins_m___aver, averageDurationMins, averageDurationRemainderSecs),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(stringResource(R.string.str_matches_played___totalgames, totalGames), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // Win / Loss Ratio Chart
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.str_win___loss_ratio__vs_ai), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (totalMatches > 0) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val errorColor = MaterialTheme.colorScheme.error
                    
                    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val winAngle = 360f * winRatio
                            val lossAngle = 360f - winAngle
                            
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = winAngle,
                                useCenter = true
                            )
                            drawArc(
                                color = errorColor,
                                startAngle = -90f + winAngle,
                                sweepAngle = lossAngle,
                                useCenter = true
                            )
                        }
                        
                        // Inner circle for donut chart look
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${(winRatio * 100).toInt()}%", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.str_wins___wins, wins), fontWeight = FontWeight.Medium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.str_losses___losses, losses), fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    Text(stringResource(R.string.str_no_matches_played_against_ai), fontSize = 16.sp, modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}

@Composable
fun NameScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val language by viewModel.language.collectAsState()

    var nameInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chess Pro TZ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.str_please_enter_your_name_to_star),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 24.dp, top = 8.dp),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { 
                        nameInput = it
                        showError = false 
                    },
                    label = { Text(stringResource(R.string.str_your_name)) },
                    isError = showError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                AnimatedVisibility(visible = showError) {
                    Text(
                        stringResource(R.string.str_please_enter_a_valid_name),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { 
                        if (nameInput.isNotBlank()) {
                            viewModel.setUserName(nameInput)
                        } else {
                            showError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.str_continue), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun MenuScreen(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val gameMode by viewModel.gameMode.collectAsState()
    val aiLevel by viewModel.aiLevel.collectAsState()
    val highestUnlockedAiLevel by viewModel.highestUnlockedAiLevel.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val wins by viewModel.wins.collectAsState()
    val losses by viewModel.losses.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val myColor by viewModel.myColor.collectAsState()
    val language by viewModel.language.collectAsState()

    var showJoinDialog by remember { mutableStateOf(false) }
    var joinIpStr by remember { mutableStateOf("") }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLeaderboard by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { Text(stringResource(R.string.str_game_rules), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.str_1__mandatory_jump__you_must_ca))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_2__multiple_jumps__if_another))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_3__king__a_piece_reaching_the))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_flying_king__tanzanian___ki))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_standard__kings_move_exactl))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.str_4__win__you_win_by_capturing_a))
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text(stringResource(R.string.str_i_understand))
                }
            }
        )
    }

    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text(stringResource(R.string.str_join_a_friend)) },
            text = {
                Column {
                    Text(stringResource(R.string.str_enter_friend_s_ip))
                    OutlinedTextField(
                        value = joinIpStr,
                        onValueChange = { joinIpStr = it },
                        singleLine = true,
                        placeholder = { Text("Mfano: 192.168.1.5") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setClientWiFi(joinIpStr)
                    viewModel.startGame()
                    showJoinDialog = false
                }) {
                    Text(stringResource(R.string.str_connect___play))
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text(stringResource(R.string.str_cancel))
                }
            }
        )
    }

    if (showLeaderboard) {
        val globalLeaderboard by viewModel.leaderboardManager.globalLeaderboard.collectAsState()

        Column(
            modifier = modifier
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showLeaderboard = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Leaderboard (Global)", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Local Stats Display
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("My Record (Local Stats)", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Wins", fontWeight = FontWeight.SemiBold)
                            Text("$wins", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Losses", fontWeight = FontWeight.SemiBold)
                            Text("$losses", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Text("Global Leaderboard", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

            if (globalLeaderboard.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Hakuna data (No records yet)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(globalLeaderboard.size) { index ->
                        val entry = globalLeaderboard[index]
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (entry.playerId == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid) 
                                    MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Column {
                                        Text(entry.playerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Score: ${entry.score}", fontSize = 14.sp)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Wins: ${entry.wins}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Streak: ${entry.winStreak} \uD83D\uDD25", fontSize = 14.sp, color = Color(0xFFFF5722))
                                }
                            }
                        }
                    }
                }
            }
        }
        return
    }

    if (showSettings) {
        var editedName by remember { mutableStateOf(userName) }
        val themeMode by viewModel.themeMode.collectAsState()

        Column(
            modifier = modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showSettings = false }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(stringResource(R.string.str_settings), fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.str_your_name_2), fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { if (editedName.isNotBlank()) viewModel.setUserName(editedName) },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.str_save))
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.str_rules), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = { showRulesDialog = true }) {
                            Text(stringResource(R.string.str_read_all_rules))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = rules == BoardState.Rules.TANZANIAN,
                            onClick = { viewModel.setRules(BoardState.Rules.TANZANIAN) }
                        )
                        Text(stringResource(R.string.str_tanzanian__flying_king))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = rules == BoardState.Rules.STANDARD,
                            onClick = { viewModel.setRules(BoardState.Rules.STANDARD) }
                        )
                        Text(stringResource(R.string.str_standard))
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.str_choose_your_colored_pieces), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UserPieceColor.values().forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.hex))
                                    .clickable { viewModel.setMyColor(c) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (myColor == c) {
                                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha=0.3f)))
                                    Icon(Icons.Filled.Star, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val language by viewModel.language.collectAsState()
                    Text(if (language == MainViewModel.Language.ENGLISH) "Language:" else "Lugha:", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = language == MainViewModel.Language.ENGLISH,
                            onClick = { viewModel.setLanguage(MainViewModel.Language.ENGLISH) }
                        )
                        Text("English", fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = language == MainViewModel.Language.SWAHILI,
                            onClick = { viewModel.setLanguage(MainViewModel.Language.SWAHILI) }
                        )
                        Text("Swahili", fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.str_theme), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeMode == ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                        )
                        Text(stringResource(R.string.str_system), fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeMode == ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                        )
                        Text(stringResource(R.string.str_light), fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = themeMode == ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                        )
                        Text(stringResource(R.string.str_dark), fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
                    Text(stringResource(R.string.str_haptic_feedback), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.setHapticsEnabled(true) }) {
                        RadioButton(
                            selected = hapticsEnabled,
                            onClick = { viewModel.setHapticsEnabled(true) }
                        )
                        Text(stringResource(R.string.str_enabled), fontWeight = FontWeight.Medium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.setHapticsEnabled(false) }) {
                        RadioButton(
                            selected = !hapticsEnabled,
                            onClick = { viewModel.setHapticsEnabled(false) }
                        )
                        Text(stringResource(R.string.str_disabled), fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val soundTheme by viewModel.soundTheme.collectAsState()
                    Text(stringResource(R.string.str_sound_theme), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    SoundTheme.values().forEach { st ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.setSoundTheme(st) }) {
                            RadioButton(
                                selected = soundTheme == st,
                                onClick = { viewModel.setSoundTheme(st) }
                            )
                            Text(st.display, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Text(
                text = "Developer: Eng. Ruben Rudovick",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
            )
        }
    } else {
        Column(
            modifier = modifier
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chess Pro TZ",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row {
                    IconButton(onClick = { viewModel.goToDashboard() }) {
                        Icon(Icons.Filled.List, contentDescription = "Dashboard", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showLeaderboard = true }) {
                        Icon(Icons.Filled.Star, contentDescription = "Leaderboard", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Text(
                text = "Habari, $userName! \uD83D\uDC4B",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Wallet Display
            val wallet by viewModel.wallet.collectAsState()
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.tertiary)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.str_your_coins), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Text("${wallet?.totalCoins ?: 0}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.str_won____wallet__wincoins____0, wallet?.winCoins ?: 0), color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.7f))
                        Text(stringResource(R.string.str_lost____wallet__lostcoins____0, wallet?.lostCoins ?: 0), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.str_wins), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("$wins", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.str_losses), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("$losses", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(stringResource(R.string.str_choose_game_mode), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp, top = 8.dp))

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            onClick = { viewModel.setGameMode(GameMode.VS_AI) },
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (gameMode == GameMode.VS_AI) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = if (gameMode == GameMode.VS_AI) 8.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.str_play_vs_computer), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                AnimatedVisibility(visible = gameMode == GameMode.VS_AI) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.str_select_difficulty), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiLevel.values().forEach { level ->
                                val isLocked = level.ordinal > highestUnlockedAiLevel.ordinal
                                FilterChip(
                                    selected = aiLevel == level,
                                    onClick = { if (!isLocked) viewModel.setAiLevel(level) },
                                    label = { Text(if (isLocked) "${level.display} 🔒" else level.display) },
                                    enabled = !isLocked
                                )
                            }
                        }
                        if (highestUnlockedAiLevel.ordinal < AiLevel.values().size - 1) {
                            Text(
                                "Shinda level ya sasa ili kufungua inayofuata.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            onClick = { viewModel.setGameMode(GameMode.WIFI) },
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (gameMode == GameMode.WIFI) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = if (gameMode == GameMode.WIFI) 8.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.str_local_network__wifi_hotspot), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                AnimatedVisibility(visible = gameMode == GameMode.WIFI) {
                    var betAmountStr by remember { mutableStateOf("0") }
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.str_bet_amount__coins), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = betAmountStr,
                            onValueChange = { betAmountStr = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { 
                                val bet = betAmountStr.toIntOrNull() ?: 0
                                NetworkManager.setBetAmount(bet)
                                viewModel.setHostWiFi()
                                viewModel.startGame()
                            }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.str_host_game))
                            }
                            Button(onClick = { 
                                val bet = betAmountStr.toIntOrNull() ?: 0
                                NetworkManager.setBetAmount(bet)
                                showJoinDialog = true 
                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.str_join_game), color = MaterialTheme.colorScheme.onSecondary)
                            }
                        }
                    }
                }
            }
        }

        OutlinedCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            onClick = { viewModel.setGameMode(GameMode.ONLINE) },
            colors = CardDefaults.outlinedCardColors(
                containerColor = if (gameMode == GameMode.ONLINE) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = if (gameMode == GameMode.ONLINE) 8.dp else 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.str_online_multiplayer), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                AnimatedVisibility(visible = gameMode == GameMode.ONLINE) {
                    var joinCode by remember { mutableStateOf("") }
                    val onlineStatus by viewModel.onlineMatchManager.connectionStatus.collectAsState()
                    val roomState by viewModel.onlineMatchManager.roomState.collectAsState()
                    val scope = rememberCoroutineScope()
                    
                    LaunchedEffect(roomState?.status) {
                        if (roomState?.status == "PLAYING") {
                            viewModel.startGame()
                        }
                    }
                    
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text("Status: $onlineStatus", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                        if (roomState != null) {
                            Text(stringResource(R.string.str_room_id____roomstate__roomid, roomState?.roomId ?: ""), fontSize = 20.sp, fontWeight = FontWeight.Black)
                            if (roomState?.status == "WAITING") {
                                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp).size(24.dp))
                                Text(stringResource(R.string.str_waiting_for_opponent), modifier = Modifier.padding(top = 8.dp))
                            } else if (roomState?.status == "PLAYING") {
                                Button(
                                    onClick = { viewModel.startGame() },
                                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(stringResource(R.string.str_return_to_game))
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { 
                                    scope.launch {
                                        viewModel.onlineMatchManager.createRoom(userName)
                                    }
                                }) {
                                    Text(stringResource(R.string.str_create_room))
                                }
                            }
                            
                            OutlinedTextField(
                                value = joinCode,
                                onValueChange = { joinCode = it },
                                label = { Text(stringResource(R.string.str_room_code)) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                            Button(onClick = { 
                                scope.launch {
                                    val success = viewModel.onlineMatchManager.joinRoom(joinCode, userName)
                                    if (success) {
                                        viewModel.startGame()
                                    }
                                }
                            }, modifier = Modifier.padding(top = 8.dp)) {
                                Text(stringResource(R.string.str_join_online_match))
                            }
                        }
                    }
                }
            }
        }
        
        if (gameMode != GameMode.WIFI && gameMode != GameMode.ONLINE) {
            Button(
                onClick = { viewModel.startGame() },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.str_start_game), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
    }
}

@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: MainViewModel = viewModel()) {
    val boardState by viewModel.boardState.collectAsState()
    val selectedPos by viewModel.selectedPos.collectAsState()
    val validMoves by viewModel.validMoves.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val aiLevel by viewModel.aiLevel.collectAsState()
    val alertMessage by viewModel.alertMessage.collectAsState()
    val winner by viewModel.winner.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val myColor by viewModel.myColor.collectAsState()
    val language by viewModel.language.collectAsState()
    val activeEmote by viewModel.activeEmote.collectAsState()
    val haptics = LocalHapticFeedback.current
    
    val isConnected by NetworkManager.isConnected.collectAsState()
    val netStatus by NetworkManager.connectionStatus.collectAsState()
    val netRole by NetworkManager.role.collectAsState()
    val opponentName by NetworkManager.opponentName.collectAsState()

    val p1Name = userName
    // For ONLINE mode, we could technically get the guest name, but usually it's fast. Let's just use "Mpinzani" or keep it generic
    val p2Name = if (gameMode == GameMode.VS_AI) "Kompyuta" else if (gameMode == GameMode.WIFI) (opponentName ?: "Mpinzani") else {
        val rs = viewModel.onlineMatchManager.roomState.value
        val isMimiHost = rs?.hostId == viewModel.onlineMatchManager.myPlayerId
        val opName = if (isMimiHost) rs?.guestName else rs?.hostName
        if (!opName.isNullOrEmpty()) opName else "Mpinzani"
    }

    val myPlayerSide = if (gameMode == GameMode.WIFI && netRole == NetworkRole.CLIENT) Player.RED else if (gameMode == GameMode.ONLINE && viewModel.onlineMatchManager.roomState.value?.guestId == viewModel.onlineMatchManager.myPlayerId) Player.RED else Player.WHITE

    val getPlayerName = { p: Player ->
        if (p == myPlayerSide) p1Name else p2Name
    }

    alertMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearAlert() },
            title = { Text("Taarifa (Alert)") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAlert() }) {
                    Text("Sawa")
                }
            }
        )
    }

    winner?.let { w ->
        val mshindiName = getPlayerName(w)
        AlertDialog(
            onDismissRequest = { viewModel.clearWinner() },
            title = { Text(stringResource(R.string.str_match_over)) },
            text = { Text(stringResource(R.string.str_winner_is___mshindiname__ud83c, mshindiName)) },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.clearWinner()
                    viewModel.restartGame() 
                }) {
                    Text(stringResource(R.string.str_play_again))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    viewModel.clearWinner()
                    viewModel.quitToMenu() 
                }) {
                    Text(stringResource(R.string.str_quit_match))
                }
            }
        )
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chess Pro TZ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = when(gameMode) {
                        GameMode.VS_AI -> "Mchezaji 1 vs Kompyuta (${aiLevel.display})"
                        GameMode.WIFI -> "Wachezaji 2 (WiFi)"
                        GameMode.ONLINE -> "Wachezaji 2 (Online)"
                        else -> ""
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = { viewModel.quitToMenu() }) {
                Icon(Icons.Filled.Star, contentDescription = "Menu")
            }
        }
        
        if (gameMode == GameMode.WIFI) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = netStatus, 
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Colors
        val myComposeColor = Color(myColor.hex)
        val opponentComposeColor = if (myColor.name == "RED") Color.White else PieceRed

        // Versus Banner
        val homeEmote = activeEmote?.takeIf { it.first == myPlayerSide }?.second
        val awayEmote = activeEmote?.takeIf { it.first != myPlayerSide }?.second

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home / Player 1
            Column(horizontalAlignment = Alignment.Start) {
                Text(if (gameMode == GameMode.WIFI || gameMode == GameMode.ONLINE) "HOME" else "MCHEZAJI 1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = myComposeColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p1Name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    AnimatedVisibility(visible = homeEmote != null) {
                        Text(homeEmote ?: "", fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            Text("VS", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.3f), fontSize = 20.sp)
            // Away / Player 2
            Column(horizontalAlignment = Alignment.End) {
                Text(if (gameMode == GameMode.WIFI || gameMode == GameMode.ONLINE) "AWAY" else "MPINZANI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = opponentComposeColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(visible = awayEmote != null) {
                        Text(awayEmote ?: "", fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(p2Name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Turn indicator
        val isMimi = boardState.currentPlayer == myPlayerSide
        val turnName = getPlayerName(boardState.currentPlayer)

        val turnColor = if (isMimi) myComposeColor else opponentComposeColor

        val turnText = if (gameMode == GameMode.VS_AI && !isMimi && isAiThinking) {
            "\uD83E\uDD16 $turnName inafikiri..."
        } else if (isMimi) {
            "Zamu yako ($turnName)"
        } else {
            "Zamu ya $turnName"
        }

        Text(
            text = turnText,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = turnColor,
            modifier = Modifier
                .background(
                    turnColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        val shouldRotate = myPlayerSide == Player.RED
        val boardRotation = if (shouldRotate) 180f else 0f

        // Board
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PaletteNavy, PaletteNavy.copy(alpha = 0.8f)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(2.dp, PaletteLightBlue, RoundedCornerShape(12.dp))
                .padding(12.dp) // The wooden rim
                .clip(RoundedCornerShape(4.dp))
                .background(BoardLight)
                .graphicsLayer { rotationZ = boardRotation }
        ) {
            val tileSize = maxWidth / 8
            
            // Draw background grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val w = canvasWidth / 8
                val h = canvasHeight / 8
                
                for (y in 0..7) {
                    for (x in 0..7) {
                        val isDark = (x + y) % 2 != 0
                        val color = if (isDark) BoardDark else BoardLight
                        drawRect(
                            color = color,
                            topLeft = Offset(x * w, y * h),
                            size = androidx.compose.ui.geometry.Size(w, h)
                        )
                        
                        // Adding bevel to dark squares for an etched look
                        if (isDark) {
                            drawLine(
                                color = Color.Black.copy(alpha = 0.5f),
                                start = Offset(x * w, y * h),
                                end = Offset((x + 1) * w, y * h),
                                strokeWidth = 6f
                            )
                            drawLine(
                                color = Color.Black.copy(alpha = 0.5f),
                                start = Offset(x * w, y * h),
                                end = Offset(x * w, (y + 1) * h),
                                strokeWidth = 6f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset(x * w, (y + 1) * h),
                                end = Offset((x + 1) * w, (y + 1) * h),
                                strokeWidth = 6f
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.2f),
                                start = Offset((x + 1) * w, y * h),
                                end = Offset((x + 1) * w, (y + 1) * h),
                                strokeWidth = 6f
                            )
                        }
                    }
                }
            }

            // Draw selection
            selectedPos?.let { pos ->
                Box(
                    modifier = Modifier
                        .offset(x = tileSize * pos.x, y = tileSize * pos.y)
                        .size(tileSize)
                        .background(BoardHighlight)
                )
            }
            
            // Draw capture paths
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val w = canvasWidth / 8
                val h = canvasHeight / 8
                
                validMoves.forEach { move ->
                    if (move.captured.isNotEmpty()) {
                        val startX = move.from.x * w + w / 2
                        val startY = move.from.y * h + h / 2
                        val endX = move.to.x * w + w / 2
                        val endY = move.to.y * h + h / 2
                        
                        drawLine(
                            color = Color.Red.copy(alpha = 0.4f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = w * 0.15f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                        
                        move.captured.forEach { cap ->
                            val capX = cap.x * w + w / 2
                            val capY = cap.y * h + h / 2
                            drawCircle(
                                color = Color.Red.copy(alpha = 0.5f),
                                radius = w * 0.4f,
                                center = Offset(capX, capY)
                            )
                        }
                    }
                }
            }
            
            // Draw valid destinations
            validMoves.forEach { move ->
                Box(
                    modifier = Modifier
                        .offset(x = tileSize * move.to.x, y = tileSize * move.to.y)
                        .size(tileSize)
                        .clickable { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.onSquareClicked(move.to)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(tileSize * 0.35f)
                            .background(BoardValidMove, shape = CircleShape)
                    )
                }
            }

            // Draw clickable overlay and pieces (for interactions)
            for (y in 0..7) {
                for (x in 0..7) {
                    val pos = Pos(x, y)
                    Box(
                        modifier = Modifier
                            .offset(x = tileSize * x, y = tileSize * y)
                            .size(tileSize)
                            .clickable { 
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.onSquareClicked(pos) 
                            }
                    ) {
                        val piece = boardState.pieces[pos]
                        if (piece != null) {
                            PieceView(piece = piece, size = tileSize, p1Color = myComposeColor, p2Color = opponentComposeColor, rotation = boardRotation)
                        }
                    }
                }
            }

            // Draw waiting overlay if not connected
            if (gameMode == GameMode.WIFI && !isConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Nasubiri Muunganisho...",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (gameMode == GameMode.ONLINE) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf("👏", "🤣", "😠", "😎", "🎯", "🤔").forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 28.sp,
                            modifier = Modifier.clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendEmote(emoji)
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            val history by viewModel.history.collectAsState()
            
            if ((gameMode == GameMode.VS_AI) && history.isNotEmpty()) {
                Button(
                    onClick = { viewModel.undoMove() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.str_undo_move))
                }
            }

            Button(
                onClick = { viewModel.restartGame() }, 
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.str_restart_match))
            }
            Button(
                onClick = { viewModel.quitToMenu() },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.str_quit_match))
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece, size: androidx.compose.ui.unit.Dp, p1Color: Color, p2Color: Color, rotation: Float = 0f) {
    val isRed = piece.player == Player.RED
    val baseColor = if (isRed) p2Color else p1Color
    
    // Create highlights and shadows based on the baseColor for a realistic 3D look
    val highlightColor = baseColor.copy(
        red = (baseColor.red + 0.3f).coerceAtMost(1f),
        green = (baseColor.green + 0.3f).coerceAtMost(1f),
        blue = (baseColor.blue + 0.3f).coerceAtMost(1f)
    )
    val shadowColor = baseColor.copy(
        red = (baseColor.red * 0.5f).coerceAtLeast(0f),
        green = (baseColor.green * 0.5f).coerceAtLeast(0f),
        blue = (baseColor.blue * 0.5f).coerceAtLeast(0f)
    )
    val deepShadow = shadowColor.copy(alpha = 0.8f)

    val elevation = if (piece.isKing) 12.dp else 6.dp
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(size * 0.08f)
            .shadow(elevation = elevation, shape = CircleShape, spotColor = Color.Black, ambientColor = deepShadow)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        // Outer bevel (creates the rim feeling)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(highlightColor, shadowColor),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = CircleShape
                )
        )
        
        // Inner ridge (gives depth to the top face)
        Box(
            modifier = Modifier
                .fillMaxSize(0.85f)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(shadowColor, highlightColor),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Flat top/face
            Box(
                modifier = Modifier
                    .fillMaxSize(0.88f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(highlightColor, baseColor, shadowColor),
                            center = Offset.Unspecified, // Defaults to center
                            radius = Float.POSITIVE_INFINITY
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Etched inner rings, classic look for checkers
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.65f)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(shadowColor.copy(alpha = 0.5f), Color.Transparent),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.9f)
                            .background(baseColor, CircleShape)
                    )
                }

                if (piece.isKing) {
                    Text(
                        text = "👑",
                        fontSize = (size.value * 0.35f).sp,
                        modifier = Modifier
                            .shadow(2.dp, CircleShape)
                            .graphicsLayer {
                                shadowElevation = 4f
                            }
                    )
                }
            }
        }
    }
}
