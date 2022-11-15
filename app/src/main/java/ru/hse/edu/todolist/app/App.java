package ru.hse.edu.todolist.app;

import android.app.Application;

import androidx.room.Room;

import ru.hse.edu.todolist.data.TaskDatabase;

public class App extends Application {

    public static App instance;

    private TaskDatabase taskDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        taskDatabase = Room.databaseBuilder(this, TaskDatabase.class, "taskDatabase")
                .allowMainThreadQueries()
                .build();
    }

    public static App getInstance() {
        return instance;
    }

    public TaskDatabase getTaskDatabase() {
        return taskDatabase;
    }
}
