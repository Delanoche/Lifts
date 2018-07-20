package connorhenke.com.lifts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;

import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding2.support.v7.widget.RxRecyclerViewAdapter;
import com.jakewharton.rxbinding2.widget.RxAdapterView;
import com.xwray.groupie.Group;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Section;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import connorhenke.com.lifts.viewmodels.Lift;
import connorhenke.com.lifts.viewmodels.Set;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import jp.wasabeef.recyclerview.animators.FadeInUpAnimator;

import javax.inject.Inject;
import javax.inject.Named;

public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    @Inject AppDatabase db;
    @Inject @Named("io") Scheduler io;
    @Inject @Named("main") Scheduler main;

    private RecyclerView recyclerView;
    private List<Lift> lifts;
    private Section liftGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((LiftApplication) getApplication()).getComponent().inject(this);

        recyclerView = findViewById(R.id.main_recycler_view);
        final GroupAdapter adapter = new GroupAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new FadeInUpAnimator(new DecelerateInterpolator()));
        recyclerView.setAdapter(adapter);
        SearchItem searchItem = new SearchItem();
        searchItem.getObservable()
                .subscribeOn(main)
                .observeOn(io)
                .map(new Function<CharSequence, List<Lift>>() {
                    @Override
                    public List<Lift> apply(@NonNull CharSequence charSequence) throws Exception {
                        List<Lift> filtered = new ArrayList<Lift>();
                        for(Lift lift : lifts) {
                            if (lift.getName().toLowerCase().contains(charSequence)) {
                                filtered.add(lift);
                            }
                        }
                        return filtered;
                    }
                })
                .observeOn(main)
                .subscribe(new Consumer<List<Lift>>() {
                    @Override
                    public void accept(@NonNull List<Lift> filtered) throws Exception {
                        adapter.remove(liftGroup);
                        liftGroup = new Section();
                        for (Lift lift : filtered) {
                            LiftItem item = new LiftItem(lift);
                            item.getObservable()
                                    .observeOn(main)
                                    .subscribe(new Consumer<Object>() {
                                        @Override
                                        public void accept(@NonNull Object o) throws Exception {
                                            startActivity(new Intent(MainActivity.this, LiftActivity.class));
                                        }
                                    });
                            liftGroup.add(item);
                        }
                        adapter.add(liftGroup);
                    }
                });
        adapter.add(searchItem);

        lifts = new ArrayList<>();
        liftGroup = new Section();

        db.liftDao().getAllLifts()
                .subscribeOn(io)
                .map(new Function<List<Lift>, List<LiftItem>>() {
                    @Override
                    public List<LiftItem> apply(List<Lift> lifts) throws Exception {
                        List<LiftItem> items = new ArrayList<>(lifts.size());
                        for (Lift lift : lifts) {
                            LiftItem item = new LiftItem(lift);
                            item.getObservable()
                                    .subscribeOn(main)
                                    .observeOn(main)
                                    .subscribe(new Consumer<Long>() {
                                        @Override
                                        public void accept(@NonNull Long o) throws Exception {
                                            startActivity(LiftActivity.getIntent(MainActivity.this, o));
                                        }
                                    });
                            items.add(item);
                        }
                        return items;
                    }
                })
                .observeOn(main)
                .subscribe(new Consumer<List<LiftItem>>() {
                    @Override
                    public void accept(List<LiftItem> liftItems) throws Exception {
                        liftGroup.addAll(liftItems);
                    }
                });
        adapter.add(liftGroup);
//        performFileSearch();
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("text/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    readTextFromUri(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void readTextFromUri(Uri uri) throws IOException, ParseException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            if (lineNumber < 1) {
                lineNumber++;
                continue;
            }
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        reader.close();
        inputStream.close();
        loadFromString(stringBuilder.toString());
    }

    private void loadFromString(String data) throws ParseException {
        Disposable disposable = Observable.just(data)
                .subscribeOn(io)
                .map(new Function<String, Boolean>() {
                    @Override
                    public Boolean apply(String s) throws Exception {
                        String[] lines = s.split("\n");
                        Set[] sets = new Set[lines.length];
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];
                            String[] split = line.split(",");
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = dateFormat.parse(split[0]);
                            List<Lift> lifts = db.liftDao().getLiftByName(split[1]).blockingGet();
                            Lift lift;
                            if (lifts.size() < 1) {
                                lift = new Lift(split[1]);
                                long id = db.liftDao().insertWithResponse(lift);
                                lift.setLid(id);
                                Log.d("INSERTED LIFT", lift.getName() + " AT " + lift.getLid());
                            } else {
                                lift = lifts.get(0);
                            }
                            double weight = Double.parseDouble(split[3]);
                            int reps = Integer.parseInt(split[4]);
                            Set set = new Set();
                            set.setDate(date);
                            set.setLiftId(lift.getLid());
                            set.setReps(reps);
                            set.setWeight(weight);
                            sets[i] = set;
                        }
                        db.setDao().insertAll(sets);
                        return Boolean.TRUE;
                    }
                })
                .observeOn(main)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        Log.d("DONE", "IMPORTING");
                    }
                });
    }
}
