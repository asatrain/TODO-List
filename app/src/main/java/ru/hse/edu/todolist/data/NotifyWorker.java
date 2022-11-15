package ru.hse.edu.todolist.data;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import ru.hse.edu.todolist.R;
import ru.hse.edu.todolist.app.MainActivity;

public class NotifyWorker extends Worker {

    public NotifyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        NotificationChannel notificationChannel = new NotificationChannel("REMINDER",
                "ReminderChannel", NotificationManager.IMPORTANCE_HIGH);
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(notificationChannel);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 1,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                notificationChannel.getId())
                .setSmallIcon(R.drawable.ic_baseline_circle_notifications)
                .setContentTitle(getInputData().getString("title"))
                .setContentText(getInputData().getString("text"))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(Integer.parseInt(getTags().iterator().next()), notification);

        return Result.success();
    }
}
