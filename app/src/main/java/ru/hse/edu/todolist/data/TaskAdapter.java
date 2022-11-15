package ru.hse.edu.todolist.data;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import ru.hse.edu.todolist.R;
import ru.hse.edu.todolist.app.App;
import ru.hse.edu.todolist.app.MainActivity;
import ru.hse.edu.todolist.app.TaskActivity;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private final MainActivity recyclerViewActivity;
    private List<Task> tasks;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final CardView cardViewTask;
        private final Button buttonEdit;
        private final ImageButton buttonDelete;
        private final CheckBox checkBoxIsDone;
        private final TextView textViewTaskText;
        private final TextView textViewTaskDateTime;

        public ViewHolder(View itemView) {
            super(itemView);

            cardViewTask = itemView.findViewById(R.id.cardViewTask);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            checkBoxIsDone = itemView.findViewById(R.id.checkBoxIsDone);
            textViewTaskText = itemView.findViewById(R.id.textViewTaskText);
            textViewTaskDateTime = itemView.findViewById(R.id.textViewTaskDateTime);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
        }
    }

    public static class OffsetDecoration extends RecyclerView.ItemDecoration {
        private final int bottomOffset;
        private final int topOffset;

        public OffsetDecoration(float itemVerticalMargin, float bottomOffset) {
            this.bottomOffset = (int) bottomOffset;
            this.topOffset = (int) itemVerticalMargin;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int dataSize = state.getItemCount();
            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.top = topOffset;
            }
            if (position == dataSize - 1) {
                outRect.bottom = bottomOffset;
            }
        }
    }

    public TaskAdapter(MainActivity recyclerViewActivity) {
        this.recyclerViewActivity = recyclerViewActivity;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = new ArrayList<>();
        if (recyclerViewActivity.isHideOverdue()) {
            Calendar now = Calendar.getInstance();
            for (Task task : tasks) {
                if (task.getTaskDateTime() == null || now.before(task.getTaskDateTime())) {
                    this.tasks.add(task);
                }
            }
        } else {
            this.tasks.addAll(tasks);
        }
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent,
                false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if (Calendar.getInstance().after(tasks.get(position).getTaskDateTime())) {
            holder.cardViewTask.setCardBackgroundColor(ContextCompat.getColor(recyclerViewActivity.getApplicationContext(), R.color.colorOverdue));
        } else {
            holder.cardViewTask.setCardBackgroundColor(Color.WHITE);
        }
        holder.buttonEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemClicked(holder.getAdapterPosition());
            }
        });

        holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onItemDelete(holder, holder.checkBoxIsDone,
                        holder.buttonEdit, holder.buttonDelete);
            }
        });

        holder.checkBoxIsDone.setOnCheckedChangeListener(null);
        holder.checkBoxIsDone.setChecked(tasks.get(position).isCompleted());
        holder.checkBoxIsDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onItemCheckedChanged(holder, buttonView,
                        holder.buttonEdit, holder.buttonDelete, isChecked);
            }
        });

        holder.textViewTaskText.setText(tasks.get(position).getTaskText());

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, MMM dd, yyyy",
                Locale.ENGLISH);
        if (tasks.get(position).getTaskDateTime() != null) {
            holder.textViewTaskDateTime.setText(simpleDateFormat.format(tasks.get(position).getTaskDateTime().getTime()));
        } else {
            holder.textViewTaskDateTime.setText(null);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    private void onItemClicked(int position) {
        Intent intent = new Intent(recyclerViewActivity, TaskActivity.class);
        intent.putExtra("taskId", tasks.get(position).getId());
        recyclerViewActivity.startActivity(intent);
    }

    private void onItemCheckedChanged(final ViewHolder holder, final CompoundButton checkBox,
                                      final Button buttonEdit, final ImageButton buttonDelete,
                                      boolean isChecked) {
        final Task task = tasks.get(holder.getAdapterPosition());
        task.setCompleted(isChecked);
        task.setId(App.getInstance().getTaskDatabase().taskDao().insert(task));
        if (!task.isCompleted() && task.getNotificationMinutesBefore() > 0) {
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
                WorkManager.getInstance(recyclerViewActivity.getApplicationContext()).enqueue(oneTimeWorkRequest);
            }
        } else {
            WorkManager.getInstance(recyclerViewActivity.getApplicationContext()).cancelAllWorkByTag(String.valueOf(task.getId()));
        }
        int filterId = recyclerViewActivity.getCurrentFilterId();
        boolean itemOutOfFilter =
                filterId == R.id.action_show_completed && !isChecked || filterId == R.id.action_show_uncompleted && isChecked;
        if (itemOutOfFilter) {
            Handler handler = new Handler(Looper.getMainLooper());
            Runnable removeItem = new Runnable() {
                @Override
                public void run() {
                    checkBox.setEnabled(true);
                    buttonEdit.setEnabled(true);
                    buttonDelete.setEnabled(true);
                    tasks.remove(task);
                    notifyDataSetChanged();
                }
            };
            checkBox.setEnabled(false);
            buttonEdit.setEnabled(false);
            buttonDelete.setEnabled(false);
            handler.postDelayed(removeItem, 400);
        }
    }

    private void onItemDelete(final ViewHolder holder, final CompoundButton checkBox,
                              final Button buttonEdit, final ImageButton buttonDelete) {
        AlertDialog alertDialog = new AlertDialog.Builder(recyclerViewActivity)
                .setTitle("Are you sure?")
                .setMessage("Delete task?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteItem(holder, checkBox, buttonEdit, buttonDelete);
                    }
                })
                .setNegativeButton("CANCEL", null)
                .create();
        alertDialog.show();
    }

    private void deleteItem(final ViewHolder holder, final CompoundButton checkBox,
                            final Button buttonEdit, final ImageButton buttonDelete) {
        final Task task = tasks.get(holder.getAdapterPosition());
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable removeItem = new Runnable() {
            @Override
            public void run() {
                checkBox.setEnabled(true);
                buttonEdit.setEnabled(true);
                buttonDelete.setEnabled(true);
                tasks.remove(task);
                notifyDataSetChanged();
            }
        };
        checkBox.setEnabled(false);
        buttonEdit.setEnabled(false);
        buttonDelete.setEnabled(false);
        App.getInstance().getTaskDatabase().taskDao().delete(task);
        WorkManager.getInstance(recyclerViewActivity.getApplicationContext()).cancelAllWorkByTag(String.valueOf(task.getId()));

        handler.postDelayed(removeItem, 250);
    }
}
