package com.example.ticktock;

import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Reminder {

    public int id;
    public String rmdName;
    public Date rmdDate;
    public String implevel;
    public String notes;
    public String rmdDateString;
    public String rmdTimeString;
    public String rmdDateTime;


    // function used in getReminderFromDB funcName(argument)
    public Reminder(int id, String rmdName, String rmdDateTime, String rmdDateString, String rmdTimeString, Date rmdDate, String implevel, String notes) {

        this.id = id;
        this.rmdName = rmdName;
        this.rmdDate = rmdDate;
        this.implevel = implevel;
        this.notes = notes;
        this.rmdDateString = rmdDateString;
        this.rmdDateTime = rmdDateTime;
        this.rmdTimeString = rmdTimeString;

    }

    public int getId ()
    {
        return id;
    }


    public String getRmdName()
    {
        return rmdName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRmdDate() {
        return rmdDateString;
    }

    public String getRmdTime(){return rmdTimeString;}



    public String getImplevel() {
        return implevel;
    }

    public String getNotes() {
        return notes;
    }





    }

