package com.phabian.mpesa.data

data class AnalyticsSummary(
    val income: Long,
    val expenditure: Long,
    val transactionCount: Int,
    val openingBalance: Long?,
    val closingBalance: Long?
) {
    val netCashFlow: Long
        get() = income - expenditure
}