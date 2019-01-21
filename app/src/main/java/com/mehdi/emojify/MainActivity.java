package com.mehdi.emojify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import pl.droidsonroids.gif.GifImageView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 1;

    private static final String FILE_PROVIDER_AUTHORITY = "com.mehdi.emojify.fileprovider";

    private ImageView mImageView;

    private Button mEmojifyButton;
    private FloatingActionButton mClearFab;

    private TextView mTitleTextView;
    private GifImageView gif;


    private String mTempPhotoPath;
    private Bitmap mResultsBitmap;
    private RelativeLayout layout;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Bind the views
        mImageView = findViewById(R.id.image_view);
        mEmojifyButton = findViewById(R.id.emojify_button);
        mClearFab = findViewById(R.id.clear_button);
        mTitleTextView = findViewById(R.id.title_text_view);
        layout = findViewById(R.id.rela);
        gif = findViewById(R.id.gif);
        visibleBegin();
    }

    @SuppressLint("RestrictedApi")
    public void visibleBegin(){
        mEmojifyButton.setVisibility(View.VISIBLE);
        mTitleTextView.setVisibility(View.VISIBLE);
        layout.setVisibility(View.GONE);
        mImageView.setVisibility(View.GONE);
        mClearFab.setVisibility(View.GONE);
    }

    @SuppressLint("RestrictedApi")
    public void visibleFun(){
        mEmojifyButton.setVisibility(View.GONE);
        mTitleTextView.setVisibility(View.GONE);
        layout.setVisibility(View.VISIBLE);
        mImageView.setVisibility(View.VISIBLE);
        mClearFab.setVisibility(View.VISIBLE);
    }

    /*
     */
    /**
     * OnClick method for "Emojify Me!" Button. Launches the camera app.
     *
     * @param view The emojify me button.
     */
    public void emojifyMe(View view) {
        int GRANTED = PackageManager.PERMISSION_GRANTED;
        int write = ContextCompat.checkSelfPermission(this , Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read = ContextCompat.checkSelfPermission(this , Manifest.permission.READ_EXTERNAL_STORAGE);
        int camera = ContextCompat.checkSelfPermission(this , Manifest.permission.CAMERA);
        if (write != GRANTED || camera != GRANTED || read != GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, REQUEST_STORAGE_PERMISSION);
        }else {
            launchCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 1 && grantResults[0] == 0 && grantResults[1] == 0 && grantResults[2] == 0) {
                    launchCamera();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent("android.media.action.IMAGE_CAPTURE");
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) return;
            File photoFile = null;
            try {
                photoFile = BitmapUtils.createTempImageFile(this);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile == null) return;

        mTempPhotoPath = photoFile.getAbsolutePath();
        Uri photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, photoFile);
        takePictureIntent.putExtra("output", photoURI);
         startActivityForResult(takePictureIntent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == -1) {
            processAndSetImage();
        } else {
            BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        }
    }

    @SuppressLint("RestrictedApi")
    private void processAndSetImage(){

        visibleFun();
        mResultsBitmap = imageRotated();
        mImageView.setImageBitmap(mResultsBitmap);
        new as().execute(mResultsBitmap);

    }

    public Bitmap imageRotated(){

        Bitmap bitmap = BitmapFactory.decodeFile(mTempPhotoPath);

        ExifInterface ei = null;
        try {
            ei = new ExifInterface(mTempPhotoPath);
        }catch (IOException e){ }

        Bitmap rotatedBitmap = null;
        if (ei != null){
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }
        }

        return rotatedBitmap;

    }
    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }


    @SuppressLint("StaticFieldLeak")
    class as extends AsyncTask<Bitmap, Void, Bitmap>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            gif.setVisibility(View.VISIBLE);

        }

        @Override
        protected Bitmap doInBackground(Bitmap... bitmaps) {

            FaceDetector detector = new FaceDetector.Builder(MainActivity.this)
                    .setTrackingEnabled(false)
                    .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                    .build();

            Frame frame = new Frame.Builder().setBitmap(bitmaps[0]).build();
            SparseArray<Face> faces = detector.detect(frame);

            Bitmap bitmapWithEmojis = null;
            if (faces.size() > 0){
                for (int i = 0; i < faces.size(); i++){
                    Face face = faces.valueAt(i);
                    int Emoji = whicheEmoji(face);
                     bitmapWithEmojis = addBitmapToFace(bitmaps[0], BitmapFactory.decodeResource(getResources(), Emoji), face);
                     }}
            detector.release();
            return bitmapWithEmojis;
        }

        @Override
        protected void onPostExecute(Bitmap s) {
            super.onPostExecute(s);
            gif.setVisibility(View.GONE);
            //Toast.makeText(MainActivity.this, s, Toast.LENGTH_LONG).show();
            if (s != null){
                mResultsBitmap = s;
                mImageView.setImageBitmap(s);
            }
        }

    }

    public Integer whicheEmoji(Face face){

        StringBuilder stat = new StringBuilder("Not defind");

        float leftOpen = face.getIsLeftEyeOpenProbability();
        float rightOpen = face.getIsRightEyeOpenProbability();
        float smile = face.getIsSmilingProbability();
        float f = Face.UNCOMPUTED_PROBABILITY;

        boolean left = (leftOpen != f) && (leftOpen >= 0.5);
        boolean right = (rightOpen != f) && (rightOpen >= 0.5);
        boolean smil = (smile != f) && (smile >= 0.15);


        if (left && right){
            stat = new StringBuilder("d");
        }
        if (right && !left){
            stat = new StringBuilder("r");
        }
        if (!right && left){
            stat = new StringBuilder("l");
        }
        if (!right && !left){
            stat = new StringBuilder("c");
        }

        if (smil){
            stat.append("o");
        }else {
            stat.append("c");
        }

        switch (stat.toString()){
            case "do":
                return R.drawable.smile;
            case "ro":
                return R.drawable.leftwink;
            case "lo":
                return R.drawable.rightwink;
            case "dc":
                return R.drawable.frown;
            case "rc":
                return R.drawable.leftwinkfrown;
            case "lc":
                return R.drawable.rightwinkfrown;
            case "co":
                return R.drawable.closed_smile;
            case "cc":
                return R.drawable.closed_frown;
            default:
                return -1;
        }
    }


    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        float scaleFactor = 1f;

        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() * newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        float emojiPositionX = (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;

        float emojiPositionY = (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;
    }


    public void saveMe(View view) {
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        BitmapUtils.saveImage(this, mResultsBitmap);
    }

    public void shareMe(View view) {
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
        BitmapUtils.saveImage(this, mResultsBitmap);
        BitmapUtils.shareImage(this, mTempPhotoPath);
    }

    /**
     * OnClick for the clear button, resets the app to original state.
     *
     * @param view The clear button.
     */
    @SuppressLint("RestrictedApi")
    public void clearImage(View view) {
        mImageView.setImageResource(0);
        visibleBegin();
        BitmapUtils.deleteImageFile(this, mTempPhotoPath);
    }

}
