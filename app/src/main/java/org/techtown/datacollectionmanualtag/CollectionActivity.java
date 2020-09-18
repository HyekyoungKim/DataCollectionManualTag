package org.techtown.datacollectionmanualtag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

public class CollectionActivity extends AppCompatActivity {

    boolean resetFlag;
    Button collectButton, showDataButton, clearButton, progressButton, finishButton;
    TextView contents;
    EditText posX, posY;

    /* For Database */
    String databaseName = "DATA_COLLECTION";
    public static String tableName = "CONVERTED_DATA";
    boolean databaseCreated = false;
    boolean tableCreated = false;

    public static SQLiteDatabase db;

    /* For Sensors */
    private SensorManager manager = null;

    private Sensor magnetometer = null;
    private SensorEventListener magListener;
    private double magX, magY, magZ;
    private boolean magSensorChanged = false;

    private Sensor gravity = null;
    private SensorEventListener gravListener;
    private double gravX, gravY, gravZ;
    private boolean gravSensorChanged = false;
    private double [] sensorData = new double [6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        Intent intent = getIntent();
        processIntent(intent);

        createDatabase();
        createTable();

        manager = (SensorManager) getSystemService(SENSOR_SERVICE);

        magnetometer = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        magListener = new MagnetometerListener();
        manager.registerListener(magListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

        gravity = manager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        gravListener = new GravityListener();
        manager.registerListener(gravListener, gravity, SensorManager.SENSOR_DELAY_NORMAL);

        collectButton = findViewById(R.id.buttonCollect);
        showDataButton = findViewById(R.id.buttonShowData);
        clearButton = findViewById(R.id.buttonClear);
        progressButton = findViewById(R.id.buttonProgress);
        finishButton = findViewById(R.id.buttonFinish);
        contents = findViewById(R.id.contents);
        posX = findViewById(R.id.x);
        posY = findViewById(R.id.y);
    }

    @Override
    protected void onDestroy () {
        super.onDestroy();
        manager.unregisterListener(magListener);
        manager.unregisterListener(gravListener);
    }

    /** Decide whether to reset or keep DB (<- user's choice) */
    private void processIntent (Intent intent) {
        resetFlag = intent.getExtras().getBoolean("reset");
    }

    /** Collect sensor data at user's current location */
    public void onClickCollect(View view) {
        String strX = posX.getText().toString().trim();
        String strY = posY.getText().toString().trim();
        if(strX.getBytes().length <= 0 || strY.getBytes().length <= 0) {
            Toast.makeText(getApplicationContext(),
                    "Enter the current position", Toast.LENGTH_SHORT).show();
        } else {
            saveSensorData(Float.parseFloat(strX), Float.parseFloat(strY));
            posX.setText(null);
            posY.setText(null);
        }
    }

    /** Show data collected so far */
    public void onClickShowData(View view) {
        Cursor c = db.rawQuery(
                "select pos_x, pos_y, vertical, horizontal, magnitude " +
                        "from " + tableName, null);
        int recordCount = c.getCount();
        Log.d("Log", "cursor count : " + recordCount + "\n");
        contents.setText("");
        for (int i = 0; i < recordCount; i++) {
            c.moveToNext();
            double _posX = c.getDouble(0);
            double _posY = c.getDouble(1);
            double _vertical = c.getDouble(2);
            double _horizontal = c.getDouble(3);
            double _magnitude = c.getDouble(4);

            contents.append("\nRecord #" + i + "\n"
                    + "Location (x,y): (" + String.format(Locale.KOREA,"%.2f", _posX)
                    + ", " + String.format(Locale.KOREA,"%.2f", _posY) + ")" + "\n"
                    + "< Converted Magnetic Field >\n"
                    + "Vertical: " + String.format(Locale.KOREA,"%.2f", _vertical) + ", "
                    + "Horizontal: " + String.format(Locale.KOREA,"%.2f", _horizontal) + ", "
                    + "Magnitude: " + String.format(Locale.KOREA,"%.2f", _magnitude) + "\n");
        }
        c.close();
    }

    /** Delete all data in the tables */
    public void onClickClear(View view) {
        clearTable();
    }

    /** Show the progress of data collection at each location */
    public void onClickProgress(View view) {
        Cursor c = db.rawQuery(
                "select pos_x, pos_y " +
                        "from " + tableName, null);
        int recordCount = c.getCount();
        contents.setText("");
        contents.append("Data collected at " + recordCount + " positions.\nPosition (x,y):\n");
        for (int i = 0; i < recordCount; i++) {
            c.moveToNext();
            double _posX = c.getDouble(0);
            double _posY = c.getDouble(1);
            contents.append("(" + String.format(Locale.KOREA,"%.2f", _posX)
                    + ", " + String.format(Locale.KOREA,"%.2f", _posY) + ")\n");
        }
        c.close();
    }

    /** Finish data collection and save the final result */
    public void onClickFinish(View view) {
        Intent intent = new Intent(getApplicationContext(), FinalActivity.class);
        startActivity(intent);
    }

    /** Create DB if there is none. It there is one, open it. */
    private void createDatabase() {
        Log.d("Log","creating database ["+databaseName+"]");
        try {
            db = openOrCreateDatabase(
                    databaseName,
                    Activity.MODE_PRIVATE,
                    null);

            databaseCreated = true;
            Log.d("Log","database has been created.");
        } catch(Exception e) {
            e.printStackTrace();
            Log.d("Log","database has not been created.");
        }
    }

    /** Create a table */
    private void createTable() {
        if (resetFlag) {
            db.execSQL("drop table if exists " + tableName);
        }
        db.execSQL("create table if not exists " + tableName + "(" +
                "_id integer PRIMARY KEY autoincrement, " +
                "pos_x real, pos_y real, " +
                "vertical real, horizontal real, magnitude real);");

        tableCreated = true;
    }

    /** Listen to the magnetometer */
    private class MagnetometerListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // All values are in micro-Tesla (uT)
            magX = event.values[0];
            magY = event.values[1];
            magZ = event.values[2];
            magSensorChanged = true;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /** Listen to the gravity sensor */
    private class GravityListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Force of gravity along the x,y and z axis
            // All values are in m/s^2
            gravX = event.values[0];
            gravY = event.values[1];
            gravZ = event.values[2];
            gravSensorChanged = true;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }

