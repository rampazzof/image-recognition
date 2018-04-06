package com.rampazzof.imagerecognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageHelper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rampazzof.imagerecognition.interfaces.OnTaskComplete;
import com.rampazzof.imagerecognition.utils.IOUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity implements OnTaskComplete {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String ENDPOINT = "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/describe";
    private static final String APIKEY = "3ee10ebd611a41bdb4b9cbdbf877d4ef";

    TextView textView;
    String mCurrentPhotoPath;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        ImageView imageHolder = findViewById( R.id.capturedPhoto );
        imageHolder.setVisibility( View.GONE );

    }

    public void takePhoto( View view ) {

        if( ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.CAMERA },
                    1 );

        }
        else if( ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    1 );

        }
        else if( ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET ) != PackageManager.PERMISSION_GRANTED ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.INTERNET },
                    1 );

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
    public void onActivityResult( int requestCode, int resultCode, Intent data ) {

        if ( requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK ) {

            ImageView imageView = findViewById( R.id.capturedPhoto );
            Bitmap bitmap = BitmapFactory.decodeFile( mCurrentPhotoPath );
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotated = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true );
            imageView.setImageBitmap( rotated );
            imageView.setVisibility( View.VISIBLE );

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

    public void sendPostRequest() throws IOException {

        textView = findViewById( R.id.resultText );
        textView.setText( "Loading..." );

        new AsyncTask< Void, Void, Void >() {

            @Override
            protected Void doInBackground( Void... voids ) {

                HttpURLConnection urlConnection = null;
                OutputStream outputStream = null;
                InputStream inputStream = null;
                String result = null;
                int responseCode = 0;

                try {

                    URL url = new URL( ENDPOINT );
                    urlConnection = ( HttpURLConnection ) url.openConnection();
                    urlConnection.setConnectTimeout( 60000 );
                    urlConnection.setReadTimeout( 60000 );
                    urlConnection.setRequestProperty( "Content-Type", "application/octet-stream" );
                    urlConnection.setRequestProperty( "Ocp-Apim-Subscription-Key", APIKEY );

                    urlConnection.setDoOutput( true );
                    outputStream = new BufferedOutputStream( urlConnection.getOutputStream() );
                    IOUtility.fileToByteArray( outputStream, mCurrentPhotoPath );

                    inputStream = new BufferedInputStream( urlConnection.getInputStream() );
                    result = IOUtility.streamToString( inputStream );
                    responseCode = urlConnection.getResponseCode();
                    MainActivity.this.onTaskCompleted( responseCode, result );

                }
                catch ( IOException e ) {
                    Log.d( "Error","Error connecting do the server" );
                    try {
                        MainActivity.this.onTaskCompleted( urlConnection.getResponseCode(), urlConnection.getResponseMessage() );
                    }
                    catch ( IOException ioe ) {
                        e.printStackTrace();
                    }
                }
                catch( Exception e ) {
                    e.printStackTrace();
                }
                finally {
                    if( inputStream != null ) { try { inputStream.close(); } catch (Exception e ){} };
                    if( outputStream != null ) { try { outputStream.close(); } catch (Exception e ){} };
                    urlConnection.disconnect();
                }
                return null;
            }
        }.execute();

    }

    @Override
    public void onTaskCompleted( int responseCode, String string ) {

        if( responseCode == 200 ) {

            try {

                String result;

                JSONObject jsonObject = new JSONObject( string );
                JSONObject description = jsonObject.getJSONObject( "description" );
                JSONArray tagsArray = description.getJSONArray( "tags" );
                String tags = "Tags: " + tagsArray.getString( 0 );
                for( int i = 1; i < tagsArray.length(); i++ ) {
                    tags = tags + ", " + tagsArray.getString(i);
                }
                tags = tags + "\n\n";
                JSONObject captionsArray = ( JSONObject )description.getJSONArray( "captions" ).get(0);
                String captionText = captionsArray.getString( "text" );
                String caption = "Caption: " + captionText + "\n\n";
                Double captionConfidence = captionsArray.getDouble( "confidence" );
                String confidence = "Confidence: " + String.valueOf( captionConfidence );

                textView.setText( tags + caption + confidence );

            }
            catch ( JSONException e ) {
                e.printStackTrace();
                textView.setText( e.getMessage() );
            }

        }
        else {

            textView.setText( String.valueOf( responseCode ) + " " + string );

        }

    }

}
