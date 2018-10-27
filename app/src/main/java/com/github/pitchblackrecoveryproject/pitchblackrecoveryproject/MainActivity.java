package com.github.pitchblackrecoveryproject.pitchblackrecoveryproject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;
import android.widget.TextView;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;

public class MainActivity extends AppCompatActivity {
    final static String path = Environment.getExternalStorageDirectory() + File.separator + "PBRP";
    final static String buildFile = "pbrp.info";
    String pbReleases = "";
    int root = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getRootAccess((TextView) findViewById(R.id.info), new File(path + "/" + buildFile)) == 1)
        init();
    }

    protected void init() {
        try {
            // Create a URL for the desired page
            URL url = new URL("https://raw.githubusercontent.com/PitchBlackRecoveryProject/vendor_pb/pb/pb.releases");
            // launch task
            new ReadTextTask().execute(url);
        }
        catch (MalformedURLException e) {
            Log.e("PBRP", "ERROR CHECK LINE 56");
        }
    }

    protected void logic() {
        // Logic starts here
        TextView message = (TextView) findViewById(R.id.info);
        message.setText(getString(R.string.info));
        File pbrpInfo = new File(path + "/" + buildFile);

        int root = getRootAccess(message, pbrpInfo);
        int result = -2;

        if(root == 1) {
            try {
                result = checkUpdate(parseInfo(pbrpInfo), pbReleases);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        switch(result) {
            case -2 : onFailed(message); break;
            case -1 : onUnOfficial(message); break;
            case 0 : onUpdateAvailable(message); break;
            case 1 : onUpdated(message); break;
        }
    }

    private class ReadTextTask extends AsyncTask<URL, Void, String> {
        @Override
        protected String doInBackground(URL... urls) {
            try {
                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(new InputStreamReader(urls[0].openStream()));
                String s = "";

                while ((s = in.readLine()) != null) {
                    pbReleases += s;
                    Log.i("PBRELEASES:", pbReleases);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Downloaded pb.releases";
        }

        @Override
        protected void onPostExecute(String result) {
            logic();
        }
    }

    protected int getRootAccess(TextView message, File pbrpInfo) {

        Process p;
        try {
            // Preform su to get root privileges
            p = Runtime.getRuntime().exec("su");

            // Attempt to write a file to a root-only
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            File pbrp = new File(path);

            // TODO Create folder if it doesn't exists
            /*
            boolean success = true;
            Log.i("PBRP", "Starting to create folder");
            if (!pbrp.exists()) {
                Log.i("PBRP", "e1");
                success = pbrp.mkdir();
                Log.i("PBRP", "e2");
            }
            Log.i("PBRP", "Done");*/

            // Copy default.prop/prop.default to PBRP
            os.writeBytes("cp /prop.default " + path + "/" + buildFile + "\n");
            os.writeBytes("cp /default.prop " + path + "/" + buildFile + "\n");

            // Check if file successfully copied

            if(!pbrpInfo.exists()) {
                Log.e("PBRP", "Failed to copy " + buildFile + " file to PBRP folder");
                message.setText("Failed to copy" + buildFile + "file to PBRP folder");
            } else {
                message.setText(buildFile + " copied to PBRP folder");
            }

            // Close the terminal
            os.writeBytes("exit\n");
            os.flush();
            try {
                p.waitFor();
                if (p.exitValue() != 255) {
                    // TODO Code to run on success
                    return 1;
                }
                else {
                    // TODO Code to run on unsuccessful
                    message.setText("Device is not rooted.");
                    return -1;
                }
            } catch (InterruptedException e) {
                // TODO Code to run in interrupted exception
                message.setText("Device is not rooted.");
                return -1;
            }
        } catch (IOException e) {
            // TODO Code to run in input/output exception
            message.setText("Device is not rooted.");
            return -1;
        }
    }

    public static String[] parseInfo(File s) {
        String[] result = new String[2];

        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(s));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }

        Pattern ptn = Pattern.compile("ro.omni.version=[^\\n\\t-]+-(\\d+)-([^\\n\\t-]+)");
        Matcher matchPtn = ptn.matcher(text);

        if(matchPtn.find()) {
            result[1] = matchPtn.group(1);
            result[0] = matchPtn.group(2);
        }
        Log.i("PBRP", Arrays.toString(result));

        return result;
    }

    protected static int checkUpdate(String[] input, String src) throws ParseException, IOException {

        JSONParser parser = new JSONParser();
        Reader reader = null;

        Object jsonObj = null;
        jsonObj = parser.parse(src);

        JSONObject release = (JSONObject) jsonObj;

        if(release.containsKey(input[0])) {
            if(release.get(input[0]).equals(input[1])) {
                // Up to Date
                return 1;
            } else {
                // Update available
                return 0;
            }

        } else {
            // UnOfficial
            return -1;
        }
    }

    private void onFailed(TextView message) {
        message.setText("Failed to check for updates.");
    }

    private void onUnOfficial(TextView message) {
        message.setText("Your device is UnOfficial. Keep your hands off this app!");
    }

    private void onUpdateAvailable(TextView message) {
        message.setText("New Update is available for your device.");
    }

    private void onUpdated(TextView message) {
        message.setText("Congrats! Your device is Up to Date.");
    }
}
