package com.texnar13.resnetimagecomparator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    // кнопки Action bar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case android.R.id.home:
                // кнопка назад
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // Номер текущей сети
    int currentModule = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_settings);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        // Спиннер выбора модели
        Spinner spinner = findViewById(R.id.activity_settings_model_selector);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.modules_names,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

                // сохранение предыдущих значений в SP
                if (currentModule != -1) {
                    saveThresholds();
                }

                // выбор текущей модели
                currentModule = i;

                // загрузка данных из SP
                SharedPreferences preferences =
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                float euclid = preferences.getFloat(SettingsSharedPrefsContract.PREFS_FLOAT_THRESHOLD_EUCLID[currentModule], 0);
                ((TextView) findViewById(R.id.activity_settings_input_euclid)).setText("" + euclid);

                float cosine = preferences.getFloat(SettingsSharedPrefsContract.PREFS_FLOAT_THRESHOLD_COSINE[currentModule], 0);
                ((TextView) findViewById(R.id.activity_settings_input_cosine)).setText("" + cosine);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        // сохраняем
        saveThresholds();
        super.onDestroy();
    }


    void saveThresholds() {
        try {
            float euclid = Float.parseFloat(((EditText) findViewById(R.id.activity_settings_input_euclid)).getText().toString());
            float cosine = Float.parseFloat(((EditText) findViewById(R.id.activity_settings_input_cosine)).getText().toString());

            SharedPreferences.Editor editor =
                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
            editor.putFloat(SettingsSharedPrefsContract.PREFS_FLOAT_THRESHOLD_EUCLID[currentModule], euclid);
            editor.putFloat(SettingsSharedPrefsContract.PREFS_FLOAT_THRESHOLD_COSINE[currentModule], cosine);
            editor.apply();
            Toast.makeText(this, "Сохранено", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ошибка, остались старые значения", Toast.LENGTH_SHORT).show();
        }
    }

}