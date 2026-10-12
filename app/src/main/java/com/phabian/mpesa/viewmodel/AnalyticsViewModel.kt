package com.phabian.mpesa.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phabian.mpesa.data.AnalyticsPeriod
import com.phabian.mpesa.data.AnalyticsRepository
import com.phabian.mpesa.data.AnalyticsSummary
import com.phabian.mpesa.data.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _selectedPeriod =
        MutableStateFlow(
            createCurrentMonthPeriod()
        )

    val selectedPeriod: StateFlow<AnalyticsPeriod> =
        _selectedPeriod.asStateFlow()

    val transactions: StateFlow<List<TransactionEntity>> =
        _selectedPeriod
            .flatMapLatest { period ->
                analyticsRepository.observePeriodTransactions(
                    start = period.start,
                    end = period.end
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(
                    5_000
                ),
                initialValue = emptyList()
            )

    val summary: StateFlow<AnalyticsSummary?> =
        _selectedPeriod
            .flatMapLatest { period ->
                analyticsRepository.observePeriodSummary(
                    start = period.start,
                    end = period.end
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(
                    5_000
                ),
                initialValue = null
            )

    fun selectYear(year: Int) {

        val calendar =
            Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            Calendar.JANUARY,
            1,
            0,
            0,
            0
        )

        val start =
            calendar.timeInMillis

        calendar.add(
            Calendar.YEAR,
            1
        )

        val end =
            calendar.timeInMillis

        _selectedPeriod.value =
            AnalyticsPeriod(
                start = start,
                end = end,
                label = year.toString()
            )
    }

    fun selectMonth(
        year: Int,
        month: Int
    ) {

        val calendar =
            Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            month,
            1,
            0,
            0,
            0
        )

        val start =
            calendar.timeInMillis

        calendar.add(
            Calendar.MONTH,
            1
        )

        val end =
            calendar.timeInMillis

        val label =
            calendar.apply {
                add(
                    Calendar.MONTH,
                    -1
                )
            }.let {
                java.text.SimpleDateFormat(
                    "MMMM yyyy",
                    java.util.Locale.ENGLISH
                ).format(it.time)
            }

        _selectedPeriod.value =
            AnalyticsPeriod(
                start = start,
                end = end,
                label = label
            )
    }

    fun selectDay(
        year: Int,
        month: Int,
        day: Int
    ) {

        val calendar =
            Calendar.getInstance()

        calendar.clear()

        calendar.set(
            year,
            month,
            day,
            0,
            0,
            0
        )

        val start =
            calendar.timeInMillis

        calendar.add(
            Calendar.DAY_OF_MONTH,
            1
        )

        val end =
            calendar.timeInMillis

        val label =
            java.text.SimpleDateFormat(
                "dd MMMM yyyy",
                java.util.Locale.ENGLISH
            ).format(
                start
                    .let {
                        java.util.Date(it)
                    }
            )

        _selectedPeriod.value =
            AnalyticsPeriod(
                start = start,
                end = end,
                label = label
            )
    }

    fun selectCurrentMonth() {

        _selectedPeriod.value =
            createCurrentMonthPeriod()
    }

    private fun createCurrentMonthPeriod():
        AnalyticsPeriod {

        val calendar =
            Calendar.getInstance()

        val year =
            calendar.get(Calendar.YEAR)

        val month =
            calendar.get(Calendar.MONTH)

        calendar.clear()

        calendar.set(
            year,
            month,
            1,
            0,
            0,
            0
        )

        val start =
            calendar.timeInMillis

        calendar.add(
            Calendar.MONTH,
            1
        )

        val end =
            calendar.timeInMillis

        val label =
            java.text.SimpleDateFormat(
                "MMMM yyyy",
                java.util.Locale.ENGLISH
            ).format(
                java.util.Date(start)
            )

        return AnalyticsPeriod(
            start = start,
            end = end,
            label = label
        )
    }
}