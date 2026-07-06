package com.planca.app.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) = taskDao.insertTask(task)

    suspend fun update(task: Task) = taskDao.updateTask(task)

    suspend fun deleteById(id: Int) = taskDao.deleteTaskById(id)

    suspend fun clearCompleted() = taskDao.clearCompletedTasks()

    suspend fun clearCompletedByCategory(category: String) = taskDao.clearCompletedTasksByCategory(category)
}
