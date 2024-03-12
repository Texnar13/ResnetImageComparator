package com.texnar13.resnetimagecomparator;

public class SettingsSharedPrefsContract {


    // название переменных сохраненных в памяти
    public static final String[] PREFS_FLOAT_THRESHOLD_EUCLID = {"euclid_0", "euclid_1", "euclid_2"};
    public static final float[] PREFS_FLOAT_THRESHOLD_EUCLID_DEFAULT = {20,30,25};

    public static final String[] PREFS_FLOAT_THRESHOLD_COSINE = {"cosine_0", "cosine_1", "cosine_2"};
    public static final float[] PREFS_FLOAT_THRESHOLD_COSINE_DEFAULT = {21,31,26};



    //
//    public static final String PREFS_INT_GDPR_STATE = "GDPRState";
//    public static final int PREFS_INT_GDPR_STATE_NONE = 0;
//    public static final int PREFS_INT_GDPR_STATE_ACCEPT = 1;
//    public static final int PREFS_INT_GDPR_STATE_DECLINE = 2;

}



// получаем сохраненные данные
//        SharedPreferences preferences =
//                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                { // имя
//                      String errorString = getResources().getString(R.string.no_name_text);
//                      String name = preferences.getString(StudentSettingsActivity.PREF_NAME, errorString);
//                      if (!name.equals(errorString)) {
//                          nameEditText.setText(name);
//                      }
//                }
//
//
//
//
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                // имя
//                String name = preferences.getString(StudentSettingsActivity.PREF_NAME,
//                getResources().getString(R.string.no_name_text));
//
//    // сохраняем параметр в SharedPreferences
//    SharedPreferences.Editor editor =
//            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
//                                editor.putInt(SharedPrefsContract.PREFS_PREMIUM_STATE,
//    SharedPrefsContract.VALUE_PREMIUM_ACTIVE);
//                                editor.apply();