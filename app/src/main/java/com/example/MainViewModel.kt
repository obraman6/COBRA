package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.AppDatabase
import com.example.db.Wallet
import com.example.db.WalletRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameMode { VS_AI, PASS_AND_PLAY, WIFI, ONLINE }

enum class AppState { MENU, PLAYING, DASHBOARD }
enum class AiLevel(val display: String, val depth: Int) {
    EASY("Rahisi (Easy)", 1),
    HARD("Ngumu (Hard)", 3),
    SUPER_HARD("Ngumu Sana (Super Hard)", 5)
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class UserPieceColor(val hex: Long, val displayName: String) {
    WHITE(0xFFFFFFFF, "Nyeupe (Default)"),
    RED(0xFFD32F2F, "Nyekundu"),
    BLUE(0xFF1976D2, "Buluu (Blue)"),
    GREEN(0xFF388E3C, "Kijani (Green)"),
    GOLD(0xFFFFD700, "Dhahabu (Gold)"),
    PURPLE(0xFF7B1FA2, "Zambarau (Purple)")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("draft_stats", Context.MODE_PRIVATE)
    
    private val database = AppDatabase.getDatabase(application)
    private val walletRepository = WalletRepository(database.walletDao(), database.matchRewardDao())

    val onlineMatchManager = OnlineMatchManager()
    val leaderboardManager = LeaderboardManager()

    val wallet: StateFlow<Wallet?> = walletRepository.getWallet().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _themeMode = MutableStateFlow(
        run {
            val saved = prefs.getString("themeMode", ThemeMode.SYSTEM.name)
            ThemeMode.values().find { it.name == saved } ?: ThemeMode.SYSTEM
        }
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("hapticsEnabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("themeMode", mode.name).apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit().putBoolean("hapticsEnabled", enabled).apply()
    }

    private val _userName = MutableStateFlow(prefs.getString("userName", "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _myColor = MutableStateFlow(
        run {
            val saved = prefs.getString("myColor", UserPieceColor.WHITE.name)
            UserPieceColor.values().find { it.name == saved } ?: UserPieceColor.WHITE
        }
    )
    val myColor: StateFlow<UserPieceColor> = _myColor.asStateFlow()

    private val _soundTheme = MutableStateFlow(
        run {
            val saved = prefs.getString("soundTheme", SoundTheme.WOODEN.name)
            SoundTheme.values().find { it.name == saved } ?: SoundTheme.WOODEN
        }
    )
    val soundTheme: StateFlow<SoundTheme> = _soundTheme.asStateFlow()

    val soundManager = SoundManager()

    enum class Language { ENGLISH, SWAHILI }

    private val _language = MutableStateFlow(
        run {
            val saved = prefs.getString("language", Language.ENGLISH.name)
            Language.values().find { it.name == saved } ?: Language.ENGLISH
        }
    )
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _wins = MutableStateFlow(prefs.getInt("wins", 0))
    val wins: StateFlow<Int> = _wins.asStateFlow()

    private val _losses = MutableStateFlow(prefs.getInt("losses", 0))
    val losses: StateFlow<Int> = _losses.asStateFlow()

    private val _totalGames = MutableStateFlow(prefs.getInt("totalGames", 0))
    val totalGames: StateFlow<Int> = _totalGames.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(prefs.getLong("totalDurationMs", 0L))
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    fun setLanguage(lang: Language) {
        _language.value = lang
        prefs.edit().putString("language", lang.name).apply()
    }

    private var startMatchTimeMillis = 0L

    private val _appState = MutableStateFlow(AppState.MENU)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    private val _boardState = MutableStateFlow(GameEngine.createInitialBoard())
    val boardState: StateFlow<BoardState> = _boardState.asStateFlow()
    
    private val _history = MutableStateFlow<List<BoardState>>(emptyList())
    val history: StateFlow<List<BoardState>> = _history.asStateFlow()

    fun undoMove() {
        if (_history.value.isNotEmpty()) {
            val lastState = _history.value.last()
            _history.value = _history.value.dropLast(1)
            _boardState.value = lastState
            _selectedPos.value = null
            _validMoves.value = emptyList()
            _winner.value = null
            
            if (_gameMode.value == GameMode.VS_AI && lastState.currentPlayer == Player.RED) {
                undoMove() // Undo AI move as well
            }
        }
    }

    private val _selectedPos = MutableStateFlow<Pos?>(null)
    val selectedPos: StateFlow<Pos?> = _selectedPos.asStateFlow()

    private val _validMoves = MutableStateFlow<List<Move>>(emptyList())
    val validMoves: StateFlow<List<Move>> = _validMoves.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _highestUnlockedAiLevel = MutableStateFlow(
        run {
            val saved = prefs.getString("highestUnlockedAiLevel", AiLevel.EASY.name)
            AiLevel.values().find { it.name == saved } ?: AiLevel.EASY
        }
    )
    val highestUnlockedAiLevel: StateFlow<AiLevel> = _highestUnlockedAiLevel.asStateFlow()

    private val _aiLevel = MutableStateFlow(_highestUnlockedAiLevel.value)
    val aiLevel: StateFlow<AiLevel> = _aiLevel.asStateFlow()

    private val _rules = MutableStateFlow(BoardState.Rules.TANZANIAN)
    val rules: StateFlow<BoardState.Rules> = _rules.asStateFlow()
    
    private val _gameMode = MutableStateFlow(GameMode.VS_AI)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _alertMessage = MutableStateFlow<String?>(null)
    val alertMessage: StateFlow<String?> = _alertMessage.asStateFlow()

    private val _winner = MutableStateFlow<Player?>(null)
    val winner: StateFlow<Player?> = _winner.asStateFlow()

    private var _rewardGivenForCurrentGame = false
    
    private val _activeEmote = MutableStateFlow<Pair<Player, String>?>(null)
    val activeEmote: StateFlow<Pair<Player, String>?> = _activeEmote.asStateFlow()

    init {
        viewModelScope.launch {
            leaderboardManager.loadGlobalLeaderboard()
        }
        viewModelScope.launch {
            walletRepository.getWallet().collect { wallet ->
                if (wallet == null) {
                    walletRepository.saveWallet(Wallet())
                }
            }
        }
        NetworkManager.setMoveCallback { move ->
            // Received a move from the network
            applyMove(move)
        }
        viewModelScope.launch {
            var lastEmoteStr = ""
            onlineMatchManager.roomState.collect { room ->
                if (_gameMode.value == GameMode.ONLINE && room != null && room.status == "PLAYING") {
                    
                    if (room.lastEmote.isNotEmpty() && room.lastEmote != lastEmoteStr) {
                        lastEmoteStr = room.lastEmote
                        val parts = room.lastEmote.split("|")
                        if (parts.size >= 3) {
                            val senderId = parts[0]
                            val emoji = parts[1]
                            val amHost = room.hostId == onlineMatchManager.myPlayerId
                            val senderSide = if (senderId == room.hostId) Player.WHITE else Player.RED
                            
                            _activeEmote.value = Pair(senderSide, emoji)
                            // Clear emote after 2.5 seconds
                            viewModelScope.launch {
                                delay(2500)
                                if (_activeEmote.value?.second == emoji) {
                                    _activeEmote.value = null
                                }
                            }
                        }
                    }

                    if (room.lastMove.isNotEmpty()) {
                        val amHost = room.hostId == onlineMatchManager.myPlayerId
                        val myPlayerSide = if (amHost) Player.WHITE else Player.RED
                        // If it's my turn in the local state, but the server says the other guy just moved
                        val lastMove = parseMoveString(room.lastMove)
                        if (lastMove != null && _boardState.value.currentPlayer == myPlayerSide) {
                            applyMove(lastMove)
                        }
                    }
                }
            }
        }
    }

    fun sendEmote(emoji: String) {
        if (_gameMode.value == GameMode.ONLINE) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    onlineMatchManager.sendEmote(onlineMatchManager.roomState.value?.roomId ?: "", emoji)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun parseMoveString(msg: String): Move? {
        val parts = msg.split(",")
        if (parts.size >= 4) {
            val fx = parts[0].toIntOrNull() ?: return null
            val fy = parts[1].toIntOrNull() ?: return null
            val tx = parts[2].toIntOrNull() ?: return null
            val ty = parts[3].toIntOrNull() ?: return null
            val captured = mutableListOf<Pos>()
            if (parts.size >= 5 && parts[4].isNotEmpty()) {
                parts[4].split(";").forEach { cStr ->
                    val cParts = cStr.split("-")
                    if (cParts.size == 2) {
                        captured.add(Pos(cParts[0].toInt(), cParts[1].toInt()))
                    }
                }
            }
            return Move(Pos(fx, fy), Pos(tx, ty), captured)
        }
        return null
    }

    private fun moveToString(move: Move): String {
        val capturedStr = move.captured.joinToString(";") { "${it.x}-${it.y}" }
        return "${move.from.x},${move.from.y},${move.to.x},${move.to.y},${capturedStr}"
    }

    fun startGame() {
        _appState.value = AppState.PLAYING
        restartGame()
    }

    fun setUserName(name: String) {
        if (name.isNotBlank()) {
            _userName.value = name.trim()
            prefs.edit().putString("userName", name.trim()).apply()
        }
    }

    fun setMyColor(color: UserPieceColor) {
        _myColor.value = color
        prefs.edit().putString("myColor", color.name).apply()
    }

    fun setSoundTheme(theme: SoundTheme) {
        _soundTheme.value = theme
        prefs.edit().putString("soundTheme", theme.name).apply()
    }

    fun quitToMenu() {
        _appState.value = AppState.MENU
        NetworkManager.disconnect()
    }

    fun goToDashboard() {
        _appState.value = AppState.DASHBOARD
    }

    fun clearAlert() {
        _alertMessage.value = null
    }

    fun clearWinner() {
        _winner.value = null
    }

    fun onSquareClicked(pos: Pos) {
        if (_isAiThinking.value) return
        if (_winner.value != null) return
        if (_gameMode.value == GameMode.WIFI && !NetworkManager.isConnected.value) return
        
        val currentState = _boardState.value
        
        // If human vs AI and it's AI's turn, ignore clicks
        if (_gameMode.value == GameMode.VS_AI && currentState.currentPlayer == Player.RED) return

        // If WiFi mode, ignore clicks if it's not my turn
        if (_gameMode.value == GameMode.WIFI) {
            val myRole = NetworkManager.role.value
            if (myRole == NetworkRole.HOST && currentState.currentPlayer != Player.WHITE) return
            if (myRole == NetworkRole.CLIENT && currentState.currentPlayer != Player.RED) return
        }

        // If ONLINE mode, ignore clicks if it's not my turn
        if (_gameMode.value == GameMode.ONLINE) {
            val room = onlineMatchManager.roomState.value
            if (room != null) {
                val amHost = room.hostId == onlineMatchManager.myPlayerId
                val myPlayerSide = if (amHost) Player.WHITE else Player.RED
                if (currentState.currentPlayer != myPlayerSide) return
            } else {
                return
            }
        }

        val clickedPiece = currentState.pieces[pos]

        if (clickedPiece != null && clickedPiece.player == currentState.currentPlayer) {
            // Select piece
            val moves = GameEngine.getValidMoves(currentState)

            if (currentState.multiCapturePos != null && pos != currentState.multiCapturePos) {
                _alertMessage.value = "Tahadhari: Lazima uendelee kula na kete uliyotumia!"
                _selectedPos.value = currentState.multiCapturePos
                _validMoves.value = moves
                return
            }
            
            // If mandatory capture, filter out pieces that can't capture
            val mandatoryCapture = moves.any { it.captured.isNotEmpty() }
            
            val validForPiece = moves.filter { it.from == pos }
            
            if (mandatoryCapture && validForPiece.all { it.captured.isEmpty() }) {
                // Must pick a piece that can capture
                _alertMessage.value = "Tahadhari/Alert: Lazima ule kete! Chagua kete inayoweza kula."
                return
            }

            _selectedPos.value = pos
            _validMoves.value = validForPiece
        } else if (_selectedPos.value != null) {
            // Attempt to move
            val move = _validMoves.value.find { it.to == pos }
            if (move != null) {
                applyMove(move)
                if (_gameMode.value == GameMode.WIFI) {
                    NetworkManager.sendMove(move)
                }
                if (_gameMode.value == GameMode.ONLINE) {
                    viewModelScope.launch(Dispatchers.IO) {
                        try {
                            onlineMatchManager.sendMove(onlineMatchManager.roomState.value?.roomId ?: "", moveToString(move), _boardState.value.currentPlayer)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } else {
                _selectedPos.value = null
                _validMoves.value = emptyList()
            }
        }
    }

    private fun triggerHapticFeedback(isCapture: Boolean) {
        if (!_hapticsEnabled.value) return
        try {
            val vibrator = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val duration = if (isCapture) 60L else 30L
                    it.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    val duration = if (isCapture) 60L else 30L
                    @Suppress("DEPRECATION")
                    it.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Haptic feedback error", e)
        }
    }

    private fun applyMove(move: Move) {
        val isCapture = move.captured.isNotEmpty()
        if (isCapture) {
            soundManager.playCaptureSound(_soundTheme.value)
        } else {
            soundManager.playMoveSound(_soundTheme.value)
        }
        triggerHapticFeedback(isCapture)
        val current = _boardState.value
        _history.value = _history.value + current
        _selectedPos.value = null
        _validMoves.value = emptyList()
        val newState = GameEngine.applyMove(current, move)
        _boardState.value = newState

        checkWinner(newState)

        if (_winner.value == null) {
            if (newState.multiCapturePos != null) {
                // Auto-select the piece that must continue capturing
                _selectedPos.value = newState.multiCapturePos
                _validMoves.value = GameEngine.getValidMoves(newState).filter { it.from == newState.multiCapturePos }
                
                if (_gameMode.value == GameMode.VS_AI && newState.currentPlayer == Player.RED) {
                    // It's still AI's turn, it will be handled by the triggerAiMove loop because we don't trigger it again here, OR we do need to trigger it if it wasn't triggered?
                    // Actually, if we are here, it means a human made a move or AI made a move.
                    // But triggerAiMove has a look that checks:
                    // if (newState.currentPlayer == Player.RED && _winner.value == null) { triggerAiMove(newState) }
                    // So we shouldn't trigger it here.
                }
            } else if (_gameMode.value == GameMode.VS_AI && newState.currentPlayer == Player.RED) {
                triggerAiMove(newState)
            }
        }
    }

    private fun checkWinner(state: BoardState) {
        val moves = GameEngine.getValidMoves(state)
        var w: Player? = null
        if (moves.isEmpty()) {
            w = if (state.currentPlayer == Player.WHITE) Player.RED else Player.WHITE
        } else {
            val whiteCount = state.pieces.values.count { it.player == Player.WHITE }
            val redCount = state.pieces.values.count { it.player == Player.RED }
            if (whiteCount == 0) w = Player.RED
            if (redCount == 0) w = Player.WHITE
        }

        if (w != null) {
            _winner.value = w
            
            // Update Duration and Total Games
            if (startMatchTimeMillis > 0) {
                val matchDuration = System.currentTimeMillis() - startMatchTimeMillis
                _totalDurationMs.value += matchDuration
                prefs.edit().putLong("totalDurationMs", _totalDurationMs.value).apply()
                startMatchTimeMillis = 0L // reset so we don't accidentally add it again
            }
            
            _totalGames.value += 1
            prefs.edit().putInt("totalGames", _totalGames.value).apply()

            if (_gameMode.value == GameMode.VS_AI) {
                if (w == Player.WHITE) {
                    _wins.value += 1
                    prefs.edit().putInt("wins", _wins.value).apply()
                    if (!_rewardGivenForCurrentGame) {
                        viewModelScope.launch {
                            leaderboardManager.updatePlayerStats(_userName.value, true)
                            val coins = when (_aiLevel.value) {
                                AiLevel.EASY -> 50
                                AiLevel.HARD -> 150
                                AiLevel.SUPER_HARD -> 300
                            }
                            walletRepository.recordSinglePlayerWin(_aiLevel.value.name, coins)

                            // Unlock next level if they won on their highest currently unlocked level
                            if (_aiLevel.value == _highestUnlockedAiLevel.value) {
                                val currentOrdinal = _aiLevel.value.ordinal
                                if (currentOrdinal < AiLevel.values().size - 1) {
                                    val nextLevel = AiLevel.values()[currentOrdinal + 1]
                                    _highestUnlockedAiLevel.value = nextLevel
                                    prefs.edit().putString("highestUnlockedAiLevel", nextLevel.name).apply()
                                    _aiLevel.value = nextLevel
                                    _alertMessage.value = "Hongera! Umeshinda level hii na kufungua ${nextLevel.display}! \uD83D\uDD13"
                                }
                            }
                        }
                        _rewardGivenForCurrentGame = true
                    }
                } else if (w == Player.RED) {
                    _losses.value += 1
                    prefs.edit().putInt("losses", _losses.value).apply()
                    if (!_rewardGivenForCurrentGame) {
                        viewModelScope.launch {
                            leaderboardManager.updatePlayerStats(_userName.value, false)
                        }
                        _rewardGivenForCurrentGame = true
                    }
                }
            } else if (_gameMode.value == GameMode.WIFI) {
                // handle wifi bets
                if (NetworkManager.role.value != NetworkRole.NONE && NetworkManager.betAmount.value > 0) {
                    val amHost = NetworkManager.role.value == NetworkRole.HOST
                    val myPlayer = if (amHost) Player.WHITE else Player.RED
                    if (!_rewardGivenForCurrentGame) {
                        viewModelScope.launch {
                            if (w == myPlayer) {
                                leaderboardManager.updatePlayerStats(_userName.value, true)
                                walletRepository.recordBetWin(NetworkManager.betAmount.value)
                            } else {
                                leaderboardManager.updatePlayerStats(_userName.value, false)
                                walletRepository.recordBetLoss(NetworkManager.betAmount.value)
                            }
                        }
                        _rewardGivenForCurrentGame = true
                    }
                }
            } else if (_gameMode.value == GameMode.ONLINE) {
                val room = onlineMatchManager.roomState.value
                if (room != null) {
                    val amHost = room.hostId == onlineMatchManager.myPlayerId
                    val myPlayer = if (amHost) Player.WHITE else Player.RED
                    if (!_rewardGivenForCurrentGame) {
                        viewModelScope.launch {
                            if (w == myPlayer) {
                                leaderboardManager.updatePlayerStats(_userName.value, true)
                            } else {
                                leaderboardManager.updatePlayerStats(_userName.value, false)
                            }
                        }
                        _rewardGivenForCurrentGame = true
                    }
                }
            }
        }
    }

    private fun triggerAiMove(state: BoardState) {
        _isAiThinking.value = true
        viewModelScope.launch {
            val depth = _aiLevel.value.depth
            val bestMove = withContext(Dispatchers.Default) {
                GameEngine.getBestMove(state, depth)
            }
            if (bestMove != null) {
                delay(500) // Simulate thinking time for UX
                applyMove(bestMove)
                
                // If it is STILL AI's turn (multi-capture), trigger again
                if (_boardState.value.currentPlayer == Player.RED && _winner.value == null) {
                    triggerAiMove(_boardState.value)
                }
            }
            _isAiThinking.value = false
        }
    }
    
    fun setRules(newRules: BoardState.Rules) {
        _rules.value = newRules
    }
    
    fun setGameMode(mode: GameMode) {
        _gameMode.value = mode
        if (mode != GameMode.WIFI) {
            NetworkManager.disconnect()
        }
    }
    
    fun setAiLevel(level: AiLevel) {
        _aiLevel.value = level
    }

    fun setHostWiFi() {
        setGameMode(GameMode.WIFI)
        NetworkManager.startHost(_userName.value)
    }

    fun setClientWiFi(ip: String) {
        setGameMode(GameMode.WIFI)
        NetworkManager.connectToHost(_userName.value, ip)
    }

    fun restartGame() {
        startMatchTimeMillis = System.currentTimeMillis()
        _boardState.value = GameEngine.createInitialBoard(_rules.value)
        _selectedPos.value = null
        _validMoves.value = emptyList()
        _isAiThinking.value = false
        _winner.value = null
        _rewardGivenForCurrentGame = false
    }
}
