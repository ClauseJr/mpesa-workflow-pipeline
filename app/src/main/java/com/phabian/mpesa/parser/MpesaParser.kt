package com.phabian.mpesa.parser

import com.phabian.mpesa.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.math.RoundingMode

data class ParsedTransaction(
    val transactionCode: String?,
    val timestamp: Long,
    val amount: Long,
    val balanceAfter: Long?,
    val transactionType: TransactionType,
    val counterparty: String?,
    val phoneNumber: String?,
    val accountReference: String?,
    val transactionCost: Long?,
    val provider: String?,
    val confidence: Double
)

class MpesaParser {

    /*
     * M-Pesa transaction code
     * Example: UJ8469VEK0
     */
    private val codeRegex =
        Regex("\\b([A-Z0-9]{10})\\b")

    /*
     * First KES/Ksh amount in the SMS is the transaction amount.
     */
    private val amountRegex =
        Regex(
            "(?i)(?:Ksh|KES)\\s*([\\d,]+(?:\\.\\d{1,2})?)"
        )

    /*
     * Exact M-Pesa balance.
     */
    private val balanceRegex =
        Regex(
            "(?i)new\\s+M-PESA\\s+balance\\s+is\\s+(?:Ksh|KES)\\s*([\\d,]+(?:\\.\\d{1,2})?)"
        )

    /*
     * Exact transaction cost.
     */
    private val transactionCostRegex =
        Regex(
            "(?i)transaction\\s+cost,?\\s*(?:Ksh|KES)\\s*([\\d,]+(?:\\.\\d{1,2})?)"
        )

    /*
     * Supports both:
     * 0795***148
     * 254795***148
     * and normal complete Kenyan numbers.
     */
    private val phoneRegex =
        Regex(
            "\\b(?:254|0)7\\d{2}(?:\\*{3}\\d{3}|\\d{6})\\b"
        )

    /*
     * Example:
     * on 8/10/26 at 7:37 AM
     * on 7/10/26 at 10:18 PM
     */
    private val dateTimeRegex =
        Regex(
            "(?i)on\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+" +
                    "at\\s+(\\d{1,2}:\\d{2}\\s*[AP]M)"
        )

    /*
     * Sent to:
     *
     * sent to James Omunani on...
     * sent to SAFARICOM POSTPAID BUNDLES for account...
     * sent to ZIIDI on...
     */
    private val sentToRegex =
        Regex(
            "(?is)sent\\s+to\\s+(.+?)" +
                    "(?=\\s+for\\s+account\\s+|" +
                    "\\s+on\\s+\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+at\\s+" +
                    "|\\s+new\\s+M-PESA\\s+balance" +
                    "|\\s+transaction\\s+cost" +
                    "|\\.|$)"
        )

    /*
     * Received from:
     *
     * received Ksh100.00 from ROBERT OTSIENO 0795***148 on...
     */
    private val receivedFromRegex =
        Regex(
            "(?is)received\\s+" +
                    "(?:Ksh|KES)\\s*[\\d,]+(?:\\.\\d{1,2})?" +
                    "\\s+from\\s+(.+?)" +
                    "(?=\\s+on\\s+\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+at\\s+" +
                    "|\\s+new\\s+M-PESA\\s+balance" +
                    "|\\.|$)"
        )

    /*
     * Account reference:
     *
     * for account Data Daily
     * for account 40076178
     */
    private val accountRegex =
        Regex(
            "(?is)for\\s+account\\s+(.+?)" +
                    "(?=\\s+on\\s+\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+at\\s+" +
                    "|\\s+new\\s+M-PESA\\s+balance" +
                    "|\\s+transaction\\s+cost" +
                    "|\\.|$)"
        )

