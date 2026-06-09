package com.example.db

import kotlinx.coroutines.flow.Flow

class WalletRepository(private val walletDao: WalletDao, private val matchRewardDao: MatchRewardDao) {
    fun getWallet(playerId: String = "local_player"): Flow<Wallet?> = walletDao.getWallet(playerId)
    fun getAllRewards(): Flow<List<MatchReward>> = matchRewardDao.getAllRewards()

    suspend fun saveWallet(wallet: Wallet) {
        walletDao.insertWallet(wallet)
    }

    suspend fun recordSinglePlayerWin(difficulty: String, coins: Int) {
        val reward = MatchReward(
            playerId = "local_player",
            difficulty = difficulty,
            coinsWon = coins,
            matchId = java.util.UUID.randomUUID().toString()
        )
        matchRewardDao.insertReward(reward)
        walletDao.addWinCoins(amount = coins)
    }
    
    suspend fun recordBetLoss(amount: Int) {
        walletDao.addLostCoins(amount = amount)
    }
    
    suspend fun recordBetWin(amount: Int) {
        walletDao.addWinCoins(amount = amount)
    }
}
