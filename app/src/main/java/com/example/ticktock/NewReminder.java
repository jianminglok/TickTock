package com.example.ticktock;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class NewReminder extends AppCompatActivity {
    Button done;
    String url;

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_reminder);
        Spinner implevel = findViewById(R.id.importanceLevel);
        EditText rmdName = findViewById(R.id.reminderName);
        EditText notes = findViewById(R.id.notes);
        EditText rmdTime = findViewById(R.id.time);
        EditText rmdDate = findViewById(R.id.date);
        Button done = findViewById(R.id.finishActivity);
        url = "http://10.0.88.248/TickTock_php/whatever.php";


        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.reminder_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        implevel.setAdapter(adapter);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!rmdDate.getText().toString().isEmpty() && !rmdName.getText().toString().isEmpty()
                        && !rmdTime.getText().toString().isEmpty()) {
                    try {

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        String rmdDateTime = rmdDate.getText().toString() + " " + rmdTime.getText().toString();
                        Date date = sdf.parse(rmdDateTime);
                        String newRmdName = rmdName.getText().toString();
                        String newimplevel = String.valueOf(implevel.getSelectedItemId() + 1);
                        String newnotes = notes.getText().toString();
                        Log.e(TAG, newRmdName + " " + rmdDateTime + " " + newimplevel + " " + newnotes);
                        addReminderToDB(newRmdName, rmdDateTime, newimplevel, newnotes);
                        MainActivity mainActivity = MainActivity.getInstance();
                        mainActivity.setAlarm(rmdDate.getText().toString(), rmdTime.getText().toString());
                       /*  mainActivity.sendData(newRmdName);
                        mainActivity.sendData(rmdDateTime); */

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
                finish();
            }
        });
    }

    public String[] convertToString(Date date) {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);
        String stringdate = df.format(date);
        String[] stringdt = stringdate.split(" ");
        return stringdt;


    }

    private void addReminderToDB(final String rmdName, final String rmdDateTime, final String implevel, final String notes) {

        @SuppressLint("StaticFieldLeak") AsyncTask<Integer, Void, Void> asyncTask = new AsyncTask<Integer, Void, Void>() {


            @Override
            protected Void doInBackground(Integer... reminderIds) {



                    OkHttpClient client = new OkHttpClient();

                    // add parameter in database and value into addformdatapart()
                    RequestBody requestBody = new FormBody.Builder()
                            .add("reminder_name", rmdName)
                            .add("reminder_datetime", rmdDateTime)
                            .add("importance_level", implevel)
                            .add("notes", notes)
                            .build();


                    Request request = new Request.Builder()


                            // database url
                            .url(url).post(requestBody).build();
                try {
                            Response response = client.newCall(request).execute();

                             JSONArray array = new JSONArray(response.body().string());
                            Log.e(TAG, array.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }


        };
        asyncTask.execute();

        // for later use in dialog reminder editing
    /*
    public Date convertToDate(String string)
    {
        try {
            String d = string.split(" ")[0];

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            java.util.Date date = sdf.parse(d);
            return date;

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Time convertToTime(String string)
    {

        try {
            String t = string.split(" ")[1];
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Time time = (Time) sdf.parse(t);
            return time;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
        }

        */
    }

}