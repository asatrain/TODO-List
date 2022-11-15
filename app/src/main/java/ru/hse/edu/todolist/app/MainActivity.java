package ru.hse.edu.todolist.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ru.hse.edu.todolist.R;
import ru.hse.edu.todolist.data.TaskAdapter;
import ru.hse.edu.todolist.data.TaskDao;

public class MainActivity extends AppCompatActivity {

    private int currentFilterId;
    private int currentSortId;
    private boolean hideOverdue;

    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;

    public int getCurrentFilterId() {
        return currentFilterId;
    }

    public boolean isHideOverdue() {
        return hideOverdue;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(this);
        tasksRecyclerView.setAdapter(taskAdapter);
        float itemVerticalMargin = getResources().getDimension(R.dimen.item_vertical_margin);
        float bottomOffset = getResources().getDimension(R.dimen.last_item_bottom_offset);
        tasksRecyclerView.addItemDecoration(new TaskAdapter.OffsetDecoration(itemVerticalMargin,
                bottomOffset));

        SharedPreferences settings = getSharedPreferences("settings", MODE_PRIVATE);
        hideOverdue = settings.getBoolean("hideOverdue", false);
        currentFilterId = settings.getInt("filterId", R.id.action_show_all);
        currentSortId = settings.getInt("sortId", R.id.action_sort_ascending);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refresh();
    }

    private void refresh() {
        TaskDao taskDao = App.getInstance().getTaskDatabase().taskDao();
        if (currentFilterId == R.id.action_show_all) {
            getSupportActionBar().setTitle("All tasks");
            taskAdapter.setTasks(currentSortId == R.id.action_sort_ascending ?
                    taskDao.getAllTasksAscending() : taskDao.getAllTasksDescending());
        } else if (currentFilterId == R.id.action_show_completed) {
            getSupportActionBar().setTitle("Completed");
            taskAdapter.setTasks(currentSortId == R.id.action_sort_ascending ?
                    taskDao.getCompletedTasksAscending() : taskDao.getCompletedTasksDescending());
        } else if (currentFilterId == R.id.action_show_uncompleted) {
            getSupportActionBar().setTitle("Uncompleted");
            taskAdapter.setTasks(currentSortId == R.id.action_sort_ascending ?
                    taskDao.getUncompletedTasksAscending() :
                    taskDao.getUncompletedTasksDescending());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        menu.findItem(R.id.hideOverdue).setChecked(hideOverdue);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_show_all || id == R.id.action_show_completed ||
                id == R.id.action_show_uncompleted || id == R.id.action_sort_ascending ||
                id == R.id.action_sort_descending) {
            currentFilterId = id;
            refresh();
        } else if (id == R.id.hideOverdue) {
            hideOverdue = !hideOverdue;
            item.setChecked(hideOverdue);
            refresh();
        } else if (id == R.id.about) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setMessage("\nTODO List\n\nAuthor - Asatryan Emin");
            dialog.setTitle("About");
            dialog.setNeutralButton("OK", null);
            dialog.setIcon(R.mipmap.ic_launcher_round);
            AlertDialog alertDialog = dialog.create();
            alertDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    public void addTask(View view) {
        Intent intent = new Intent(this, TaskActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor settingsEditor =
                getSharedPreferences("settings", MODE_PRIVATE).edit();
        settingsEditor.putBoolean("hideOverdue", hideOverdue);
        settingsEditor.putInt("filterId", currentFilterId);
        settingsEditor.putInt("sortId", currentSortId);
        settingsEditor.apply();
    }
}