    fun parse(
        sender: String,
        body: String,
        receivedAt: Long
    ): ParsedTransaction? {

        if (!isLikelyMpesa(sender, body)) {
            return null
        }

        val code =
            codeRegex
                .find(body)
                ?.groupValues
                ?.get(1)

        val amount =
            amountRegex
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toMoneyLong()

        /*
         * We cannot create a transaction without an amount.
         */
        if (amount == null) {
            return null
        }

        val balanceAfter =
            balanceRegex
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toMoneyLong()

        val transactionCost =
            transactionCostRegex
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toMoneyLong()

        val transactionType =
            detectTransactionType(body)

        val counterparty =
            extractCounterparty(body)

        val phoneNumber =
            phoneRegex
                .find(body)
                ?.value

        val accountReference =
            extractAccountReference(body)

        val provider =
            extractProvider(counterparty)

        val timestamp =
            extractTransactionTimestamp(
                body = body,
                fallback = receivedAt
            )

        val confidence =
            calculateConfidence(
                code = code,
                amount = amount,
                balanceAfter = balanceAfter,
                transactionCost = transactionCost,
                timestamp = timestamp,
                receivedAt = receivedAt,
                transactionType = transactionType,
                counterparty = counterparty
            )

        return ParsedTransaction(
            transactionCode = code,
            timestamp = timestamp,
            amount = amount,
            balanceAfter = balanceAfter,
            transactionType = transactionType,
            counterparty = counterparty,
            phoneNumber = phoneNumber,
            accountReference = accountReference,
            transactionCost = transactionCost,
            provider = provider,
            confidence = confidence
        )
    }

    private fun detectTransactionType(
        body: String
    ): TransactionType {

        return when {

            /*
             * Reversal must be checked first.
             */
            body.contains(
                "reversal",
                ignoreCase = true
            ) ->
                TransactionType.REVERSAL

            /*
             * Bank transfer.
             */
            body.contains(
                "co-operative bank",
                ignoreCase = true
            ) ||
                    body.contains(
                        "cooperative bank",
                        ignoreCase = true
                    ) ||
                    body.contains(
                        "bank transfer",
                        ignoreCase = true
                    ) ->
                TransactionType.BANK_TRANSFER

            /*
             * Paybill.
             */
            body.contains(
                "pay bill",
                ignoreCase = true
            ) ||
                    body.contains(
                        "paybill",
                        ignoreCase = true
                    ) ->
                TransactionType.PAYBILL

            /*
             * ZIIDI messages are still outgoing M-Pesa
             * transactions at this level.
             *
             * Investment classification will be handled
             * separately once we understand the actual
             * ZIIDI deposit/withdrawal/interest SMS formats.
             */
            body.contains(
                "sent to",
                ignoreCase = true
            ) ->
                TransactionType.SEND_MONEY

            /*
             * Incoming money.
             */
            body.contains(
                "you have received",
                ignoreCase = true
            ) ||
                    body.contains(
                        "received",
                        ignoreCase = true
                    ) ->
                TransactionType.RECEIVE_MONEY

            /*
             * Goods or merchant payment.
             */
            body.contains(
                "paid to",
                ignoreCase = true
            ) ->
                TransactionType.BUY_GOODS

            /*
             * Withdrawal.
             */
            body.contains(
                "withdraw",
                ignoreCase = true
            ) ->
                TransactionType.WITHDRAW

            /*
             * Deposit.
             */
            body.contains(
                "deposit",
                ignoreCase = true
            ) ->
                TransactionType.DEPOSIT

            /*
             * Airtime.
             */
            body.contains(
                "airtime",
                ignoreCase = true
            ) ->
                TransactionType.AIRTIME_PURCHASE

            else ->
                TransactionType.UNKNOWN
        }
    }

    private fun extractCounterparty(
        body: String
    ): String? {

        val sentTo =
            sentToRegex
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.cleanText()

        if (!sentTo.isNullOrBlank()) {
            return sentTo
        }

        val receivedFrom =
            receivedFromRegex
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.cleanText()

        if (!receivedFrom.isNullOrBlank()) {
            /*
             * Remove the phone number from the person's
             * name so that counterparty remains clean.
             */
            return receivedFrom
                .replace(
                    phoneRegex,
                    ""
                )
                .cleanText()
        }

        return null
    }

