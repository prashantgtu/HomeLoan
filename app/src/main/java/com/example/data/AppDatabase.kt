package com.example.data

import android.content.Context
import androidx.room.*
import com.example.LoanInput
import com.example.Prepayment
import com.example.RateChange
import com.example.RepaymentStrategy
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val principal: Double,
    val annualInterestRate: Double,
    val tenureMonths: Int,
    val strategy: RepaymentStrategy,
    val extraMonthlyPayment: Double,
    val startDateMs: Long,
    val prepayments: List<Prepayment>,
    val rateChanges: List<RateChange>,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans ORDER BY timestamp DESC")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity): Long

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteLoanById(id: Long)
}

class LoanConverters {
    private val moshi = Moshi.Builder().build()
    private val prepayListType = Types.newParameterizedType(List::class.java, Prepayment::class.java)
    private val rateListType = Types.newParameterizedType(List::class.java, RateChange::class.java)
    private val prepayAdapter = moshi.adapter<List<Prepayment>>(prepayListType)
    private val rateAdapter = moshi.adapter<List<RateChange>>(rateListType)

    @TypeConverter
    fun fromRepaymentStrategy(strategy: RepaymentStrategy): String = strategy.name

    @TypeConverter
    fun toRepaymentStrategy(name: String): RepaymentStrategy = RepaymentStrategy.valueOf(name)

    @TypeConverter
    fun fromPrepayments(list: List<Prepayment>?): String? {
        return if (list == null) null else prepayAdapter.toJson(list)
    }

    @TypeConverter
    fun toPrepayments(json: String?): List<Prepayment>? {
        return if (json == null) null else prepayAdapter.fromJson(json)
    }

    @TypeConverter
    fun fromRateChanges(list: List<RateChange>?): String? {
        return if (list == null) null else rateAdapter.toJson(list)
    }

    @TypeConverter
    fun toRateChanges(json: String?): List<RateChange>? {
        return if (json == null) null else rateAdapter.fromJson(json)
    }
}

@Database(entities = [LoanEntity::class], version = 2, exportSchema = false)
@TypeConverters(LoanConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loan_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
