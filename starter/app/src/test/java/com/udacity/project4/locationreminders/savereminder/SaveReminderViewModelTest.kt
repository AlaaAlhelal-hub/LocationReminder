package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class SaveReminderViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application : Application
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var dataItem: ReminderDataItem
    private var reminders = mutableListOf<ReminderDTO>()

    @Before
    fun init() {
        stopKoin()
        application = ApplicationProvider.getApplicationContext() as Application
        dataItem = ReminderDataItem("place to go","this is for testing purpose","random location",-14.33222,-45.6555, "TestId")
        dataSource = FakeDataSource(reminders)
        viewModel = SaveReminderViewModel(application, dataSource)
    }

    @Test
    fun validateEnteredData() {

        // invalid reminder
        val invalidReminder = ReminderDataItem("", "", "", 0.0, 0.0)
        assertThat( viewModel.validateEnteredData(invalidReminder), `is`(false))

        // valid reminder
        val validReminder = dataItem
        assertThat( viewModel.validateEnteredData(validReminder), `is`(true))
    }

    @Test
    fun saveReminder() {
        val reminder = dataItem
        viewModel.validateAndSaveReminder(reminder)
        assertThat(viewModel.showToast.getOrAwaitValue(), `is`(application.getString(R.string.reminder_saved)))
        assertEquals(viewModel.navigationCommand.getOrAwaitValue(), NavigationCommand.Back)
    }

    @Test
    fun clearReminder() {

        viewModel.apply{
            reminderTitle.value = "title"
            reminderDescription.value = "description"
            reminderSelectedLocationStr.value = "location"
            latitude.value = 0.0
            longitude.value = 0.0
        }

        viewModel.onClear()

        viewModel.apply {
            assertThat(reminderTitle.getOrAwaitValue(), nullValue())
            assertThat(reminderDescription.getOrAwaitValue(), nullValue())
            assertThat(reminderSelectedLocationStr.getOrAwaitValue(), nullValue())
            assertThat(latitude.getOrAwaitValue(), nullValue())
            assertThat(longitude.getOrAwaitValue(), nullValue())
        }
    }


}