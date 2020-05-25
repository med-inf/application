package com.example.medapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity
data class UserLocation(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "square_id") val squareId: String?,
    @ColumnInfo(name = "timestamp") val timestamp: String?
)

@Dao
interface LocationDao {
    @Query("SELECT * FROM UserLocation")
    fun getAll(): List<UserLocation>

    @Insert
    fun insertAll(vararg locations: UserLocation)

    @Delete
    fun delete(location: UserLocation)
}

@Database(entities = arrayOf(UserLocation::class), version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}


//class WordViewModel(application: Application) : AndroidViewModel(application) {
//
//    private val repository: WordRepository
//    val allLocations: LiveData<List<UserLocation>>
//
//    init {
//        val wordsDao = WordRoomDatabase.getDatabase(application, viewModelScope).wordDao()
//        repository = WordRepository(wordsDao)
//        allWords = repository.allWords
//    }
//
//    /**
//     * Launching a new coroutine to insert the data in a non-blocking way
//     */
//    fun insert(word: Word) = viewModelScope.launch(Dispatchers.IO) {
//        repository.insert(word)
//    }
//}