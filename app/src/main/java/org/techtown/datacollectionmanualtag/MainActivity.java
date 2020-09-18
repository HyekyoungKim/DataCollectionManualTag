package org.techtown.datacollectionmanualtag;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onButtonClicked (View v) {
        Intent intent = new Intent(getApplicationContext(), CollectionActivity.class);

        final RadioGroup radioGroup = findViewById(R.id.radioGroup);
        int id = radioGroup.getCheckedRadioButtonId();

        if (id == R.id.radio1){ // Reset DB
            intent.putExtra("reset", true);
            startActivity(intent);
        } else if (id == R.id.radio2){  // Keep the existing DB
            intent.putExtra("reset", false);
            startActivity(intent);
        } else {    // No radio button is checked
            Toast.makeText(getApplicationContext(),
                    "Please check one of the options.", Toast.LENGTH_SHORT).show();
        }
    }
}