    private fun extractAccountReference(
        body: String
    ): String? {

        return accountRegex
            .find(body)
            ?.groupValues
            ?.get(1)
            ?.cleanText()
            ?.takeIf {
                it.isNotBlank()
            }
    }

    /*
     * Provider is deliberately conservative.
     *
     * We only assign a provider when the counterparty
     * clearly represents a known service/provider.
     *
     * Normal person-to-person transfers return null.
     */
    private fun extractProvider(
        counterparty: String?
    ): String? {

        if (counterparty.isNullOrBlank()) {
            return null
        }

        val value =
            counterparty
                .trim()
                .uppercase(Locale.ROOT)

        return when {

            value.startsWith("ZIIDI") ->
                "ZIIDI"

            value.contains(
                "SAFARICOM POSTPAID"
            ) ->
                "SAFARICOM"

            value.contains(
                "CO-OPERATIVE BANK"
            ) ||
                    value.contains(
                        "COOPERATIVE BANK"
                    ) ->
                "CO-OPERATIVE BANK"

            else ->
                null
        }
    }

    private fun extractTransactionTimestamp(
        body: String,
        fallback: Long
    ): Long {

        val match =
            dateTimeRegex.find(body)
                ?: return fallback

        val date =
            match.groupValues[1]

        val time =
            match.groupValues[2]
                .replace(
                    Regex("\\s+"),
                    " "
                )
                .trim()
                .uppercase(Locale.ROOT)

        val dateTime =
            "$date $time"

        val formats =
            listOf(
                "d/M/yy h:mm a",
                "dd/MM/yy h:mm a",
                "d/M/yyyy h:mm a",
                "dd/MM/yyyy h:mm a"
            )

        for (format in formats) {

            try {

                val formatter =
                    SimpleDateFormat(
                        format,
                        Locale.ENGLISH
                    )

                formatter.isLenient = false

                val parsedDate =
                    formatter.parse(dateTime)

                if (parsedDate != null) {
                    return parsedDate.time
                }

            } catch (_: Exception) {
                // Try the next format.
            }
        }

        return fallback
    }

    private fun calculateConfidence(
        code: String?,
        amount: Long?,
        balanceAfter: Long?,
        transactionCost: Long?,
        timestamp: Long,
        receivedAt: Long,
        transactionType: TransactionType,
        counterparty: String?
    ): Double {

        var score = 0.0

        if (!code.isNullOrBlank()) {
            score += 0.20
        }

        if (amount != null) {
            score += 0.20
        }

        if (balanceAfter != null) {
            score += 0.15
        }

        if (transactionCost != null) {
            score += 0.10
        }

        if (timestamp != receivedAt) {
            score += 0.10
        }

        if (transactionType != TransactionType.UNKNOWN) {
            score += 0.15
        }

        if (!counterparty.isNullOrBlank()) {
            score += 0.10
        }

        return score.coerceIn(
            0.0,
            1.0
        )
    }

    private fun isLikelyMpesa(
        sender: String,
        body: String
    ): Boolean {

        return sender.equals(
            "MPESA",
            ignoreCase = true
        ) ||
                body.contains(
                    "M-PESA",
                    ignoreCase = true
                ) ||
                body.contains(
                    "M-PESA balance",
                    ignoreCase = true
                ) ||
                body.contains(
                    "M-PESA balance is",
                    ignoreCase = true
                )
    }

    private fun String.cleanText(): String {

        return replace(
            Regex("\\s+"),
            " "
        )
            .trim()
            .trimEnd('.')
    }

    /*
     * Current database stores monetary values as Long.
     *
     * For now this converts KES 70.00 -> 70.
     * We will address exact cent-level storage before
     * finalising the financial analytics layer.
     */
    private fun String.toMoneyLong(): Long? =
    runCatching {
        replace(",", "")
            .toBigDecimalOrNull()
            ?.movePointRight(2)
            ?.setScale(0, RoundingMode.HALF_UP)
            ?.longValueExact()
    }.getOrNull()
}