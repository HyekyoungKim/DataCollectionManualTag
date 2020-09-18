package org.techtown.datacollectionmanualtag;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.Locale;

public class FinalActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);

        /* Show the final result (fully processed data at each location) */
        final TextView contents = findViewById(R.id.contents);
        Cursor c = CollectionActivity.db.rawQuery(
                "select pos_x, pos_y, vertical, horizontal, magnitude " +
                        "from " + CollectionActivity.tableName, null);
        int recordCount = c.getCount();
        contents.setText("");
        for (int i = 0; i < recordCount; i++) {
            c.moveToNext();
            double _posX = c.getDouble(0);
            double _posY = c.getDouble(1);
            double _vertical = c.getDouble(2);
            double _horizontal = c.getDouble(3);
            double _magnitude = c.getDouble(4);

            contents.append("Location (x,y): (" + String.format(Locale.KOREA,"%.2f", _posX)
                    + ", " + String.format(Locale.KOREA,"%.2f", _posY) + ")" + "\n" +
                    "Vertical: " +
                    String.format(Locale.KOREA, "%.2f",_vertical) + "\n" +
                    "Horizontal: " +
                    String.format(Locale.KOREA, "%.2f",_horizontal) + "\n" +
                    "Magnitude: " +
                    String.format(Locale.KOREA, "%.2f",_magnitude) + "\n\n");
        }
        c.close();
    }
}
