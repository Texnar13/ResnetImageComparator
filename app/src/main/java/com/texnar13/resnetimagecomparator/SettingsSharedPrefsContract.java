package com.texnar13.resnetimagecomparator;

public class SettingsSharedPrefsContract {

    // название переменных сохраненных в памяти
    public static final String[] PREFS_FLOAT_THRESHOLD_EUCLID = {"euclid_0", "euclid_1", "euclid_2", "euclid_3"};
    public static final float[] PREFS_FLOAT_THRESHOLD_EUCLID_DEFAULT = {20, 30, 25, 25};

    public static final String[] PREFS_FLOAT_THRESHOLD_COSINE = {"cosine_0", "cosine_1", "cosine_2", "cosine_3"};
    public static final float[] PREFS_FLOAT_THRESHOLD_COSINE_DEFAULT = {21, 31, 26, 25};

}