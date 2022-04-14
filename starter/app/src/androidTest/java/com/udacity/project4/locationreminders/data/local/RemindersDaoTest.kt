package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun close() = database.close()

    @Test
    fun insertReminder() = runBlockingTest {

        val reminder = ReminderDTO("place to go", "description", "location", 0.0, 0.0, "TestId")
        database.reminderDao().saveReminder(reminder)

        val loaded = database.reminderDao().getReminderById("TestId")

        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }


    @Test
    fun updateReminder() = runBlockingTest {

        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0, "TestId2")
        database.reminderDao().saveReminder(reminder)

        val reminderNew = ReminderDTO("title_new", "description_new", "location_new", -11.0, -11.0, "TestId2")
        database.reminderDao().saveReminder(reminderNew)


        val loaded = database.reminderDao().getReminderById("TestId2")


        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminderNew.id))
        assertThat(loaded.title, `is`(reminderNew.title))
        assertThat(loaded.description, `is`(reminderNew.description))
        assertThat(loaded.location, `is`(reminderNew.location))
        assertThat(loaded.latitude, `is`(reminderNew.latitude))
        assertThat(loaded.longitude, `is`(reminderNew.longitude))
    }
}