package com.texnar13.resnetimagecomparator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NavUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 1;

    // какая модель выбрана
    private int currentModule = 0;
    /*
     * 0 - resnet18_traced
     * 1 - deeplabv3_scripted //deeplabv3_resnet101
     * */

    // кнопки Action bar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add("Настройки");
        menu.getItem(0)
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setOnMenuItemClickListener(menuItem -> {

                    // нажатие на кнопки Action bar
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;

                })
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Назначение обработчиков

        // Выбор левой картинки
        findViewById(R.id.main_activity_img_left).setOnClickListener(view -> {
            Intent i = new Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);
        });

        // Выбор правой картинки
        findViewById(R.id.main_activity_img_right).setOnClickListener(view -> {
            try {
                detectImage();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Спиннер выбора модели
        Spinner spinner = findViewById(R.id.main_activity_spinner);
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
                currentModule = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        // разрешение на чтение картинок из памяти (если надо)
        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

    }

    // обратная связь вызова активити
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = findViewById(R.id.main_activity_img_left);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            //Setting the URI so we can read the Bitmap from the image
            imageView.setImageURI(null);
            imageView.setImageURI(selectedImage);

            // скрываем текст подсказки
            findViewById(R.id.main_activity_img_left_hint).setAlpha(0.0F);
        }
    }

    void detectImage() throws IOException {


        // показываем ProgressBar загрузки
        findViewById(R.id.main_activity_status_progressBar).setAlpha(1.0F);
        // чтобы работало надо в поток...


        // Ссылка на изображение в разметке
        ImageView imageView = findViewById(R.id.main_activity_img_left);

        // Сохраняем картинку из imageView в Bitmap
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        // Пережатие изображения в 400*400
        bitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

        // Загружаем файл с нейронкой
        Module module;
        switch (currentModule) {
            case 1:
                module = LiteModuleLoader.load(MainActivity.fetchModelFile(
                        MainActivity.this, "deeplabv3_scripted.ptl"));
                break;
            default: // 0
                module = LiteModuleLoader.load(MainActivity.fetchModelFile(
                        MainActivity.this, "resnet18_traced.ptl"));
                break;
        }

        // Преобразуем изображение из bitmap во входной тензор
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(
                bitmap,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );

        // обработка изображения в загруженной модели
        final float[] score_arr;
        switch (currentModule) {
            case 1: {
                Map<String, IValue> outTensors = module.forward(IValue.from(inputTensor)).toDictStringKey();
                // the key "out" of the output tensor contains the semantic masks
                // see https://pytorch.org/hub/pytorch_vision_deeplabv3_resnet101
                final Tensor outputTensor = outTensors.get("out").toTensor();
                score_arr = outputTensor.getDataAsFloatArray();


                // вывод
                ((TextView) findViewById(R.id.main_activity_status_text))
                        .setText("3 len = " + score_arr.length);
                break;
            }
            default: { // 0
                final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                score_arr = outputTensor.getDataAsFloatArray();


                // Fetch the index of the value with maximum score
                float max_score = -Float.MAX_VALUE;
                int ms_ix = -1;
                for (int i = 0; i < score_arr.length; i++) {
                    if (score_arr[i] > max_score) {
                        max_score = score_arr[i];
                        ms_ix = i;
                    }
                }

                // Находим название в листе основанном на индексах
                String detected_class = ModelClasses.MODEL_CLASSES[ms_ix];


                // вывод
                ((TextView) findViewById(R.id.main_activity_status_text))
                        .setText(detected_class);
                break;
            }
        }


        // скрываем ProgressBar загрузки
        findViewById(R.id.main_activity_status_progressBar).setAlpha(0.0F);
    }


    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    public static String fetchModelFile(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

}







