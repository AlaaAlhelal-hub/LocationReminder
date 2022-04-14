package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.utils.MainCoroutineRule
import com.udacity.project4.locationreminders.utils.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
class RemindersListViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var application : Application
    private lateinit var viewModel: RemindersListViewModel
    private lateinit var dataSource: FakeDataSource
    private lateinit var reminders :MutableList<ReminderDTO>


    @Before
    fun init() {
        stopKoin()
        application = ApplicationProvider.getApplicationContext() as Application

        reminders =  MutableList(5) { index ->
            ReminderDTO(
                "title-index",
                "description-$index",
                "location-$index",
                index.toDouble() + 10,
                -index.toDouble() - 10
            )
        }
        dataSource = FakeDataSource(reminders)
        viewModel = RemindersListViewModel(application, dataSource)
    }


    @Test
    fun loadReminders_getAllDataSuccessfully() {
        dataSource.shouldReturnError = false

        viewModel.loadReminders()

        val value = viewModel.remindersList.getOrAwaitValue()
        assertThat(value.size, `is`(reminders.size))
    }

    @Test
    fun loadReminders_getAllDataFailed() {
        dataSource.shouldReturnError = true

        viewModel.loadReminders()

        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception: something went wrong"))

    }

    @Test
    fun loadReminders_checkLoading() {
        mainCoroutineRule.pauseDispatcher()

        viewModel.loadReminders()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()

        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

}