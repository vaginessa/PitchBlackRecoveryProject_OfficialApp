package com.github.pitchblackrecoveryproject.pitchblackrecoveryproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Environment;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    //final static String path = Environment.getExternalStorageDirectory() + File.separator + "PBRP";
    final static String path = "/sdcard/PBRP";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logic();
    }

    protected void logic() {
        TextView message = (TextView) findViewById(R.id.info);
        Process p;
        try {
            // Preform su to get root privileges
            p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());

            // Create folder if it doesn't exists
            File pbrp = new File(path);
            boolean success = true;
            /*Log.i("PBRP", "Starting to create folder");
            if (!pbrp.exists()) {
                Log.i("PBRP", "e1");
                success = pbrp.mkdir();
                Log.i("PBRP", "e2");
            }
            Log.i("PBRP", "Done");*/

            // Copy default.prop/prop.default to PBRP
            Log.i("PBRP", "Staring to write");
            os.writeBytes("cp /prop.default " + path + "/build.info\n");
            os.writeBytes("cp /default.prop " + path + "/build.info\n");
            Log.i("PBRP", "Done writing");

            // Check if file copied successfully

            // Close the terminal
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    // TODO Code to run on success
                    message.setText("root");
                }
                else {
                    // TODO Code to run on unsuccessful
                    message.setText("not root");
                }
            } catch (InterruptedException e) {
                // TODO Code to run in interrupted exception
                message.setText("not root");
            }
        } catch (IOException e) {
            // TODO Code to run in input/output exception
            message.setText("not root");
        }
    }
}
