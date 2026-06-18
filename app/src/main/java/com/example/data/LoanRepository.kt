package com.example.data

import kotlinx.coroutines.flow.Flow

class LoanRepository(private val loanDao: LoanDao) {
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()

    suspend fun insertLoan(loan: LoanEntity): Long = loanDao.insertLoan(loan)

    suspend fun deleteLoanById(id: Long) = loanDao.deleteLoanById(id)
}
