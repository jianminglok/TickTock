package com.example.ticktock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static android.content.ContentValues.TAG;
import static android.content.Context.ALARM_SERVICE;

// THE VIEWHOLDER FOR THE REMINDERS

// code im not using (yet)
// String rmdDateString = convertToString(rmdDate)[0]
// String rmdTimeString = convertToString(rmdDate)[1];




// to dismiss alarm
// alarmManager.cancel(pendingIntent);
public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ViewHolder> {

    private Context context;
    private List<Reminder> reminders;
    // request code for alarm
    public static final int REQUEST_CODE=101;


    public ReminderAdapter(Context context, List<Reminder> reminders) {
        this.context = context;
        this.reminders = reminders;

    }

    // create new viewholder(columns) for new reminders
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.card, parent, false);
        return new ViewHolder(itemView);
    }



    @Override

    // for existing reminders(string date time)


    public void onBindViewHolder(final ViewHolder holder, int position) {

        Log.e(TAG, reminders.get(position).getRmdDate());
        holder.rmdName.setText(reminders.get(position).getRmdName());
        holder.implevel.setText(reminders.get(position).getNotes());
        holder.rmdDate.setText(reminders.get(position).getRmdDate());
        holder.rmdTime.setText(reminders.get(position).getRmdTime());
        holder.implevel.setText(reminders.get(position).getImplevel());
        holder.notes.setText(reminders.get(position).getNotes());
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    // display as string
    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView rmdName;
        public TextView notes;
        public TextView implevel;
        public TextView rmdTime;
        public TextView rmdDate;
        public Button delete;

        public EditText editName;
        public EditText editDate;
        public EditText editTime;
        public EditText editNotes;


        public ViewHolder(View itemView) {

            super(itemView);

            // find view by id

            delete = itemView.findViewById(R.id.delete);
            rmdName = itemView.findViewById(R.id.reminderName2);
            rmdDate = itemView.findViewById(R.id.date2);
            rmdTime = itemView.findViewById(R.id.time2);
            implevel = itemView.findViewById(R.id.importanceLevel2);
            notes = itemView.findViewById(R.id.notes2);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
                LayoutInflater inflater = LayoutInflater.from(context);
                View dialogView = inflater.inflate(R.layout.dialog, null);
                dialogBuilder.setCancelable(true);

                editName = dialogView.findViewById(R.id.editName);
                editDate = dialogView.findViewById(R.id.editDate);
                editTime = dialogView.findViewById(R.id.editTime);
                editNotes = dialogView.findViewById(R.id.editNotes);
                Spinner editSpinner = dialogView.findViewById(R.id.editSpinner);

                editName.setText(reminders.get(position).getRmdName());
                editDate.setText(reminders.get(position).getRmdDate());
                editTime.setText(reminders.get(position).getRmdTime());
                editNotes.setText(reminders.get(position).getNotes());

                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.reminder_array, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                editSpinner.setAdapter(adapter);

                editSpinner.setSelection(Integer.parseInt(reminders.get(position).getImplevel()));

                dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                dialogBuilder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(!editDate.getText().toString().isEmpty() && !editName.getText().toString().isEmpty() && !editTime.getText().toString().isEmpty()) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                String rmdDateTime = editDate.getText().toString() + " " + editTime.getText().toString();
                                Date date = sdf.parse(rmdDateTime);
                                String newRmdName = editName.getText().toString();
                                String newnotes = editNotes.getText().toString();
                                String newimplevel = String.valueOf(editSpinner.getSelectedItemPosition());
                                addReminderToDB(reminders.get(position).getId(), newRmdName, rmdDateTime, newimplevel, newnotes);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                dialogBuilder.setView(dialogView);
                dialogBuilder.show();
            });

        }
    }

    private void addReminderToDB(final int id, final String rmdName, final String rmdDateTime, final String implevel, final String notes) {

        @SuppressLint("StaticFieldLeak") AsyncTask<Integer, Void, Void> asyncTask = new AsyncTask<Integer, Void, Void>() {


            @Override
            protected Void doInBackground(Integer... reminderIds) {

                OkHttpClient client = new OkHttpClient();

                // add parameter in database and value into addformdatapart()
                RequestBody requestBody = new FormBody.Builder()
                        .add("id", String.valueOf(id))
                        .add("reminder_name", rmdName)
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

       /*
        public void pendingIntent()
        {
            public static final int REQUEST_CODE=101;
            Intent intent=new Intent(this,MyReceiver.class);
            PendingIntent.getBroadcast(this,REQUEST_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        }
    */
/*
1st Param : Context
2nd Param : Integer request code
3rd Param : Wrapped Intent
4th Intent: Flag
*/



    }


