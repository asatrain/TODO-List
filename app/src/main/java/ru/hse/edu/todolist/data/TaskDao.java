package ru.hse.edu.todolist.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM Task ORDER BY taskDateTime")
    List<Task> getAllTasksAscending();

    @Query("SELECT * FROM Task WHERE isCompleted = 1 ORDER BY taskDateTime")
    List<Task> getCompletedTasksAscending();

    @Query("SELECT * FROM Task WHERE isCompleted = 0 ORDER BY taskDateTime")
    List<Task> getUncompletedTasksAscending();

    @Query("SELECT * FROM Task ORDER BY taskDateTime DESC")
    List<Task> getAllTasksDescending();

    @Query("SELECT * FROM Task WHERE isCompleted = 1 ORDER BY taskDateTime DESC")
    List<Task> getCompletedTasksDescending();

    @Query("SELECT * FROM Task WHERE isCompleted = 0 ORDER BY taskDateTime DESC")
    List<Task> getUncompletedTasksDescending();

    @Query("SELECT * FROM Task WHERE id = :id")
    Task getById(Long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Task task);

    @Delete
    void delete(Task task);
}
