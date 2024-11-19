package co.com.jairocpd.ar_app.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class SingletonTest {

    class TestClass(val value: String)

    @Test
    fun `instance returns the same instance for multiple calls`() {
        val singleton = Singleton { arg: String -> TestClass(arg) }

        val instance1 = singleton.instance("first")
        val instance2 = singleton.instance("first")

        assertSame(instance1, instance2)
    }

    @Test
    fun `instance initializes only once`() {
        var initializationCount = 0

        val singleton = Singleton { _: String ->
            initializationCount++
            TestClass("unique")
        }

        val instance1 = singleton.instance("test")
        val instance2 = singleton.instance("test")

        assertSame(instance1, instance2)
        assertNotNull(instance1)
        assertNotNull(instance2)
        assertSame(instance1, instance2)
        assert(initializationCount == 1) { "Expected only one initialization, but got $initializationCount" }
    }

    @Test
    fun `instance handles null creator after initialization`() {
        val singleton = Singleton { arg: String -> TestClass(arg) }

        val instance1 = singleton.instance("test")

        val instance2 = singleton.instance("test")

        assertSame(instance1, instance2)
    }
}
