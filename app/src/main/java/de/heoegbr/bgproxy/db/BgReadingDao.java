package de.heoegbr.bgproxy.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface BgReadingDao {
    @Query("SELECT * FROM bgreadings ORDER BY date DESC LIMIT 24")
    LiveData<List<BgReading>> getLiveReadings();

    @Query("SELECT * FROM bgreadings ORDER BY date DESC LIMIT 24") // 2 hours
    List<BgReading> getStaticReadings();

    @Insert
    void insert(BgReading bgReading);

    @Query("DELETE FROM bgreadings")
    void deleteAll();
}
