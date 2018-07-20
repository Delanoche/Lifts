package connorhenke.com.lifts;

import android.app.Application;
import android.arch.persistence.room.Room;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class MainModule {

    private LiftApplication application;

    public MainModule(LiftApplication application) {
        this.application = application;
    }

    @Provides
    @Singleton
    LiftApplication providesApplication() {
        return application;
    }

    @Provides
    @Singleton
    AppDatabase providesDatabase(LiftApplication application) {
        return Room.databaseBuilder(application, AppDatabase.class, "lifts").build();
    }

    @Provides
    @Singleton
    @Named("io")
    Scheduler providesIOScheduler() {
        return Schedulers.io();
    }

    @Provides
    @Singleton
    @Named("main")
    Scheduler providesMainScheduler() {
        return AndroidSchedulers.mainThread();
    }

}
