package de.heoegbr.bgproxy.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities={BgReading.class}, version = 1, exportSchema = false)
public abstract class BgReadingDatabase extends RoomDatabase {

    public abstract BgReadingDao bgReadingDao();

    private static volatile BgReadingDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 2;
    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static BgReadingDatabase getDatabase(final Context context){
        if (INSTANCE == null){
            synchronized (BgReadingDatabase.class){
                if (INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            BgReadingDatabase.class, "bgreading_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
