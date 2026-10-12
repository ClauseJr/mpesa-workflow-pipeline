package com.phabian.mpesa.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.phabian.mpesa.data.*
import com.phabian.mpesa.parser.MpesaParser
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MpesaSmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TransactionRepository

    @Inject
    lateinit var rawSmsDao: RawSmsDao

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action !=
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {
            return
        }

        val pending =
            goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val parser =
                    MpesaParser()

                Telephony.Sms.Intents
                    .getMessagesFromIntent(intent)
                    .forEach { sms ->

                        val sender =
                            sms.originatingAddress ?: ""

                        val body =
                            sms.messageBody ?: ""

                        val timestamp =
                            sms.timestampMillis

                        val parsed =
                            parser.parse(
                                sender = sender,
                                body = body,
                                receivedAt = timestamp
                            )
                                ?: return@forEach

                        val rawId =
                            rawSmsDao.insert(
                                RawSmsEntity(
                                    sender = sender,
                                    body = body,
                                    receivedAt = timestamp,
                                    smsId = null,
                                    parsed = true,
                                    parseStatus =
                                        ParseStatus.SUCCESS
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

                                category = null,

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

            } finally {

                pending.finish()
            }
        }
    }
}