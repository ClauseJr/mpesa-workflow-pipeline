
package com.phabian.mpesa.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

enum class ZidiiTransactionType {
    DEPOSIT,
    WITHDRAWAL
}

data class ZidiiTransaction(
    val transactionCode: String,
    val timestamp: Long,
    val amount: Long,
    val type: ZidiiTransactionType
)

data class ZidiiSummary(
    val totalInvested: Long,
    val totalWithdrawn: Long,
    val netContributions: Long,
    val depositCount: Int,
    val withdrawalCount: Int
)

object ZidiiAnalytics {

    private val depositPattern = Regex(
        """(?i)\bKsh\s*([\d,]+(?:\.\d{1,2})?)\s+sent\s+to\s+ZIIDI\b"""
    )

    private val withdrawalPattern = Regex(
        """(?i)\breceived\s+Ksh\s*([\d,]+(?:\.\d{1,2})?)\s+from\s+ZIIDI\b"""
    )

    private fun amountToCents(value: String): Long? =
        runCatching {
            BigDecimal(value.replace(",", ""))
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        }.getOrNull()

    fun classify(
        rows: List<ZidiiTransactionRow>
    ): List<ZidiiTransaction> {

        return rows.mapNotNull { row ->
            val code = row.transactionCode ?: return@mapNotNull null

            val depositMatch = depositPattern.find(row.smsBody)
            val withdrawalMatch = withdrawalPattern.find(row.smsBody)

            when {
                depositMatch != null -> {
                    val amount = amountToCents(
                        depositMatch.groupValues[1]
                    ) ?: return@mapNotNull null

                    ZidiiTransaction(
                        transactionCode = code,
                        timestamp = row.timestamp,
                        amount = amount,
                        type = ZidiiTransactionType.DEPOSIT
                    )
                }

                withdrawalMatch != null -> {
                    val amount = amountToCents(
                        withdrawalMatch.groupValues[1]
                    ) ?: return@mapNotNull null

                    ZidiiTransaction(
                        transactionCode = code,
                        timestamp = row.timestamp,
                        amount = amount,
                        type = ZidiiTransactionType.WITHDRAWAL
                    )
                }

                else -> null
            }
        }.distinctBy { it.transactionCode }
            .sortedByDescending { it.timestamp }
    }

    fun summarize(
        transactions: List<ZidiiTransaction>
    ): ZidiiSummary {

        val deposits = transactions.filter {
            it.type == ZidiiTransactionType.DEPOSIT
        }

        val withdrawals = transactions.filter {
            it.type == ZidiiTransactionType.WITHDRAWAL
        }

        val invested = deposits.sumOf { it.amount }
        val withdrawn = withdrawals.sumOf { it.amount }

        return ZidiiSummary(
            totalInvested = invested,
            totalWithdrawn = withdrawn,
            netContributions = invested - withdrawn,
            depositCount = deposits.size,
            withdrawalCount = withdrawals.size
        )
    }

    fun formatCurrency(amount: Long): String {
        val negative = amount < 0
        val absolute = BigDecimal.valueOf(amount).abs()
        val shillings = absolute
            .divide(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.DOWN)

        val formatter = NumberFormat.getNumberInstance(
            Locale("en", "KE")
        ).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        val sign = if (negative) "-" else ""
        return "KES $sign${formatter.format(shillings)}"
    }

    fun periodStart(
        timestamp: Long,
        period: String
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4

            when (period.lowercase(Locale.ROOT)) {
                "day" -> {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                "week" -> {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                "month" -> {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                "year" -> {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }

        return calendar.timeInMillis
    }
}