    /** Process raw sensor data and save corresponding converted data to the table */
    private void saveSensorData(final float x, final float y) {
        /** Magnetic Data Elements Conversion
         * Reference:
         * Lee, N.; Ahn S.; Han D. AMID: Accurate Magnetic Indoor Localization Using Deep Learning.
         * Sensors 2018, 18, 1598. */
        double G = Math.sqrt(Math.pow(gravX,2) + Math.pow(gravY,2) + Math.pow(gravZ,2));
        double cosA = gravZ/G;
        double magXY = Math.sqrt(Math.pow(magX,2) + Math.pow(magY,2));
        double magVer = magZ * cosA + magXY * Math.sqrt(1 - Math.pow(cosA,2));
        double magMag = Math.sqrt(Math.pow(magX,2) + Math.pow(magY,2) + Math.pow(magZ,2));
        double magHor = Math.sqrt(Math.pow(magMag,2) - Math.pow(magVer,2));

        final ContentValues recordValues = new ContentValues();

        recordValues.put("pos_x", x);
        recordValues.put("pos_y", y);
        recordValues.put("vertical", magVer);
        recordValues.put("horizontal", magHor);
        recordValues.put("magnitude", magMag);

        db.insert(tableName, null, recordValues);
    }

    /** Delete all records in the tables */
    private void clearTable() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear the tables");
        builder.setMessage("All the records in the tables will be deleted. " +
                "Are you sure to continue?");
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db.delete(tableName, null, null);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
