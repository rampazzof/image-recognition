package com.rampazzof.imagerecognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
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

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
            callApi( mCurrentPhotoPath );
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

    private void callApi( String str ) {
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("url", str);
            final String requestBody = jsonBody.toString();

            RequestQueue queue = Volley.newRequestQueue(this);

            StringRequest postRequest = new StringRequest(
                    Request.Method.POST,
                    ENDPOINT,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // response
                            Log.d("Response", response);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            Log.d("Error.Response", error.toString());
                        }
                    }
            ) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = super.getHeaders();
                    headers.put("Content-Type", "application/json");
                    headers.put("Ocp-Apim-Subscription-Key", APIKEY);
                    return headers;
                }

                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> params = super.getParams();
                    params.put("visualFeatures", "Categories,Description,Color");
                    params.put("language", "en");

                    return params;
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }
            };
            queue.add(postRequest);
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }
    }
}
