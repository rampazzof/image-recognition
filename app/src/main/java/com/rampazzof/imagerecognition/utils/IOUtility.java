package com.rampazzof.imagerecognition.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class IOUtility {

    public static String streamToString( InputStream is) throws IOException {

        StringBuilder sb = new StringBuilder();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String line;

        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }

        is.close();
        rd.close();

        return sb.toString();
    }

    public static void fileToByteArray(OutputStream outputStream, String pathToFile ) throws IOException {
        outputStream.write( getBytes( pathToFile ) );
    }

    private static byte[] getBytes(String path) {

        byte[] getBytes = {};
        File file = new File( path );

        try( InputStream inputStream = new FileInputStream( file ) ) {

            if ( ( float ) file.length() / ( 1024 * 1024 ) > 4 ) {
                Log.d("Error:", "Maggiore di 4 MB" );
            }

            getBytes = new byte[ ( int ) file.length() ];
            inputStream.read( getBytes );

        } catch( FileNotFoundException e ) {
            e.printStackTrace();
        } catch( IOException e ) {
            e.printStackTrace();
        }

        return getBytes;
    }

}
