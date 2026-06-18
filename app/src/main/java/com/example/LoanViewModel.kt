package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LoanEntity
import com.example.data.LoanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoanViewModel(private val repository: LoanRepository) : ViewModel() {

    val savedLoans: StateFlow<List<LoanEntity>> = repository.allLoans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _inputFlow = MutableStateFlow(
        LoanInput(
            principal = 5000000.0,
            annualInterestRate = 8.55,
            tenureMonths = 240,
            strategy = RepaymentStrategy.REDUCE_TENURE,
            extraMonthlyPayment = 0.0
        )
    )
    val inputFlow: StateFlow<LoanInput> = _inputFlow.asStateFlow()

    private val _prepayments = MutableStateFlow<List<Prepayment>>(emptyList())
    val prepayments = _prepayments.asStateFlow()

    private val _rateChanges = MutableStateFlow<List<RateChange>>(emptyList())
    val rateChanges = _rateChanges.asStateFlow()

    private val _summary = MutableStateFlow<LoanSummary?>(null)
    val summary = _summary.asStateFlow()

    init {
        calculate()
    }

    fun updateInput(update: (LoanInput) -> LoanInput) {
        _inputFlow.update(update)
        calculate()
    }

    fun addPrepayment(month: Int, amount: Double, strategy: RepaymentStrategy = RepaymentStrategy.REDUCE_TENURE) {
        _prepayments.update { current ->
            val list = current.toMutableList()
            list.add(Prepayment(month, amount, strategy))
            list.sortedBy { it.month }
        }
        calculate()
    }

    fun removePrepayment(prepayment: Prepayment) {
        _prepayments.update { current ->
            current.filterNot { it == prepayment }
        }
        calculate()
    }

    fun addRateChange(month: Int, rate: Double) {
        _rateChanges.update { current ->
            val list = current.toMutableList()
            list.removeAll { it.month == month }
            list.add(RateChange(month, rate))
            list.sortedBy { it.month }
        }
        calculate()
    }

    fun removeRateChange(rateChange: RateChange) {
        _rateChanges.update { current ->
            current.filterNot { it == rateChange }
        }
        calculate()
    }

    private fun calculate() {
        _summary.value = LoanMath.generateSchedule(
            input = _inputFlow.value,
            prepayments = _prepayments.value,
            rateChanges = _rateChanges.value
        )
    }

    fun resetToDefault() {
        _inputFlow.value = LoanInput(
            principal = 5000000.0,
            annualInterestRate = 8.55,
            tenureMonths = 240,
            strategy = RepaymentStrategy.REDUCE_TENURE,
            extraMonthlyPayment = 0.0
        )
        _prepayments.value = emptyList()
        _rateChanges.value = emptyList()
        calculate()
    }

    fun saveCurrentLoan(name: String) {
        viewModelScope.launch {
            repository.insertLoan(
                LoanEntity(
                    name = name,
                    principal = _inputFlow.value.principal,
                    annualInterestRate = _inputFlow.value.annualInterestRate,
                    tenureMonths = _inputFlow.value.tenureMonths,
                    strategy = _inputFlow.value.strategy,
                    extraMonthlyPayment = _inputFlow.value.extraMonthlyPayment,
                    startDateMs = _inputFlow.value.startDateMs,
                    prepayments = _prepayments.value,
                    rateChanges = _rateChanges.value
                )
            )
        }
    }

    fun loadLoan(loan: LoanEntity) {
        _inputFlow.value = LoanInput(
            principal = loan.principal,
            annualInterestRate = loan.annualInterestRate,
            tenureMonths = loan.tenureMonths,
            strategy = loan.strategy,
            extraMonthlyPayment = loan.extraMonthlyPayment,
            startDateMs = loan.startDateMs
        )
        _prepayments.value = loan.prepayments
        _rateChanges.value = loan.rateChanges
        calculate()
    }

    fun deleteLoan(id: Long) {
        viewModelScope.launch { repository.deleteLoanById(id) }
    }
}

class LoanViewModelFactory(private val repository: LoanRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoanViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
