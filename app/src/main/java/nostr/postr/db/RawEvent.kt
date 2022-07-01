package nostr.postr.db

import android.content.Context
import androidx.room.*
import androidx.room.Dao
import kotlinx.coroutines.flow.Flow

@Entity
data class RawEvent(
    @PrimaryKey
    val id: String,
    val pubKey: String,
    val createdAt: Long,
    val kind: Int,
    val tags: List<List<String>>,
    val content: String,
    val sig: String
)

@Dao
interface Dao {
    @Insert
    suspend fun addItem(event: RawEvent)

    @Query("SELECT * FROM RawEvent")
    fun getList(): Flow<List<RawEvent>>

    @Query("DELETE FROM RawEvent WHERE id = :id")
    suspend fun deleteItem(id: String)
}

@Database(entities = [RawEvent::class], version = 1, exportSchema = false)
abstract class ListDatabase : RoomDatabase(){
    abstract fun Dao(): Dao

    companion object {
        @Volatile
        private var INSTANCE: ListDatabase? = null

        fun getDatabase(context: Context): ListDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        ListDatabase::class.java,
                        "list_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}