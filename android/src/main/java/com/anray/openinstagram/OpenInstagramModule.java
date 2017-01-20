package com.anray.openinstagram;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.IdRes;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


import static android.app.Activity.RESULT_OK;


public class OpenInstagramModule extends ReactContextBaseJavaModule {

    private static final int REQUEST_CAMERA_PICTURE = 100;
    private static final int CAMERA_REQUEST_PERMISSION_CODE = 101;
    private static final int FROM_INSTAGRAM = 200;
    private Button mInstaBtn;
    private File mPhotoFile = null;


    private String mHashTagValue = "#Imthebest ";
    private String mInstagramPackageName = "com.instagram.android";
    private String mPlayMarketAbsentMessage = "Play Маркет отсутствует.";
    private String mLoadFromCameraErrorMessage = "Загрузка из камеры не удалась";
    private String mPhotoWasUploadedMessage = "Спасибо! Ваше фото загружено!";
    private String mPhotoUploadWasCanceledMessage = "Операция отменена";


    private String mHashTagPasteMessage = "Вставьте в подпись #ХэшТег из буфера";
    private String mImageFileName;
    private final String PHOTO_UID = "527ba7dcf5099904b634a9f9ae0da05c4af6d08b";
    private boolean mShowToast = false;


    //EXPORT CONSTANTS EXAMPLE
  /*
  private static final String DURATION_SHORT_KEY = "SHORT";
  private static final String DURATION_LONG_KEY = "LONG";

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    constants.put(DURATION_SHORT_KEY, Toast.LENGTH_SHORT);
    constants.put(DURATION_LONG_KEY, Toast.LENGTH_LONG);
    return constants;
  }

  */

    public OpenInstagramModule(ReactApplicationContext reactContext) {
        super(reactContext);

        // Add the listener for `onActivityResult`
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

        /**
         * Получение результата от другой Activity: фото из камеры или галерии
         *
         * @param requestCode код идентифицирующий источник фото
         * @param resultCode  код операции
         * @param data        возвращенный интент
         */
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

            if (requestCode == REQUEST_CAMERA_PICTURE) {

                if (resultCode == RESULT_OK) {
                    createInstagramIntent(mPhotoFile);

                }

            } else {

                //delete temp files
                try {

                    for (File file : mPhotoFile.getParentFile().listFiles()) {
                        if (!file.isDirectory()) {
                            if (file.getName().contains(PHOTO_UID))
                                file.delete();
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (resultCode == RESULT_OK) {

                    showToast(mPhotoWasUploadedMessage);
                } else {

                    showToast(mPhotoUploadWasCanceledMessage);

                }
            }

        }
    };

    @Override
    public String getName() {
        return "OpenInstagram";
    }


    private void createInstagramIntent(File file) {

        //проерка установлен ли Instagram
        Intent intent = getReactApplicationContext().getPackageManager().getLaunchIntentForPackage(mInstagramPackageName);
        if (intent != null) {

           /* // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager) getReactApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);

            // Creates a new text clip to put on the clipboard
            ClipData clip = ClipData.newPlainText(null, mHashTagValue);


            // Set the clipboard's primary clip.
            clipboard.setPrimaryClip(clip);*/


            Intent instagram = new Intent(android.content.Intent.ACTION_SEND);
            instagram.setType("image/*");
            instagram.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));

            instagram.putExtra(Intent.EXTRA_TEXT, mHashTagValue);
            instagram.setPackage(mInstagramPackageName);
            getCurrentActivity().startActivityForResult(instagram, FROM_INSTAGRAM);

            if (mShowToast) {
                showToast(mHashTagPasteMessage);
            }


        } else {
            // bring user to the market to download the app.

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + mInstagramPackageName));

            try {
                getCurrentActivity().startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                //if Play Market is absent

                showToast(mPlayMarketAbsentMessage);
            }
        }

    }

    @ReactMethod
    public void loadPhotoFromCameraWithHastag(String hashTagValue) {

        mHashTagValue = hashTagValue;

        loadPhotoFromCamera();

    }

    @ReactMethod
    public void setHashTag(String hashTagValue) {

        mHashTagValue = hashTagValue;

    }

    @ReactMethod
    public void setHashTagToastMessage(String hashTagPasteMessage) {

        mHashTagPasteMessage = hashTagPasteMessage;

    }

    @ReactMethod
    public void shareToInstagram(boolean showToast) {

        mShowToast = showToast;

        loadPhotoFromCamera();

    }

    /**
     * pass null value to prevent showing Toast message
     *
     * @param hashTagValue
     * @param hashTagPasteMessage
     * @param photoWasUploadedMessage
     * @param photoUploadWasCanceledMessage
     */
    @ReactMethod
    public void loadPhotoFromCameraWithMessages(String hashTagValue, String hashTagPasteMessage, String photoWasUploadedMessage, String photoUploadWasCanceledMessage) {

        mHashTagValue = hashTagValue;
        mHashTagPasteMessage = hashTagPasteMessage;
        mPhotoWasUploadedMessage = photoWasUploadedMessage;
        mPhotoUploadWasCanceledMessage = photoUploadWasCanceledMessage;

        loadPhotoFromCamera();

    }


    @ReactMethod
    public void loadPhotoFromCamera() {

//    if (ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
//            && ContextCompat.checkSelfPermission(getCurrentActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

        //Will check permissions by React Native component

        //если есть права
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getCurrentActivity().getPackageManager()) != null) {
            try {
                mPhotoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();

                Log.e("TAG_CAMERA", "Load from camera was not successful");
                showToast(mLoadFromCameraErrorMessage);

            }

            if (mPhotoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mPhotoFile));
                getCurrentActivity().startActivityForResult(takePictureIntent, REQUEST_CAMERA_PICTURE);

            }
        }


  /*  else

    {
        //запрашиваем разрешения
        ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, CAMERA_REQUEST_PERMISSION_CODE);


    }*/
    }

    @ReactMethod
    public void showToast(String message) {

        if (message != null) {
            Toast.makeText(getCurrentActivity(), message, Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Создание файла, куда будет сохранятся фотография с камеры
     *
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        mImageFileName = "JPEG_" + timestamp + "_" + PHOTO_UID + "_";

        //get folder where app is installed
        File storageDir = getCurrentActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(mImageFileName, ".jpg", storageDir);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.MediaColumns.DATA, image.getAbsolutePath());

        try {
            getCurrentActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return image;

    }

  /*
  // onRequestPermissionsResult method cannot be processed by JS code, therefore need to process by native component
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == CAMERA_REQUEST_PERMISSION_CODE && grantResults.length == 2) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        // 07.07.2016 тут обрабатываем разрешение, если разрешение получено вывести сообщение или реализовать какую-то другую логику
//                showToast(getString(R.string.need_camera_permissions));
        loadPhotoFromCamera();
      }


    }
  }*/


    @SuppressWarnings("unchecked")
    public <T> T $(@IdRes int resource) {
        return (T) getCurrentActivity().findViewById(resource);
    }

}