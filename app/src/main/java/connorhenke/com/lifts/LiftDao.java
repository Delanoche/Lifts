package connorhenke.com.lifts;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import connorhenke.com.lifts.viewmodels.Lift;
import connorhenke.com.lifts.viewmodels.Set;
import io.reactivex.Flowable;
import io.reactivex.Single;

@Dao
public interface LiftDao {

    @Query("SELECT * FROM lift")
    Flowable<List<Lift>> getAllLifts();

    @Query("SELECT COUNT(lid) FROM lift")
    Flowable<Long> getLiftCount();

    @Query("SELECT * FROM lift WHERE lid = :lid LIMIT 1")
    Single<Lift> getLift(long lid);

    @Query("SELECT * FROM `set` WHERE lift_id = :lid ORDER BY date DESC")
    Flowable<List<Set>> getSets(long lid);

    @Query("SELECT * FROM lift WHERE name = :name")
    Single<List<Lift>> getLiftByName(String name);

    @Insert
    void insertAll(Lift... lifts);

    @Insert
    long insertWithResponse(Lift lift);
}
