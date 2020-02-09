package de.heoegbr.bgproxy;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.heoegbr.bgproxy.ble.WorkScheduleHelper;
import de.heoegbr.bgproxy.db.BgReading;
import de.heoegbr.bgproxy.ui.PreferencesFragment;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MAIN_ACTIVITY";

    private MainViewModel mMainViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // load setting fragment
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.bolus_calculator_fragment_container, new PreferencesFragment())
                .commit();

        final GraphView graph = findViewById(R.id.bg_graph);
        // enable x axis interaction
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(-120);
        graph.getViewport().setMaxX(5);
        graph.getViewport().setScalable(true);

        // get view model
        mMainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mMainViewModel.getAllReadings().observe(this, new Observer<List<BgReading>>() {
            @Override
            public void onChanged(List<BgReading> bgReadings) {
                Log.d(TAG, "Got live data update.");
                updateGraph(graph, bgReadings);

                //TODO find a more suitable place to schedule this
                WorkScheduleHelper.scheduleBtBroadcast(getApplicationContext());
            }
        });
    }

    private void updateGraph(GraphView graph, List<BgReading> bgReadings) {
        if (bgReadings == null || bgReadings.isEmpty()) return;

        // calculate graph data
        List<DataPoint> convertedData = new ArrayList<>();
        long now = new Date().getTime();
        for (int i = bgReadings.size() - 1; i >= 0; i--) {
            BgReading reading = bgReadings.get(i);
            int offset = (int) -1* Math.round((now-reading.date)/60000);
            convertedData.add(new DataPoint(offset, reading.value));
        }
        LineGraphSeries<DataPoint> bgSeries = new LineGraphSeries<>(
                convertedData.toArray(new DataPoint[0]));

        // styling
        bgSeries.setDrawDataPoints(true);
        BgReading item = bgReadings.get(0);
        if (item.value < 60 || item.value > 300) {
            bgSeries.setColor(Color.rgb(204, 0, 0));
        } else if (item.value > 180) {
            bgSeries.setColor(Color.rgb(255, 183, 0));
        } else {
            bgSeries.setColor(Color.rgb(2, 173, 48));
        }


        graph.getSeries().clear();
        graph.addSeries(bgSeries);
    }
}
