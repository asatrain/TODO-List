package ru.hse.edu.todolist.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.Calendar;
import java.util.Objects;

@Entity
public class Task {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String taskText;
    @TypeConverters(CalendarConverter.class)
    private Calendar taskDateTime;
    private boolean isTimeSet;
    private boolean isCompleted;
    private int notificationMinutesBefore;

    public long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskText() {
        return taskText;
    }

    public void setTaskText(String taskText) {
        this.taskText = taskText;
    }

    public Calendar getTaskDateTime() {
        return taskDateTime;
    }

    public void setTaskDateTime(Calendar taskDateTime) {
        this.taskDateTime = taskDateTime;
    }

    public boolean isTimeSet() {
        return isTimeSet;
    }

    public void setTimeSet(boolean timeSet) {
        isTimeSet = timeSet;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getNotificationMinutesBefore() {
        return notificationMinutesBefore;
    }

    public void setNotificationMinutesBefore(int notificationMinutesBefore) {
        this.notificationMinutesBefore = notificationMinutesBefore;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id &&
                isTimeSet == task.isTimeSet &&
                isCompleted == task.isCompleted &&
                notificationMinutesBefore == task.notificationMinutesBefore &&
                Objects.equals(taskText, task.taskText) &&
                Objects.equals(taskDateTime, task.taskDateTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, taskText, taskDateTime, isTimeSet, isCompleted,
                notificationMinutesBefore);
    }
}
