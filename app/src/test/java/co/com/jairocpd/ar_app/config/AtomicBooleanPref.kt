package co.com.jairocpd.ar_app.config

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class AtomicBooleanPrefTest {

    @Test
    fun `get returns the current value`() {
        val prefs = mock(SharedPreferences::class.java)
        `when`(prefs.getBoolean("key", true)).thenReturn(true)

        val pref = Settings.AtomicBooleanPref(true, "key", prefs)

        assertTrue(pref.get())
    }

}
