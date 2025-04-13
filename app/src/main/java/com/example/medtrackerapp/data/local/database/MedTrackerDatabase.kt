package com.example.medtrackerapp.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Base de datos principal de la aplicación
 */
@Database(
    entities = [ExamEntity::class, ParameterEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class MedTrackerDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
    abstract fun parameterDao(): ParameterDao
    abstract fun categoryDao(): CategoryDao
}

/**
 * Conversor para fechas
 */
class DateConverter {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

/**
 * Entidad para los exámenes médicos
 */
@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val id: String,
    val date: Date,
    val laboratoryName: String,
    val doctorName: String?,
    val pdfFilePath: String
)

/**
 * Entidad para las categorías
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: String,
    val name: String
)

/**
 * Entidad para los parámetros
 */
@Entity(tableName = "parameters")
data class ParameterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val examId: String,
    val categoryId: Long,
    val name: String,
    val value: String,
    val unit: String,
    val referenceRange: String,
    @ColumnInfo(name = "status") val statusValue: Int // 0: Normal, 1: Watch, 2: Attention, 3: Undefined
)

/**
 * DAO para exámenes
 */
@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date DESC")
    fun getExams(): Flow<List<ExamEntity>>
    
    @Query("SELECT * FROM exams WHERE id = :examId")
    suspend fun getExamById(examId: String): ExamEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)
    
    @Query("DELETE FROM exams WHERE id = :examId")
    suspend fun deleteExam(examId: String)
}

/**
 * DAO para categorías
 */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE examId = :examId")
    suspend fun getCategoriesByExamId(examId: String): List<CategoryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long
    
    @Query("DELETE FROM categories WHERE examId = :examId")
    suspend fun deleteByExamId(examId: String)
}

/**
 * DAO para parámetros
 */
@Dao
interface ParameterDao {
    @Query("SELECT * FROM parameters WHERE examId = :examId")
    suspend fun getParametersByExamId(examId: String): List<ParameterEntity>
    
    @Query("SELECT * FROM parameters WHERE categoryId = :categoryId")
    suspend fun getParametersByCategoryId(categoryId: Long): List<ParameterEntity>
    
    @Query("""
        SELECT p.* FROM parameters p
        JOIN exams e ON p.examId = e.id
        WHERE p.name LIKE :paramName
        ORDER BY e.date
    """)
    suspend fun getParameterHistory(paramName: String): List<ParameterEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParameter(parameter: ParameterEntity)
    
    @Query("DELETE FROM parameters WHERE examId = :examId")
    suspend fun deleteByExamId(examId: String)
} 