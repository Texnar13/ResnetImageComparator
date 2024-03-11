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
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
import java.lang.ref.WeakReference;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_LEFT_IMAGE = 1;
    private static final int RESULT_LOAD_RIGHT_IMAGE = 2;

    // какая модель выбрана
    private int currentModule = 0;
    /*
     * 0 - resnet18_traced
     * 1 - deeplabv3_scripted //deeplabv3_resnet101
     * 2 - mobilenet_v3_small
     * */

    // поток обсчета модели
    static Thread modelCalculation;
    // обратная связь от потока
    Handler modelCalculationLogging;
    // состояние обсчета
    int calculaitingStatate = 0;
    // 0 - ничего не выбрано
    // 1 - выбрано левое изображение
    // 2 - выбрано правое изображение
    // 3 - выбраны оба изображения
    // 4 - запущено вычисление


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
            if (calculaitingStatate != 4) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_LEFT_IMAGE);
            }
        });

        // Выбор правой картинки
        findViewById(R.id.main_activity_img_right).setOnClickListener(view -> {
            if (calculaitingStatate != 4) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, RESULT_LOAD_RIGHT_IMAGE);
            }
        });

        // Кнопка начала расчетов
        Button startButton = findViewById(R.id.main_activity_start_button);
        startButton.setOnClickListener(view -> {
            if (calculaitingStatate == 3) startCalculation();
        });
        startButton.setAlpha(0);


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


        // обратная связь от потока
        modelCalculationLogging = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);

                if (msg.what == 777) {
                    TextView outText = findViewById(R.id.main_activity_status_text);
                    outText.setText(outText.getText() + msg.obj.toString());
                }
                if (msg.what == 778) {
                    onCalculationEnd();
                }
            }
        };


        // разрешение на чтение картинок из памяти (если надо)
        requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

    }

    @Override
    protected void onDestroy() {
        modelCalculationLogging = null;
        modelCalculation.stop();
        super.onDestroy();
    }

    // метод запуска расчетов
    void startCalculation() {
        if (calculaitingStatate != 3) return;
        calculaitingStatate = 4;

        // чистим вывод
        ((TextView) findViewById(R.id.main_activity_status_text))
                .setText("Просчитываем модель:\n");

        // блокируем спиннер


        // показываем ProgressBar загрузки
        findViewById(R.id.main_activity_status_progressBar).setAlpha(1.0F);

        // запускаем поток вычислений
        modelCalculation = new Thread() {
            @Override
            public void run() {
                super.run();
                try {

                    detectImage();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        modelCalculation.start();

    }

    // вызов метода говорит о том, что расчеты завершены
    void onCalculationEnd() {
        calculaitingStatate = 3;

        // скрываем ProgressBar загрузки
        findViewById(R.id.main_activity_status_progressBar).setAlpha(0.0F);


        // разблокируем спиннер


        // не скрываем кнопку
    }


    // обратная связь вызова активити
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == RESULT_LOAD_LEFT_IMAGE || requestCode == RESULT_LOAD_RIGHT_IMAGE) &&
                resultCode == RESULT_OK && null != data) {

            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            ImageView imageView = findViewById(
                    (requestCode == RESULT_LOAD_LEFT_IMAGE) ?
                            R.id.main_activity_img_left : R.id.main_activity_img_right
            );
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

            //Setting the URI so we can read the Bitmap from the image
            imageView.setImageURI(null);
            imageView.setImageURI(selectedImage);

            // скрываем текст подсказки
            findViewById(
                    (requestCode == RESULT_LOAD_LEFT_IMAGE) ?
                            R.id.main_activity_img_left_hint : R.id.main_activity_img_right_hint
            ).setAlpha(0.0F);

            // манипулируем со статусом и запускаем обработку, если фотки одинаковые
            if (requestCode == RESULT_LOAD_LEFT_IMAGE) {
                if (calculaitingStatate == 0 || calculaitingStatate == 1) {
                    calculaitingStatate = 1;
                } else if (calculaitingStatate == 2) {
                    calculaitingStatate = 3;
                    // Показываем кнопку
                    findViewById(R.id.main_activity_start_button).setAlpha(1);
                }
            } else {
                if (calculaitingStatate == 0 || calculaitingStatate == 2) {
                    calculaitingStatate = 2;
                } else if (calculaitingStatate == 1) {
                    calculaitingStatate = 3;
                    // Показываем кнопку
                    findViewById(R.id.main_activity_start_button).setAlpha(1);
                }
            }
        }
    }


    // =============================================================================================
    //                           Расчет модели
    //==============================================================================================

    // отправить сообщение в выходной TextView
    void sendTextToLog(String text) {
        Message message = new Message();
        message.what = 777;
        message.obj = text;
        modelCalculationLogging.sendMessage(message);
    }

    void detectImage() throws IOException {

        sendTextToLog("Загрузка модели... ");
        // Загружаем файл с нейронкой
        Module module;
        switch (currentModule) {
            case 1:
                module = LiteModuleLoader.load(MainActivity.fetchModelFile(
                        MainActivity.this, "deeplabv3_scripted.ptl"));
                break;
            case 2:
                module = LiteModuleLoader.load(MainActivity.fetchModelFile(
                        MainActivity.this, "mobilenet_v3_small.ptl"));
                break;
            default: // 0
                module = LiteModuleLoader.load(MainActivity.fetchModelFile(
                        MainActivity.this, "resnet18_traced.ptl"));
                break;
        }
        sendTextToLog("завершена\n");


        // ------------------------------------ Расчет тензоров ------------------------------------

        sendTextToLog("Расчет тензоров\n");


        // Ссылка на изображение в разметке
        ImageView imageViewLeft = findViewById(R.id.main_activity_img_left);
        // Сохраняем картинку из imageView в Bitmap
        Bitmap bitmapLeft = ((BitmapDrawable) imageViewLeft.getDrawable()).getBitmap();
        // Пережатие изображения в 400*400
        bitmapLeft = Bitmap.createScaledBitmap(bitmapLeft, 400, 400, true);

        // Преобразуем изображение из bitmap во входной тензор
        final Tensor inputTensorLeft = TensorImageUtils.bitmapToFloat32Tensor(
                bitmapLeft,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );
        // обработка изображения в загруженной модели
        final float[] outTensorLeft = calculateTensor(currentModule, module, inputTensorLeft);
        sendTextToLog("Левое изображение просчитано\n");


        // Ссылка на изображение в разметке
        ImageView imageViewRight = findViewById(R.id.main_activity_img_right);
        // Сохраняем картинку из imageView в Bitmap
        Bitmap bitmapRight = ((BitmapDrawable) imageViewRight.getDrawable()).getBitmap();
        // Пережатие изображения в 400*400
        bitmapRight = Bitmap.createScaledBitmap(bitmapRight, 400, 400, true);

        // Преобразуем изображение из bitmap во входной тензор
        final Tensor inputTensorRight = TensorImageUtils.bitmapToFloat32Tensor(
                bitmapRight,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB
        );
        // обработка изображения в загруженной модели
        final float[] outTensorRight = calculateTensor(currentModule, module, inputTensorRight);
        sendTextToLog("Правое изображение просчитано\n");


        sendTextToLog("Размер тензора = " + outTensorLeft.length + "\nЕвклидово расстояние: ");
        sendTextToLog("" + calculateEuclidDistance(outTensorLeft, outTensorRight));


        sendTextToLog("\nКосинусное расстояние: ");
        sendTextToLog("" + calculateCosineDistance(outTensorLeft, outTensorRight));


        // сигнал для активности о завершении расчетов
        modelCalculationLogging.sendEmptyMessage(778);

    }

    // обработка изображения в загруженной модели
    float[] calculateTensor(int currentModule, Module module, Tensor inputTensor) {

        final float[] score_arr;
        switch (currentModule) {
            case 1: {
                Map<String, IValue> outTensors = module.forward(IValue.from(inputTensor)).toDictStringKey();
                // the key "out" of the output tensor contains the semantic masks
                // see https://pytorch.org/hub/pytorch_vision_deeplabv3_resnet101
                final Tensor outputTensor = outTensors.get("out").toTensor();
                score_arr = outputTensor.getDataAsFloatArray();
                break;
            }
            case 2: {
                final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                score_arr = outputTensor.getDataAsFloatArray();
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

                // вывод                      // Находим название в листе основанном на индексах
                sendTextToLog("   Обнаружено: " + ModelClasses.MODEL_CLASSES[ms_ix] + "\n");
                break;
            }
        }

        return score_arr;
    }


    // Расчет евклидова расстояния
    float calculateEuclidDistance(float[] tensorLeft, float[] tensorRight) {
        if (tensorLeft.length != tensorRight.length) return 0;

        // d = sqrt( SUMM[i=0->length]( (Li-Ri)^2 ))

        float summ = 0;
        float temp;

        for (int dimensionI = 0; dimensionI < tensorLeft.length; dimensionI++) {
            temp = tensorLeft[dimensionI] - tensorRight[dimensionI];
            temp *= temp;
            summ += temp;
        }

        return (float) Math.sqrt(summ);
    }

    // Расчет косинусного расстояния
    float calculateCosineDistance(float[] tensorLeft, float[] tensorRight) {
        if (tensorLeft.length != tensorRight.length) return 0;

        float divisible = 0;
        float leftMid = 0;
        float rightMid = 0;

        for (int dimensionI = 0; dimensionI < tensorLeft.length; dimensionI++) {
            divisible += (tensorLeft[dimensionI] * tensorRight[dimensionI]);

            leftMid += tensorLeft[dimensionI] * tensorLeft[dimensionI];
            rightMid += tensorRight[dimensionI] * tensorRight[dimensionI];
        }

        float divider = (float) (Math.sqrt(leftMid) * Math.sqrt(rightMid));

        return divisible / divider;
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







