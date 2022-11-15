package ru.hse.edu.todolist.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import ru.hse.edu.todolist.R;
import ru.hse.edu.todolist.data.NotifyWorker;
import ru.hse.edu.todolist.data.Task;

public class TaskActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener,
        TimePickerDialog.OnTimeSetListener, CompoundButton.OnCheckedChangeListener {

    private enum NotificationTimeBefore {
        NO("No", 0),
        MIN10("10 minutes before", 10),
        HOUR("1 hour before", 60),
        HOUR2("3 hours before", 180),
        HOUR12("12 hours before", 720),
        DAY("A day before", 1440),
        WEEK("A week before", 1440 * 7);

        public static int getPosByMinutes(int minutes) {
            NotificationTimeBefore[] values = values();
            for (int i = 0; i < values.length; i++) {
                if (values[i].getMinutes() == minutes)
                    return i;
            }
            return -1;
        }

        private final String friendlyName;
        private final int minutes;

        NotificationTimeBefore(String friendlyName, int minutes) {
            this.friendlyName = friendlyName;
            this.minutes = minutes;
        }

        public int getMinutes() {
            return minutes;
        }

        @Override
        public String toString() {
            return friendlyName;
        }
    }

    private EditText editTextTaskText;
    private CheckBox checkBoxTaskCompleted;
    private EditText editTextTaskDate;
    private EditText editTextTaskTime;
    private ImageButton buttonClearDate;
    private ImageButton buttonClearTime;
    private Spinner spinnerNotificationTimesBefore;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy",
            Locale.ENGLISH);

    private boolean isEditActivity;
    private Task task;
    private Task taskBeforeEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isEditActivity = getIntent().hasExtra("taskId");
        if (!isEditActivity) {
            setContentView(R.layout.activity_task2);
        } else {
            setContentView(R.layout.activity_task);
            checkBoxTaskCompleted = findViewById(R.id.checkBoxTaskCompleted);
            checkBoxTaskCompleted.setOnCheckedChangeListener(this);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        editTextTaskText = findViewById(R.id.editTextTaskText);
        editTextTaskDate = findViewById(R.id.editTextTaskDate);
        editTextTaskTime = findViewById(R.id.editTextTaskTime);
        buttonClearDate = findViewById(R.id.buttonClearDate);
        buttonClearDate.setEnabled(false);
        buttonClearTime = findViewById(R.id.buttonClearTime);
        buttonClearTime.setEnabled(false);
        spinnerNotificationTimesBefore = findViewById(R.id.spinnerNotificationTimesBefore);
        spinnerNotificationTimesBefore.setEnabled(false);

        initializeSpinnerArrayAdapter();
        spinnerNotificationTimesBefore.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                task.setNotificationMinutesBefore(((NotificationTimeBefore) parent.getItemAtPosition(position)).getMinutes());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        initializeActivityViewElements();

        checkNotificationSetCorrect();
    }

    private void initializeActivityViewElements() {
        if (isEditActivity) {
            long taskId = getIntent().getExtras().getLong("taskId");
            task = App.getInstance().getTaskDatabase().taskDao().getById(taskId);
            taskBeforeEdit = App.getInstance().getTaskDatabase().taskDao().getById(taskId);
        } else {
            task = new Task();
            taskBeforeEdit = new Task();
            taskBeforeEdit.setTaskText("");
        }

        editTextTaskText.setText(task.getTaskText());
        if (isEditActivity) {
            checkBoxTaskCompleted.setChecked(task.isCompleted());
        }
        if (task.getTaskDateTime() == null) {
            editTextTaskTime.setEnabled(false);
        } else {
            editTextTaskDate.setText(dateFormat.format(task.getTaskDateTime().getTime()));
            buttonClearDate.setEnabled(true);
            if (task.isTimeSet()) {
                editTextTaskTime.setText(timeFormat.format(task.getTaskDateTime().getTime()));
                buttonClearTime.setEnabled(true);
                spinnerNotificationTimesBefore.setEnabled(true);
                spinnerNotificationTimesBefore.setSelection(NotificationTimeBefore.getPosByMinutes(task.getNotificationMinutesBefore()));
            }
        }
    }

    private void initializeSpinnerArrayAdapter() {
        final ArrayAdapter<NotificationTimeBefore> adapter =
                new ArrayAdapter<NotificationTimeBefore>(this,
                        android.R.layout.simple_spinner_item, NotificationTimeBefore.values()) {
                    @Override
                    public boolean isEnabled(int position) {
                        if (position == 0)
                            return true;
                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.SECOND, 0);
                        long nowTime = now.getTime().getTime();
                        NotificationTimeBefore[] values = NotificationTimeBefore.values();
                        Calendar taskDateTime = task.getTaskDateTime();
                        long targetTime =
                                taskDateTime.getTime().getTime() - 60000L * values[position].getMinutes();
                        return nowTime < targetTime;
                    }

                    @Override
                    public View getDropDownView(int position, View convertView,
                                                @NonNull ViewGroup parent) {
                        View view = super.getDropDownView(position, convertView, parent);
                        TextView tv = (TextView) view;
                        if (position == 0) {
                            tv.setTextColor(Color.BLACK);
                            return view;
                        }
                        Calendar now = Calendar.getInstance();
                        now.set(Calendar.SECOND, 0);
                        long nowTime = now.getTime().getTime();
                        NotificationTimeBefore[] values = NotificationTimeBefore.values();
                        Calendar taskDateTime = task.getTaskDateTime();
                        long targetTime =
                                taskDateTime.getTime().getTime() - 60000L * values[position].getMinutes();

                        if (nowTime < targetTime) {
                            tv.setTextColor(Color.BLACK);
                        } else {
                            tv.setTextColor(Color.GRAY);
                        }
                        return view;
                    }
                };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNotificationTimesBefore.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isEditActivity) {
            getMenuInflater().inflate(R.menu.activity_task_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Are you sure?")
                    .setMessage("Delete task?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            App.getInstance().getTaskDatabase().taskDao().delete(taskBeforeEdit);
                            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(String.valueOf(taskBeforeEdit.getId()));
                            finish();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .create();
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        task.setCompleted(isChecked);
        buttonView.setText(task.isCompleted() ? "Task is completed!" :
                "Is task completed?");
    }

    public void setDate(View view) {
        Calendar dateTime = task.getTaskDateTime() != null ? task.getTaskDateTime() :
                Calendar.getInstance();
        new DatePickerDialog(this, this, dateTime.get(Calendar.YEAR),
                dateTime.get(Calendar.MONTH), dateTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    public void setTime(View view) {
        Calendar dateTime = task.isTimeSet() ? task.getTaskDateTime() : Calendar.getInstance();
        new TimePickerDialog(this, this, dateTime.get(Calendar.HOUR_OF_DAY),
                dateTime.get(Calendar.MINUTE), true).show();
    }

    public void clearDate(View view) {
        task.setTaskDateTime(null);
        editTextTaskDate.setText(null);
        editTextTaskTime.setEnabled(false);
        buttonClearDate.setEnabled(false);
        clearTime(view);
    }

    public void clearTime(View view) {
        task.setTimeSet(false);
        editTextTaskTime.setText(null);
        buttonClearTime.setEnabled(false);
        spinnerNotificationTimesBefore.setSelection(0);
        spinnerNotificationTimesBefore.setEnabled(false);
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        if (task.getTaskDateTime() == null) {
            Calendar dateTime = Calendar.getInstance();
            dateTime.set(year, month, dayOfMonth, 0, 0, 0);
            task.setTaskDateTime(dateTime);
            editTextTaskTime.setEnabled(true);
        } else {
            task.getTaskDateTime().set(year, month, dayOfMonth);
        }

        editTextTaskDate.setText(dateFormat.format(task.getTaskDateTime().getTime()));
        buttonClearDate.setEnabled(true);
        checkNotificationSetCorrect();
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        task.getTaskDateTime().set(Calendar.HOUR_OF_DAY, hourOfDay);
        task.getTaskDateTime().set(Calendar.MINUTE, minute);
        task.setTimeSet(true);

        editTextTaskTime.setText(timeFormat.format(task.getTaskDateTime().getTime()));
        buttonClearTime.setEnabled(true);
        spinnerNotificationTimesBefore.setEnabled(true);
        checkNotificationSetCorrect();
    }

    private void checkNotificationSetCorrect() {
        if (task.getNotificationMinutesBefore() > 0) {
            Calendar now = Calendar.getInstance();
            long nowTime = now.getTime().getTime();
            long targetTime = task.getTaskDateTime().getTime().getTime();
            if (nowTime > targetTime) {
                spinnerNotificationTimesBefore.setSelection(0);
                task.setNotificationMinutesBefore(0);
            }
        }
    }

    public void acceptTask(View view) {
        task.setTaskText(editTextTaskText.getText().toString());
        if (task.getTaskText().replaceAll(" ", "").isEmpty()) {
            Toast toast = Toast.makeText(this, "Task text should be not empty", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            task.setId(App.getInstance().getTaskDatabase().taskDao().insert(task));
            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(String.valueOf(task.getId()));
            if (task.getNotificationMinutesBefore() > 0 && !task.isCompleted()) {
                long targetTime =
                        task.getTaskDateTime().getTime().getTime() - 60000L * task.getNotificationMinutesBefore();
                long nowTime = Calendar.getInstance().getTime().getTime();
                if (nowTime < targetTime) {
                    Data inputData = new Data.Builder()
                            .putString("title", "Don't forget, You have a task!")
                            .putString("text", task.getTaskText())
                            .build();
                    long delay = targetTime - nowTime;
                    OneTimeWorkRequest oneTimeWorkRequest =
                            new OneTimeWorkRequest.Builder(NotifyWorker.class)
                                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                    .setInputData(inputData)
                                    .addTag(String.valueOf(task.getId()))
                                    .build();
                    WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
                }
            }
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        task.setTaskText(editTextTaskText.getText().toString());
        if (Objects.equals(taskBeforeEdit, task)) {
            finish();
        } else {
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Quit without saving?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .create();
            alertDialog.show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
