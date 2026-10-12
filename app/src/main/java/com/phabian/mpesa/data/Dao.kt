
package com.phabian.mpesa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class ZidiiTransactionRow(
    val transactionCode: String?,
    val timestamp: Long,
    val amount: Long,
    val smsBody: String
)

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM transactions
            WHERE transactionCode = :code
        )
        """
    )
    suspend fun exists(code: String): Boolean

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE timestamp >= :start
          AND timestamp < :end
        ORDER BY timestamp DESC
        """
    )
    fun observeTransactionsBetween(
        start: Long,
        end: Long
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        WITH zidii_sms AS (
            SELECT
                SUBSTR(body, 1, 10) AS transactionCode,
                MAX(
                    CASE
                        WHEN UPPER(body) LIKE '%SENT TO ZIIDI%'
                        THEN body
                        ELSE ''
                    END
                ) AS depositBody,
                MAX(
                    CASE
                        WHEN UPPER(body) LIKE '%RECEIVED KSH%FROM ZIIDI%'
                        THEN body
                        ELSE ''
                    END
                ) AS withdrawalBody
            FROM raw_sms
            WHERE UPPER(body) LIKE '%ZIIDI%'
            GROUP BY SUBSTR(body, 1, 10)
        )
        SELECT
            t.transactionCode AS transactionCode,
            t.timestamp AS timestamp,
            t.amount AS amount,
            CASE
                WHEN z.depositBody <> ''
                THEN z.depositBody
                ELSE z.withdrawalBody
            END AS smsBody
        FROM transactions AS t
        INNER JOIN zidii_sms AS z
            ON z.transactionCode = t.transactionCode
        WHERE z.depositBody <> ''
           OR z.withdrawalBody <> ''
        ORDER BY t.timestamp DESC
        """
    )
    fun observeZidiiTransactions(): Flow<List<ZidiiTransactionRow>>
}


@Dao
interface RawSmsDao {

    @Insert
    suspend fun insert(message: RawSmsEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM raw_sms
            WHERE smsId = :smsId
        )
        """
    )
    suspend fun existsBySmsId(
        smsId: String
    ): Boolean

    @Query(
        """
        SELECT *
        FROM raw_sms
        WHERE id = :rawMessageId
        LIMIT 1
        """
    )
    suspend fun getById(
        rawMessageId: Long
    ): RawSmsEntity?
}

