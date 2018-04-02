package com.rampazzof.imagerecognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.provider.SyncStateContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.rampazzof.imagerecognition.api.Caller;

import org.json.*;
import com.loopj.android.http.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import cz.msebera.android.httpclient.Header;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static android.provider.Telephony.Carriers.PASSWORD;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String ENDPOINT = "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0";
    private static final String APIKEY = "";

    private ImageView imageHolder;
    String mCurrentPhotoPath;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
    }

    public void takePhoto( View view ) {

        if( ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this,
                    new String[]{ Manifest.permission.CAMERA }, 1 );

        }
        else if( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this,
                    new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1 );

        }
        else if( ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions( this,
                    new String[]{ Manifest.permission.INTERNET }, 1 );

        }
        else {

            Intent takePictureIntent = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );

            if( takePictureIntent.resolveActivity( getPackageManager() ) != null ) {

                File photo = null;
                try {
                    photo = createImageFile();
                }
                catch ( IOException e ) {
                    Log.getStackTraceString( e );
                }
                if( photo != null ) {
                    Uri uriPhoto = FileProvider.getUriForFile(
                            this,
                            "com.rampazzof.imagerecognition.fileprovider",
                            photo );
                    takePictureIntent.putExtra( MediaStore.EXTRA_OUTPUT, uriPhoto );
                    startActivityForResult( takePictureIntent, REQUEST_IMAGE_CAPTURE );
                }
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK ) {
            imageHolder = findViewById( R.id.capturedPhoto );
            imageHolder.setImageBitmap( BitmapFactory.decodeFile( mCurrentPhotoPath ) );

            try {
                sendPostRequest();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format( new Date() );
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir( Environment.DIRECTORY_PICTURES );
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void sendPostRequest() throws MalformedURLException, IOException {

        URL url = new URL("https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/analyze" );
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setRequestProperty( "Content-Type", "application/octet-stream" );
            urlConnection.setRequestProperty("Ocp-Apim-Subscription-Key", "3ee10ebd611a41bdb4b9cbdbf877d4ef" );

            urlConnection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(Files.readAllBytes( new File( mCurrentPhotoPath ).toPath() ).toString());
            wr.flush();
            wr.close();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            streamToString( in );
        } finally {
            urlConnection.disconnect();
        }
    }

    private static String streamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}
