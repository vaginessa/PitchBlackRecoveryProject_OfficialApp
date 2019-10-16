package com.github.pitchblackrecoveryproject.pitchblackrecoveryproject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Environment;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.widget.Toast;
import android.Manifest;
import android.support.v4.app.ActivityCompat;

import com.stericson.RootShell.*;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout dl;
    private ActionBarDrawerToggle t;
    private NavigationView nv;
    private Button updateBtn;
    private ProgressDialog mProgressDialog;

    final static String path = Environment.getExternalStorageDirectory() + File.separator + "PBRP";
    final static String buildFile = "pbrp.info";
    private int root = -1;

    private final String repoBranch = "test";
    private String latestPbrpVersion = "2.9.0";
    private int STORAGE_PERMISSION_CODE = 1;

    private String deviceCodeName = "";
    private String deviceBuildDate = "";
    private String pbReleasesRaw = "";
    private String newUpdateBuildDate = "";
    private String newUpdateBuildTime = "";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        //Toggle
        dl = findViewById(R.id.dl);
        ImageView navToggle = findViewById(R.id.navToggle);
        navToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View e) {
                dl.openDrawer(GravityCompat.START);
            }
        });

        // Check for update
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View e) {
                init();
            }
        });

        // Update Button
        updateBtn = findViewById(R.id.updateBtn);
        updateBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View e) {
                final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
                downloadTask.execute("https://master.dl.sourceforge.net/project/pitchblack-twrp/" + deviceCodeName +"/PitchBlack-" + deviceCodeName + "-" + latestPbrpVersion + "-" + newUpdateBuildDate + "-" + newUpdateBuildTime +"-OFFICIAL.zip");

                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true); //cancel the task
                    }
                });
            }
        });

        //Permissions
        requestStoragePermission();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast toast = Toast.makeText(getApplicationContext(), "PBRP: Storage Permission Denied", Toast.LENGTH_LONG);
            toast.show();
        }
        if (!RootShell.isRootAvailable()) {
            Toast toast = Toast.makeText(getApplicationContext(), "PBRP: Update Features will not work \n Reason: Non Rooted", Toast.LENGTH_LONG);
            Log.e("PBRP: ", "Root not Found");
            toast.show();
        }
        if (!RootShell.isAccessGiven()) {
            Toast toast = Toast.makeText(getApplicationContext(), "PBRP: Root not Granted", Toast.LENGTH_LONG);
            Log.e("PBRP: ", "Root not Granted");
            toast.show();
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                // create dir
                File out = new File(path + "/" + newUpdateBuildDate + "-" + newUpdateBuildTime);
                out.mkdirs();

                output = new FileOutputStream(path + "/" + newUpdateBuildDate + "-" + newUpdateBuildTime + "/" + newUpdateBuildDate + "-" + newUpdateBuildTime + ".zip");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.setMessage("Downloading Latest Build...\nPlease Wait...");
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else
                update();
        }
    }

    private void update() {
        // TODO: unzip and flash recovery and update PBRP folders...
        final ProgressDialog Updater = new ProgressDialog(this);
        Updater.setTitle("Initializing Update");
        Updater.setMessage("Preparing to update...\nPlease wait...");
        Updater.show();
        boolean recovery_flashable = false;

        if (unpackZip(path + "/" + newUpdateBuildDate + "-" + newUpdateBuildTime + "/", newUpdateBuildDate + "-" + newUpdateBuildTime + ".zip") && installPBRP()) {
            Updater.dismiss();
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            final AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder.setTitle("Update Ready");
            builder.setMessage("A new update (" + newUpdateBuildDate + ") is ready to be installed, please make sure you have the backup of the recovery if anything goes wrong. Do you want to flash the recovery?");
            builder.setCancelable(false);

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!(sudo("dd if=/dev/block/bootdevice/by-name/recovery of=" + path + "/recovery-backup.img"))) {
                        builder2.setTitle("Failed to Backup current Recovery Image");
                        builder2.setMessage("Issue encountered while backing up current image");
                        builder2.setCancelable(true);
                        builder2.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           @Override
                            public void onClick(DialogInterface dialog, int which) {
                                recovery_flashable = true;
                            }
                        }
                    }
                    if (recovery_flashable && sudo("dd if=" + path + "/" + newUpdateBuildDate + "-" + newUpdateBuildTime + "/TWRP/recovery.img of=/dev/block/bootdevice/by-name/recovery")) {
                        builder2.setTitle("Updated Successfully");
                        builder2.setMessage("PBRP succussfully updated. Do you want to reboot to recovery now?");
                        builder2.setCancelable(false);
                        builder2.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sudo("reboot recovery");
                            }
                        });

                        builder2.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(getApplicationContext(), "Update will be completed only if it will be rebooted to recovery.", Toast.LENGTH_LONG).show();
                            }
                        });

                        builder2.show();

                    } else {
                        builder2.setTitle("Update Failed");
                        builder2.setMessage("There was some problem installing the reovery. Please send log report to the developers @pbrpcom via telegram.");
                        builder2.setCancelable(true);
                        builder2.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // nothing
                            }
                        });
                        builder2.show();
                    }
                }
            });

            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(getApplicationContext(), "Update Cancelled.", Toast.LENGTH_SHORT).show();
                }
            });

            builder.show();

        } else {
            Updater.dismiss();
            final AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setTitle("Update Failed");
            builder2.setMessage("There is some problem with the update package. Please try again or Contact developers @pbrpcom via telegram.");
            builder2.setCancelable(true);
            builder2.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // nothing
                }
            });
            builder2.show();
        }
    }

    private boolean unpackZip(String spath, String zipname)
    {
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(spath + zipname);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(spath + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(spath + filename);

                while ((count = zis.read(buffer)) != -1)
                {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private Boolean installPBRP() {
        // TODO: Install Scripts

        return true;
    }

    protected void init() {
        updateBtn.setVisibility(View.GONE);
        try {
            setMessage(getString(R.string.info));
            // Create a URL for the desired page
            URL url = new URL("https://raw.githubusercontent.com/PitchBlackRecoveryProject/vendor_pb/" + repoBranch + "/pb.releases");
            // launch task
            new ReadTextTask().execute(url);
        } catch (MalformedURLException e) {
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
            result = checkUpdate(parseInfo(pbrpInfo), pbReleasesRaw);
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        switch (result) {
            case -2:
                onFailed(message);
                break;
            case -1:
                onUnOfficial(message);
                break;
            case 0:
                onUpdateAvailable(message);
                break;
            case 1:
                onUpdated(message);
                break;
        }
    }

    private class ReadTextTask extends AsyncTask<URL, Void, Integer> {
        @Override
        protected Integer doInBackground(URL... urls) {
            try {
                // Read all the text returned by the server
                BufferedReader in = new BufferedReader(new InputStreamReader(urls[0].openStream()));
                pbReleasesRaw = "";
                String s = "";

                while ((s = in.readLine()) != null) {
                    pbReleasesRaw += s;
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
            if (result == 1) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        Pattern ptn = Pattern.compile("[^\\n\\t-]+-(\\d+)-([^\\n\\t-]+)");
        Matcher matchPtn = ptn.matcher(text);

        if (matchPtn.find()) {
            result[1] = matchPtn.group(1);
            result[0] = matchPtn.group(2);
        }

        return result;
    }

    protected int checkUpdate(String[] input, String src) throws ParseException, IOException {

        JSONParser parser = new JSONParser();
        Reader reader = null;

        Object jsonObj = parser.parse(src);

        JSONObject release = (JSONObject) jsonObj;

        deviceCodeName = input[0];
        deviceBuildDate = input[1];

        if (release.containsKey(input[0])) {
            if (release.get(input[0]).equals(input[1])) {
                // Up to Date
                return 1;
            } else {
                // TODO Fix this
                int mon[] = new int[2], date[] = new int[2], yr[] = new int[2];
                yr[0] = Integer.parseInt(input[1].substring(0, 4));
                mon[0] = Integer.parseInt(input[1].substring(4, 6));
                date[0] = Integer.parseInt(input[1].substring(6));
                yr[1] = Integer.parseInt(release.get(input[0]).toString().split("-")[0].substring(0, 4));
                mon[1] = Integer.parseInt(release.get(input[0]).toString().split("-")[0].substring(4, 6));
                date[1] = Integer.parseInt(release.get(input[0]).toString().split("-")[0].substring(6));
                if (yr[1] >= yr[0])
                    if (mon[1] >= mon[0])
                        if (date[1] > date[0] || mon[1] > mon[0] || yr[1] > yr[0]) {
                            // Update available
                            this.newUpdateBuildDate = release.get(input[0]).toString().split("-")[0];
                            this.newUpdateBuildTime = release.get(input[0]).toString().split("-")[1];
                            return 0;
                        }
                return 1;
            }

        } else {
            // UnOfficial
            return -1;
        }
    }

    public static Boolean sudo(String...strings) {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            for (String s : strings) {
                outputStream.writeBytes(s+"\n");
                outputStream.flush();
            }

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            outputStream.close();
            return true;
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
    }

    private void onFailed(TextView message) {
        message.setText("Failed to check for updates.");
    }

    private void onUnOfficial(TextView message) {
        message.setText("Your device is Unofficial. Keep your hands off this app!");
    }

    private void onUpdateAvailable(TextView message) {
        message.setText("New Update is available for your device.\n PBRP " + latestPbrpVersion + " " + newUpdateBuildDate);
        updateBtn.setVisibility(View.VISIBLE);
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
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }
}
