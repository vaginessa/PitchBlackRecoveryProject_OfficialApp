package com.github.pitchblackrecoveryproject.pitchblackrecoveryproject;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.Toast;
import android.Manifest;
import android.support.v4.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;

    private int STORAGE_PERMISSION_CODE = 1;
    final static String path = Environment.getExternalStorageDirectory() + File.separator + "PBRP";
    final static String buildFile = "pbrp.info";
    String pbReleases = "";
    int root = -1;
    String update_build = "";
    String latest_pbrp = "2.9.0";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean per=true;
        dl = findViewById(R.id.dl);
        ImageView navToggle = findViewById(R.id.navToggle);
        navToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View e) {
                dl.openDrawer(GravityCompat.START);
            }
        });

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View e) {
                init();
            }
        });
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            requestStoragePermission();
        try {
            Runtime.getRuntime().exec("su");
            while(per)
            {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    per=false;
                }
                else {
                    requestStoragePermission();
                    per = true;
                }
            }
        }
        catch (IOException e){Toast toast = Toast.makeText(getApplicationContext(), "PBRP: Update Features will not work \n Reason: Non Rooted", Toast.LENGTH_SHORT);
            //toast.setMargin(50, 50);
            Log.e("PBRP: ", "Root not Found");
            toast.show();}
    }

    protected void init() {
        try {
            setMessage(getString(R.string.info));
            // Create a URL for the desired page
            URL url = new URL("https://raw.githubusercontent.com/PitchBlackRecoveryProject/vendor_pb/pb/pb.releases");
            // launch task
            new ReadTextTask().execute(url);
        }
        catch (MalformedURLException e) {
            Log.e("PBRP: ", "ERROR MalformedURL Exception");
            e.printStackTrace();
        }
    }

    protected void logic() {
        // Logic starts here
        TextView message = (TextView) findViewById(R.id.info);
        setMessage(getString(R.string.info));
        File pbrpInfo = new File(path + "/" + buildFile);

        int result = -2;

        try {
            result = checkUpdate(parseInfo(pbrpInfo), pbReleases);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch(result) {
            case -2 : onFailed(message); break;
            case -1 : onUnOfficial(message); break;
            case 0 : onUpdateAvailable(message); break;
            case 1 : onUpdated(message); break;
        }
    }

    private class ReadTextTask extends AsyncTask<URL, Void, Integer> {
        @Override
        protected Integer doInBackground(URL... urls) {
            try {
                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(new InputStreamReader(urls[0].openStream()));
                pbReleases = "";
                String s = "";

                while ((s = in.readLine()) != null) {
                    pbReleases += s;
                    Log.i("PBRELEASES:", pbReleases);
                }

            } catch (Exception e) {
                setMessage("Failed to connect to the server,\nCheck your internet connection and try again");
                Log.e("PBRP: ", "FAILED TO CONNECT TO SERVER");
                e.printStackTrace();
                return 0;
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result == 1) {
                logic();
            }
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
            e.printStackTrace();
        }

        Pattern ptn = Pattern.compile("[^\\n\\t-]+-(\\d+)-([^\\n\\t-]+)");
        Matcher matchPtn = ptn.matcher(text);

        if(matchPtn.find()) {
            result[1] = matchPtn.group(1);
            result[0] = matchPtn.group(2);
        }

        Log.i("PBRP: ", Arrays.toString(result));

        return result;
    }

    protected int checkUpdate(String[] input, String src) throws ParseException, IOException {

        JSONParser parser = new JSONParser();
        Reader reader = null;

        Log.i("PBRP: ", src);
        Object jsonObj = parser.parse(src);

        JSONObject release = (JSONObject) jsonObj;

        if(release.containsKey(input[0])) {
            if(release.get(input[0]).equals(input[1])) {
                // Up to Date
                return 1;
            } else {
                int mon[]=new int[2], date[]=new int[2], yr[]=new int[2];
                yr[0]=Integer.parseInt(input[1].substring(0,4));
                mon[0]=Integer.parseInt(input[1].substring(4,6));
                date[0]=Integer.parseInt(input[1].substring(6));
                yr[1]=Integer.parseInt(release.get(input[0]).toString().substring(0,4));
                mon[1]=Integer.parseInt(release.get(input[0]).toString().substring(4,6));
                date[1]=Integer.parseInt(release.get(input[0]).toString().substring(6));
                if(yr[1]>=yr[0])
                    if(mon[1]>=mon[0])
                        if(date[1]>date[0] || mon[1]>mon[0] || yr[1]>yr[0]) {
                            // Update available
                            this.update_build = release.get(input[0]).toString();
                            return 0;
                        }
                return 1;
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
        message.setText("Your device is Unofficial. Keep your hands off this app!");
    }

    private void onUpdateAvailable(TextView message) {
        message.setText("New Update is available for your device.\n PBRP " + latest_pbrp + " " + update_build);
    }

    private void onUpdated(TextView message) {
        message.setText("Congrats! Your device is up-to-date.");
    }

    private void setMessage(String msg) {
        TextView message = (TextView) findViewById(R.id.info);
        message.setText(msg);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }
}
