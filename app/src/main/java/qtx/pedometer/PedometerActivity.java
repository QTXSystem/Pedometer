package qtx.pedometer;

/**
 * Created by qtxdev on 6/18/2016.
 */
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PedometerActivity extends Activity implements SensorEventListener, StepListener {
    private static final float THRESHOLD = (float)11.5;
    private static final int SAMPLE_SIZE = 5;
    private SimpleStepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps = 0;
    private TextView heightText;
    private TextView stepText;
    private TextView distanceFeetText;
    private TextView distanceMilesText;
    private TextView deltaText;
    private TextView msgText;
    private Button  start;
    private Button  stop;
    private Spinner spinner;
    Chronometer myChronometer;
    private boolean startMeasure = false;
    private float mAccelCurrent = 0;
    private float mAccelLast = 0;
    private long lastMotionTime = 0;
    private List<sensorData> sensorList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedometer);
    //    TextView myView = (TextView) findViewById(R.layout.activity_pedometer);

        RelativeLayout rl = (RelativeLayout)findViewById(R.id.RelativeLayout1);
      //  rl.setBackgroundResource(R.drawable.bridge);

        spinner = (Spinner) findViewById(R.id.spinner);

        stepText = (TextView) findViewById(R.id.steps);
        distanceFeetText = (TextView) findViewById(R.id.distance_feet);
        distanceMilesText = (TextView) findViewById(R.id.distance_miles);
        msgText = (TextView) findViewById(R.id.msg);
        deltaText = (TextView) findViewById(R.id.delta);
        myChronometer = (Chronometer)findViewById(R.id.chronometer);

        start = (Button)findViewById(R.id.start_button);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numSteps = 0;
                startMeasure = true;
                msgText.setText("Started");
                stepText.setText(TEXT_NUM_STEPS+ 0);
                distanceMilesText.setText("Distance (miles): 0");
                distanceFeetText.setText("Distance (feet): 0");
                myChronometer.setBase(SystemClock.elapsedRealtime());
                myChronometer.start();
            }
        });

        stop = (Button)findViewById(R.id.stop_button);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startMeasure = false;
                msgText.setText("Stopped");
                myChronometer.stop();
            }
        });
        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(this);

        lastMotionTime = -0xffffff;
    }

    @Override
    public void onResume() {
        super.onResume();
        numSteps = 0;
        stepText.setText(TEXT_NUM_STEPS + numSteps);
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
            if (!startMeasure)
                return;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float)Math.sqrt(x*x + y*y + z*z);

            sensorData data = new sensorData();
            data.x = x;
            data.y = y;
            data.z = z;
            data.timestamp = event.timestamp;
            data.accel = mAccelCurrent;

            if (sensorList.size() ==SAMPLE_SIZE)
                sensorList.remove(0);
            sensorList.add(data);
            boolean motionFound = true;
            String motion = "";
            if (sensorList.size() ==SAMPLE_SIZE){
                for (int i=0; i<SAMPLE_SIZE; i++){
                    if (sensorList.get(i).accel < THRESHOLD) {
                        motionFound = false;
                        break;
                    }
                   // motion += String.format("%.5f,", sensorList.get(i).accel);
                    motion = "raising/sitdown";
                    lastMotionTime = System.currentTimeMillis();
                }
            }
            else
                motionFound = false;

            if (motionFound == true) {
                int n = motion.indexOf("raising/sitdown");
                if (n < 0)
                    motion += " raising/sitdown";
                deltaText.setText(motion);
            }

        }
    }

    @Override
    public void step(long timeNs) {
        if (!startMeasure)
            return;

        numSteps++;
        stepText.setText(TEXT_NUM_STEPS + numSteps);
        String str = String.valueOf(spinner.getSelectedItem());
        double height = Double.parseDouble(str);
        double footLength = height * 0.414;
        double feet = numSteps * footLength;
        double miles = feet /5280;
        String milesStr = String.format("%.5f", miles);
        String feetStr = String.format("%.5f", feet);

        distanceFeetText.setText("Distance (feet): " + feetStr);
        distanceMilesText.setText("Distance (miles): " + milesStr);


            String text =deltaText.getText().toString();
            int n = text.indexOf("walking");
            if (n < 0)
                text += " walking";
            // long curr = System.currentTimeMillis();
            // if ((curr - lastMotionTime) >= 500)
            text = text.replace("raising/sitdown", "");
            deltaText.setText(text);


    }
    public class sensorData{
         public float x;
        public float y;
        public float z;
        public long timestamp;
        public float accel;
    }
}

