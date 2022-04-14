package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersDao

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(
    private val remindersDao: MutableList<ReminderDTO>
    ) : ReminderDataSource {


    var shouldReturnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception: something went wrong")
        }
        return Result.Success(remindersDao)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersDao.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test exception: something went wrong")
        }

        val data = remindersDao.filter { it.id == id }

        return when {
            data.isNullOrEmpty() -> {
                Result.Error("No such element with id $id found")
            }
            data.size >= 1 -> {
                Result.Error("More than one element with id $id")
            }
            else -> {
                Result.Success(data[0])
            }
        }
    }

    override suspend fun deleteAllReminders() {
        remindersDao.clear()
    }


}