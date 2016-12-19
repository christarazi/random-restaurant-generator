package com.chris.randomrestaurantgenerator.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * This fragment opens a TimePicker dialog for the user to enter the time.
 * Used when the user wants to filter listings for the time they're open at.
 */
public class TimePickerFragment extends DialogFragment
        implements TimePickerDialog.OnTimeSetListener {

    private TimePickerCallbacks listener;

    public void setListener(TimePickerCallbacks listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current time as the default values for the picker.
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Create a new instance of TimePickerDialog and return it.
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        df.setTimeZone(TimeZone.getDefault());

        final Calendar c = Calendar.getInstance();
        int month = c.get(Calendar.MONTH) + 1;      // It is zero-based so add 1.
        int day = c.get(Calendar.DAY_OF_MONTH);
        int year = c.get(Calendar.YEAR);

        Date date;
        try {
            Log.d("RRG", "onTimeSet: " + String.format("%02d/%02d/%04d %02d:%02d:00",
                    month, day, year, hourOfDay, minute));

            date = df.parse(String.format(Locale.US, "%02d/%02d/%04d %02d:%02d:00",
                    month, day, year, hourOfDay, minute));

            listener.timePickerDataCallback(date.getTime() / 1000);

            Log.d("RRG", "onTimeSet: " + date.getTime() / 1000);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        dialog.cancel();
        listener.timePickerDataCallback(0);
    }

    public interface TimePickerCallbacks {
        void timePickerDataCallback(long data);
    }
}

