
package com.phabian.mpesa.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val rawSmsDao: RawSmsDao
) {
    fun observeTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeAll()

    fun observeZidiiTransactions(): Flow<List<ZidiiTransactionRow>> =
        transactionDao.observeZidiiTransactions()

    suspend fun save(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun getOriginalSms(rawMessageId: Long): RawSmsEntity? =
        rawSmsDao.getById(rawMessageId)
}
