package com.example

import kotlin.math.abs
import kotlin.math.max

enum class Player { RED, WHITE } // RED goes "down" (+y), WHITE goes "up" (-y)

data class Pos(val x: Int, val y: Int) {
    operator fun plus(other: Pos) = Pos(x + other.x, y + other.y)
    operator fun minus(other: Pos) = Pos(x - other.x, y - other.y)
    operator fun times(scalar: Int) = Pos(x * scalar, y * scalar)
    val isValid get() = x in 0..7 && y in 0..7
}

data class Piece(val player: Player, val isKing: Boolean = false)

data class Move(val from: Pos, val to: Pos, val captured: List<Pos> = emptyList())

data class BoardState(
    val pieces: Map<Pos, Piece>,
    val currentPlayer: Player,
    val rules: Rules = Rules.TANZANIAN,
    val multiCapturePos: Pos? = null,
    val deadPieces: Set<Pos> = emptySet()
) {
    enum class Rules { STANDARD, TANZANIAN }
}

class GameEngine {
    companion object {
        fun applyMove(state: BoardState, move: Move): BoardState {
            val piece = state.pieces[move.from] ?: return state
            val newPieces = state.pieces.toMutableMap()
            newPieces.remove(move.from)
            for (c in move.captured) newPieces.remove(c)
            
            val newDeadPieces = state.deadPieces.toMutableSet()
            newDeadPieces.addAll(move.captured)
            
            var nextTurn = state.currentPlayer
            var nextMultiCapturePos: Pos? = null
            var finalPiece: Piece = piece

            if (move.captured.isNotEmpty()) {
                val tempState = state.copy(pieces = newPieces, deadPieces = newDeadPieces)
                val furtherMoves = getMovesForPiece(tempState, move.to, piece).filter { it.captured.isNotEmpty() }
                
                if (furtherMoves.isNotEmpty()) {
                    nextTurn = state.currentPlayer
                    nextMultiCapturePos = move.to
                } else {
                    nextTurn = if (state.currentPlayer == Player.WHITE) Player.RED else Player.WHITE
                    
                    var promote = false
                    if (piece.player == Player.WHITE && move.to.y == 0 && !piece.isKing) promote = true
                    if (piece.player == Player.RED && move.to.y == 7 && !piece.isKing) promote = true
                    if (promote) finalPiece = piece.copy(isKing = true)
                }
            } else {
                nextTurn = if (state.currentPlayer == Player.WHITE) Player.RED else Player.WHITE
                
                var promote = false
                if (piece.player == Player.WHITE && move.to.y == 0 && !piece.isKing) promote = true
                if (piece.player == Player.RED && move.to.y == 7 && !piece.isKing) promote = true
                if (promote) finalPiece = piece.copy(isKing = true)
            }
            
            newPieces[move.to] = finalPiece
            
            return state.copy(
                pieces = newPieces, 
                currentPlayer = nextTurn, 
                multiCapturePos = nextMultiCapturePos,
                deadPieces = if (nextMultiCapturePos != null) newDeadPieces else emptySet()
            )
        }

        fun createInitialBoard(rules: BoardState.Rules = BoardState.Rules.TANZANIAN): BoardState {
            val pieces = mutableMapOf<Pos, Piece>()
            for (y in 0..2) {
                for (x in 0..7) {
                    if ((x + y) % 2 == 1) pieces[Pos(x, y)] = Piece(Player.RED)
                }
            }
            for (y in 5..7) {
                for (x in 0..7) {
                    if ((x + y) % 2 == 1) pieces[Pos(x, y)] = Piece(Player.WHITE)
                }
            }
            return BoardState(pieces, Player.WHITE, rules)
        }

        fun getValidMoves(state: BoardState): List<Move> {
            val allMoves = mutableListOf<Move>()
            val player = state.currentPlayer
            
            val piecesToCheck = if (state.multiCapturePos != null) {
                listOfNotNull(state.pieces[state.multiCapturePos]?.let { state.multiCapturePos to it })
            } else {
                state.pieces.filter { it.value.player == player }.toList()
            }

            for ((pos, piece) in piecesToCheck) {
                allMoves.addAll(getMovesForPiece(state, pos, piece))
            }

            // Mandatory capture rule
            val captureMoves = allMoves.filter { it.captured.isNotEmpty() }
            if (captureMoves.isNotEmpty() && state.rules == BoardState.Rules.TANZANIAN) {
                var maxCaptures = 0
                val moveMaxCap = mutableMapOf<Move, Int>()
                for (move in captureMoves) {
                    val capSeqLen = getMaxCaptureSequenceLength(state, move)
                    moveMaxCap[move] = capSeqLen
                    if (capSeqLen > maxCaptures) maxCaptures = capSeqLen
                }
                return captureMoves.filter { moveMaxCap[it] == maxCaptures }
            } else if (captureMoves.isNotEmpty()) {
                return captureMoves
            } else {
                return allMoves
            }
        }
        
        private fun getMaxCaptureSequenceLength(state: BoardState, move: Move): Int {
            val piece = state.pieces[move.from] ?: return 1
            val newPieces = state.pieces.toMutableMap()
            newPieces.remove(move.from)
            for (c in move.captured) newPieces.remove(c)
            
            val newDeadPieces = state.deadPieces.toMutableSet()
            newDeadPieces.addAll(move.captured)
            
            val tempState = state.copy(pieces = newPieces, deadPieces = newDeadPieces)
            val furtherMoves = getMovesForPiece(tempState, move.to, piece).filter { it.captured.isNotEmpty() }
            
            if (furtherMoves.isEmpty()) {
                return 1
            }
            return 1 + furtherMoves.maxOf { getMaxCaptureSequenceLength(tempState, it) }
        }
        
        private fun getMovesForPiece(state: BoardState, pos: Pos, piece: Piece): List<Move> {
            return if (piece.isKing && state.rules == BoardState.Rules.TANZANIAN) {
                getFlyingKingMoves(state, pos, piece)
            } else if (piece.isKing) {
                getStandardKingMoves(state, pos, piece)
            } else {
                getPawnMoves(state, pos, piece)
            }
        }

        private val dirs = listOf(Pos(1, 1), Pos(1, -1), Pos(-1, 1), Pos(-1, -1))

        private fun getPawnMoves(state: BoardState, pos: Pos, piece: Piece): List<Move> {
            val moves = mutableListOf<Move>()
            val forwardDirs = if (piece.player == Player.WHITE) listOf(Pos(1, -1), Pos(-1, -1)) else listOf(Pos(1, 1), Pos(-1, 1))

            // Non-capturing moves (only forward)
            for (dir in forwardDirs) {
                val nextPos = pos + dir
                if (nextPos.isValid && !state.pieces.containsKey(nextPos) && !state.deadPieces.contains(nextPos)) {
                    moves.add(Move(pos, nextPos))
                }
            }

            // Capturing moves (pawns capture forward AND backward in Tanzanian rules)
            val captureDirs = if (state.rules == BoardState.Rules.TANZANIAN) dirs else forwardDirs
            for (dir in captureDirs) {
                val jumpPos = pos + dir * 2
                val capturePos = pos + dir
                if (jumpPos.isValid && !state.pieces.containsKey(jumpPos) && !state.deadPieces.contains(jumpPos)) {
                    val targetPiece = state.pieces[capturePos]
                    if (targetPiece != null && targetPiece.player != piece.player) {
                        moves.add(Move(pos, jumpPos, listOf(capturePos)))
                    }
                }
            }
            return moves
        }

        private fun getStandardKingMoves(state: BoardState, pos: Pos, piece: Piece): List<Move> {
            val moves = mutableListOf<Move>()
            for (dir in dirs) {
                val nextPos = pos + dir
                if (nextPos.isValid && !state.pieces.containsKey(nextPos) && !state.deadPieces.contains(nextPos)) {
                    moves.add(Move(pos, nextPos))
                }
                val jumpPos = pos + dir * 2
                val capturePos = pos + dir
                if (jumpPos.isValid && !state.pieces.containsKey(jumpPos) && !state.deadPieces.contains(jumpPos)) {
                    val targetPiece = state.pieces[capturePos]
                    if (targetPiece != null && targetPiece.player != piece.player) {
                        moves.add(Move(pos, jumpPos, listOf(capturePos)))
                    }
                }
            }
            return moves
        }

        private fun getFlyingKingMoves(state: BoardState, pos: Pos, piece: Piece): List<Move> {
            val moves = mutableListOf<Move>()
            
            for (dir in dirs) {
                var current = pos + dir
                var capturedPiecePos: Pos? = null
                
                while (current.isValid && !state.deadPieces.contains(current)) {
                    val p = state.pieces[current]
                    if (p != null) {
                        if (p.player == piece.player) {
                            break // Blocked by own piece
                        } else {
                            if (capturedPiecePos != null) {
                                break // Can't jump two pieces in a row in a single slide
                            } else {
                                capturedPiecePos = current
                            }
                        }
                    } else {
                        // Empty square
                        if (capturedPiecePos != null) {
                            moves.add(Move(pos, current, listOf(capturedPiecePos)))
                        } else {
                            moves.add(Move(pos, current))
                        }
                    }
                    current += dir
                }
            }
            return moves
        }

        // --- AI (Minimax with Alpha-Beta Pruning) ---
        fun getBestMove(state: BoardState, depth: Int): Move? {
            val moves = getValidMoves(state)
            if (moves.isEmpty()) return null

            var bestVal = Int.MIN_VALUE
            var bestMove = moves.random()

            for (move in moves) {
                val nextState = applyMove(state, move)
                val moveVal = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, state.currentPlayer)
                if (moveVal > bestVal) {
                    bestVal = moveVal
                    bestMove = move
                }
            }
            return bestMove
        }

