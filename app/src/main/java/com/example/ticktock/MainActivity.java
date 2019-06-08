package com.example.ticktock;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.CalendarContract;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.ticktock.ReminderAdapter.REQUEST_CODE;


public class MainActivity extends AppCompatActivity {

    static MainActivity instance;

    //  THE MAIN LIST OF REMINDERS


    public String url;
    public String blAddress = "98:D3:61:FD:36:2A";
    RecyclerView rmdList;
    Button newRmd;
    private static final String TAG = MainActivity.class.getSimpleName();

    //  #create database
    private List<Reminder> reminders;
    private GridLayoutManager gridLayout;
    private ReminderAdapter adapter;

    private BluetoothDevice mmDevice;
    private BluetoothSocket mmSocket;
    private OutputStream mmOutputStream;
    private InputStream mmInputStream;
    private BluetoothAdapter mBluetoothAdapter;
    public Set<BluetoothDevice> pairedDevices;
    private Thread workerThread;
    byte[] readBuffer;
    private Thread bluetoothThread;
    String received;

    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);
        url = "http://192.168.88.11/TickTock_php/conn.php";
        try {
            findBT2();
            openBT2();


        }
        catch (IOException e) {
            e.printStackTrace();
        }

        rmdList = findViewById(R.id.reminderList);
        newRmd = findViewById(R.id.newReminder);

        rmdList.setHasFixedSize(true);
        // set array for the list of reminders
        reminders = new ArrayList<>();

        // get the list from database
        getRemindersFromDB(0);

        // according to the list insert reminder into the grid
        gridLayout = new GridLayoutManager(this, 2);
        rmdList.setLayoutManager(gridLayout);

        // for viewholder
        adapter = new ReminderAdapter(MainActivity.this, reminders);
        rmdList.setAdapter(adapter);

        // load new reminders when scrolled
        rmdList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rmdList, int dx, int dy)
            // if reminders scrolled to the end, retrieve more from database
            {
                if (gridLayout.findLastCompletelyVisibleItemPosition() == reminders.size() - 1) {
                    getRemindersFromDB(reminders.get(reminders.size() - 1).getId());

                }
            }

        });


        // to set a new reminder head to the next activity
        newRmd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, NewReminder.class));
            }
        });

        instance = this;


    }

    public void setAlarm(String date, String time)
    {

        String month = date.split("-")[1];
        int intmonth = Integer.parseInt(month);
        String day = date.split("-")[2];
        int intday = Integer.parseInt(day);
        String hour = time.split(":")[0];
        int inthour = Integer.parseInt(hour);
        String minute = time.split(":")[1];
        int intmin = Integer.parseInt(minute);
        String second = time.split(":")[2];
        int intsec = Integer.parseInt(second);

        Log.e(TAG, month + " " + day + " " + hour + " " + minute + " " + second);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent=new Intent(MainActivity.this,rmdAlarm.class);
        //creating a pending intent using the intent
        PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 110,intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR),intmonth, intday, inthour, intmin, intsec);

        am.set(AlarmManager.RTC, calendar.getTimeInMillis(), pi);

        Toast.makeText(this, "Alarm set in " + (calendar.getTimeInMillis() - System.currentTimeMillis()) + " seconds",Toast.LENGTH_LONG).show();

        /* String data = readData();

        if (data == "true")
        {
            am.cancel(pi);
        }

        Toast.makeText(MainActivity.this, "Reminder dismissed" ,Toast.LENGTH_SHORT).show(); */


    }




    void openBT2() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        readData();
        Toast.makeText(MainActivity.this, "Bluetooth worked", Toast.LENGTH_SHORT).show();

    }


    public void closeBT2() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(MainActivity.this, "Bluetooth Closed", Toast.LENGTH_SHORT).show();
    }

    public static MainActivity getInstance()

    {
        return instance;
    }


    void findBT2() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "No bluetooth adaptor found", Toast.LENGTH_SHORT).show();
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        pairedDevices = mBluetoothAdapter.getBondedDevices();
        // Ask Leong about the address of the bluetooth device of Arduino
        mmDevice = (BluetoothDevice) mBluetoothAdapter.getRemoteDevice(blAddress);

        if (pairedDevices.contains(mmDevice)) {
            Toast.makeText(MainActivity.this, "Blutooth device found: " + mmDevice.getAddress(), Toast.LENGTH_LONG).show();

        }

    }


        // Retrieve data from database
        private void getRemindersFromDB (int id){
            @SuppressLint("StaticFieldLeak") AsyncTask<Integer, Void, Void> asyncTask = new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... reminderIds) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            // database url
                            .url("http://10.0.88.248/TickTock_php/conn.php?id=" + reminderIds[0])
                            .build();

                    try {
                        Response response = client.newCall(request).execute();

                        JSONArray array = new JSONArray(response.body().string());

                        for (int i = 0; i < array.length(); i++) {

                            JSONObject object = array.getJSONObject(i);
                            Date date = convertToDate(object.getString("reminder_datetime"));
                            String dateString = splitTimeDate(object.getString("reminder_datetime"))[0];
                            String timeString = splitTimeDate(object.getString("reminder_datetime"))[1];

                            Reminder reminder = new Reminder(object.getInt("id"), object.getString("reminder_name"),
                                    object.getString("reminder_datetime"), dateString, timeString, date,
                                    object.getString("imp_level"), object.getString("notes"));


                            MainActivity.this.reminders.add(reminder);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                    return null;

                }

                protected void onPostExecute(Void aVoid) {
                    adapter.notifyDataSetChanged();
                }
            };
            asyncTask.execute(id);
        }


        public Date convertToDate (String string){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date date = sdf.parse(string);
                return date;

            } catch (ParseException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void sendData (String i) throws IOException {
                // Output of data sent from this device to other device


                byte[] buffer = i.getBytes();
                mmOutputStream.write(buffer);



    }


            public String[] splitTimeDate (String string){
                String[] separated = string.split(" ");
                return separated;
            }




    public String readData(){
        final Handler handler = new Handler();
        final byte delimter = 10;

        readBufferPosition = 0;
        readBuffer = new byte [1024];
        bluetoothThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()){
                try {
                    int bytesAvailable = mmInputStream.available();
                    if (bytesAvailable > 0){
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInputStream.read(packetBytes);
                        for(int i = 0; i < bytesAvailable; i++)
                        {
                            byte b = packetBytes[i];
                            if (b == delimter)
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                readBufferPosition = 0;

                                handler.post(() ->
                                {
                                    received = data;

                                });

                            }


                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });
        byte[] encodedBytes = new byte[readBufferPosition];
        final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
        return data;
    }

    private void addReminderToDB(final String rmdName, final String rmdDateTime, final String implevel, final String notes) {

        @SuppressLint("StaticFieldLeak") AsyncTask<Integer, Void, Void> asyncTask = new AsyncTask<Integer, Void, Void>() {


            @Override
            protected Void doInBackground(Integer... reminderIds) {

                OkHttpClient client = new OkHttpClient();

                // add parameter in database and value into addformdatapart()
                RequestBody requestBody = new FormBody.Builder().add("reminder_name", rmdName)
                        .add("reminder_datetime", rmdDateTime)
                        .add("importance_level", implevel)
                        .add("notes", notes)
                        .build();


                Request request = new Request.Builder()


                        // database url
                        .url("").post(requestBody).build();
                try {
                    Response response = client.newCall(request).execute();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }


        };
    }


    }


