package com.mealplanplus.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

/**
 * Tests for the extracted pure-logic helper used by [NotificationAlarmReceiver].
 *
 * The receiver delegates the firing decision to [NotificationAlarmReceiver.shouldPostMealAlarm]
 * which wraps the existing [com.mealplanplus.util.NotificationDecider] — no Android deps needed.
 */
class NotificationAlarmReceiverTest {

    companion object {
        @JvmStatic @BeforeClass fun setUpClass() {}
        @JvmStatic @AfterClass fun tearDownClass() {}
    }

    @Test
    fun shouldPostMealAlarm_allConditionsMet_returnsTrue() {
        assertTrue(
            NotificationAlarmReceiver.shouldPostMealAlarm(
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun shouldPostMealAlarm_notificationsDisabled_returnsFalse() {
        assertFalse(
            NotificationAlarmReceiver.shouldPostMealAlarm(
                notificationsEnabled = false,
                mealRemindersEnabled = true,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun shouldPostMealAlarm_alreadyLogged_returnsFalse() {
        assertFalse(
            NotificationAlarmReceiver.shouldPostMealAlarm(
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                isAlreadyLogged = true
            )
        )
    }
}