        private fun minimax(state: BoardState, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean, aiPlayer: Player): Int {
            if (depth == 0) return evaluateState(state, aiPlayer)

            val moves = getValidMoves(state)
            if (moves.isEmpty()) {
                return if (isMaximizing) -10000 else 10000 // Loss or Win
            }

            var a = alpha
            var b = beta

            if (isMaximizing) {
                var maxEval = Int.MIN_VALUE
                for (move in moves) {
                    val eval = minimax(applyMove(state, move), depth - 1, a, b, false, aiPlayer)
                    maxEval = max(maxEval, eval)
                    a = max(a, eval)
                    if (b <= a) break
                }
                return maxEval
            } else {
                var minEval = Int.MAX_VALUE
                for (move in moves) {
                    val eval = minimax(applyMove(state, move), depth - 1, a, b, true, aiPlayer)
                    minEval = Math.min(minEval, eval)
                    b = Math.min(b, eval)
                    if (b <= a) break
                }
                return minEval
            }
        }

        private fun evaluateState(state: BoardState, aiPlayer: Player): Int {
            var score = 0
            for ((pos, piece) in state.pieces) {
                val value = if (piece.isKing) 30 else 10
                if (piece.player == aiPlayer) {
                    score += value
                } else {
                    score -= value
                }
            }
            return score
        }
    }
}
