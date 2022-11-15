package ru.hse.edu.todolist.data;

import androidx.room.TypeConverter;

import java.util.Calendar;

public class CalendarConverter {
    @TypeConverter
    public static Calendar toCalendar(Long l) {
        if (l == null)
            return null;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(l);
        return c;
    }

    @TypeConverter
    public static Long fromCalendar(Calendar c) {
        return c == null ? null : c.getTime().getTime();
    }
}
