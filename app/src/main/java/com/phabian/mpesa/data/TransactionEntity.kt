package com.phabian.mpesa.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["transactionCode"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["transactionType"]),
        Index(value = ["provider"])
    ]
)
data class TransactionEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val transactionCode: String?,

    val timestamp: Long,

    val amount: Long,

    val balanceAfter: Long?,

    val transactionType: TransactionType,

    val counterparty: String?,

    val phoneNumber: String?,

    /*
     * Account or reference supplied in the SMS.
     *
     * Examples:
     * Data Daily
     * 40076178
     * Tunukiwa
     */
    val accountReference: String?,

    /*
     * M-Pesa transaction charge.
     *
     * Examples:
     * KES 0.00
     * KES 5.00
     */
    val transactionCost: Long?,

    /*
     * Service/provider involved in the transaction.
     *
     * Examples:
     * ZIIDI
     * ZIIDI TRADER
     * Safaricom Offers
     * Co-operative Bank Money Transfer
     */
    val provider: String?,

    val category: String?,

    val rawMessageId: Long?,

    val parserVersion: String,

    val confidence: Double,

    val createdAt: Long
)

enum class TransactionType {

    SEND_MONEY,

    RECEIVE_MONEY,

    BUY_GOODS,

    PAYBILL,

    WITHDRAW,

    DEPOSIT,

    AIRTIME_PURCHASE,

    BANK_TRANSFER,

    LOAN_DISBURSEMENT,

    LOAN_REPAYMENT,

    REVERSAL,

    CHARGE,

    UNKNOWN
}

enum class ParseStatus {

    SUCCESS,

    PARTIAL,

    FAILED,

    DUPLICATE,

    UNSUPPORTED
}