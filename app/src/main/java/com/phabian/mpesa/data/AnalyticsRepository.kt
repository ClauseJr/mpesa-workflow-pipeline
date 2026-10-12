package com.phabian.mpesa.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject

class AnalyticsRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {

    fun observePeriodTransactions(
        start: Long,
        end: Long
    ): Flow<List<TransactionEntity>> =
        transactionDao.observeTransactionsBetween(
            start = start,
            end = end
        )

    fun observePeriodSummary(
        start: Long,
        end: Long
    ): Flow<AnalyticsSummary> {

        return transactionDao
            .observeTransactionsBetween(
                start = start,
                end = end
            )
            .map { transactions ->

                val income =
                    transactions
                        .filter {
                            it.transactionType in incomeTypes
                        }
                        .sumOf {
                            it.amount
                        }

                val expenditure =
                    transactions
                        .filter {
                            it.transactionType in expenditureTypes
                        }
                        .sumOf {
                            it.amount
                        }

                val openingBalance =
                    findOpeningBalance(
                        transactions = transactions
                    )

                val closingBalance =
                    findClosingBalance(
                        transactions = transactions
                    )

                AnalyticsSummary(
                    income = income,
                    expenditure = expenditure,
                    transactionCount = transactions.size,
                    openingBalance = openingBalance,
                    closingBalance = closingBalance
                )
            }
    }

    private fun findOpeningBalance(
        transactions: List<TransactionEntity>
    ): Long? {

        val firstTransaction =
            transactions
                .minByOrNull {
                    it.timestamp
                }

        return firstTransaction?.let {
            calculateBalanceBefore(it)
        }
    }

    private fun findClosingBalance(
        transactions: List<TransactionEntity>
    ): Long? {
        return transactions
            .filter { it.balanceAfter != null }
            .maxByOrNull { it.timestamp }
            ?.balanceAfter
    }

    private fun calculateBalanceBefore(
        transaction: TransactionEntity
    ): Long? {

        val balanceAfter =
            transaction.balanceAfter
                ?: return null

        return when (transaction.transactionType) {

            TransactionType.RECEIVE_MONEY,
            TransactionType.DEPOSIT,
            TransactionType.LOAN_DISBURSEMENT ->

                balanceAfter - transaction.amount

            TransactionType.REVERSAL ->

                balanceAfter

            else ->

                balanceAfter + transaction.amount
        }
    }

    companion object {

        private val incomeTypes =
            setOf(
                TransactionType.RECEIVE_MONEY,
                TransactionType.DEPOSIT,
                TransactionType.LOAN_DISBURSEMENT
            )

        private val expenditureTypes =
            setOf(
                TransactionType.SEND_MONEY,
                TransactionType.BUY_GOODS,
                TransactionType.PAYBILL,
                TransactionType.WITHDRAW,
                TransactionType.AIRTIME_PURCHASE,
                TransactionType.BANK_TRANSFER,
                TransactionType.LOAN_REPAYMENT,
                TransactionType.CHARGE
            )
    }
}