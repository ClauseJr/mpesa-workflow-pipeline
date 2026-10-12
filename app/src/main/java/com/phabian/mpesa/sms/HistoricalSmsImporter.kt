package com.phabian.mpesa.sms

import android.content.Context
import android.provider.Telephony
import com.phabian.mpesa.data.*
import com.phabian.mpesa.parser.MpesaParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class HistoricalSmsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransactionRepository,
    private val rawSmsDao: RawSmsDao
) {

    private val parser = MpesaParser()

    suspend fun importAll() {

        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            null,
            null,
            "${Telephony.Sms.DATE} DESC"
        ) ?: return

        cursor.use {

            val idCol =
                it.getColumnIndexOrThrow(
                    Telephony.Sms._ID
                )

            val addressCol =
                it.getColumnIndexOrThrow(
                    Telephony.Sms.ADDRESS
                )

            val bodyCol =
                it.getColumnIndexOrThrow(
                    Telephony.Sms.BODY
                )

            val dateCol =
                it.getColumnIndexOrThrow(
                    Telephony.Sms.DATE
                )

            while (it.moveToNext()) {

                val smsId =
                    it.getString(idCol)

                // Prevent the same historical SMS
                // from being imported more than once.
                if (rawSmsDao.existsBySmsId(smsId)) {
                    continue
                }

                val sender =
                    it.getString(addressCol) ?: ""

                val body =
                    it.getString(bodyCol) ?: ""

                val receivedAt =
                    it.getLong(dateCol)

                val parsed =
                    parser.parse(
                        sender = sender,
                        body = body,
                        receivedAt = receivedAt
                    ) ?: continue

                val rawId =
                    rawSmsDao.insert(
                        RawSmsEntity(
                            sender = sender,
                            body = body,
                            receivedAt = receivedAt,
                            smsId = smsId,
                            parsed = true,
                            parseStatus = ParseStatus.SUCCESS
                        )
                    )

                repository.save(
                    TransactionEntity(
                        transactionCode =
                            parsed.transactionCode,

                        timestamp =
                            parsed.timestamp,

                        amount =
                            parsed.amount,

                        balanceAfter =
                            parsed.balanceAfter,

                        transactionType =
                            parsed.transactionType,

                        counterparty =
                            parsed.counterparty,

                        phoneNumber =
                            parsed.phoneNumber,

                        accountReference =
                            parsed.accountReference,

                        transactionCost =
                            parsed.transactionCost,

                        provider =
                            parsed.provider,

                        category =
                            null,

                        rawMessageId =
                            rawId,

                        parserVersion =
                            "0.2.0",

                        confidence =
                            parsed.confidence,

                        createdAt =
                            System.currentTimeMillis()
                    )
                )
            }
        }
    }
}