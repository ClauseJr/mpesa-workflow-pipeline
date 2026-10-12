package com.phabian.mpesa

import com.phabian.mpesa.data.TransactionType
import com.phabian.mpesa.parser.MpesaParser
import org.junit.Assert.*
import org.junit.Test

class MpesaParserTest {

    @Test
    fun detects_mpesа_message() {
        val parser = MpesaParser()

        val result = parser.parse(
            "MPESA",
            "ABC1234567 Confirmed. Ksh1,500.00 sent to JOHN DOE.",
            System.currentTimeMillis()
        )

        assertNotNull(result)
        assertEquals(1500L, result!!.amount)
        assertEquals(TransactionType.SEND_MONEY, result.transactionType)
    }
}
