package co.com.jairocpd.ar_app.util

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class SingletonTest {

    // Clase de prueba para verificar el patrón Singleton
    class TestClass(val value: String)

    @Test
    fun `instance returns the same instance for multiple calls`() {
        // Crea un Singleton de TestClass
        val singleton = Singleton { arg: String -> TestClass(arg) }

        // Obtén dos instancias con el mismo argumento
        val instance1 = singleton.instance("first")
        val instance2 = singleton.instance("first")

        // Verifica que ambas instancias sean iguales
        assertSame(instance1, instance2)
    }

    @Test
    fun `instance initializes only once`() {
        // Variable para contar el número de inicializaciones
        var initializationCount = 0

        // Crea un Singleton con un contador
        val singleton = Singleton { _: String ->
            initializationCount++
            TestClass("unique")
        }

        // Llama a `instance` varias veces
        val instance1 = singleton.instance("test")
        val instance2 = singleton.instance("test")

        // Verifica que solo se haya inicializado una vez
        assertSame(instance1, instance2)
        assertNotNull(instance1)
        assertNotNull(instance2)
        assertSame(instance1, instance2)
        assert(initializationCount == 1) { "Expected only one initialization, but got $initializationCount" }
    }

    @Test
    fun `instance handles null creator after initialization`() {
        // Crea un Singleton
        val singleton = Singleton { arg: String -> TestClass(arg) }

        // Llama a `instance` para inicializarlo
        val instance1 = singleton.instance("test")

        // Intenta obtener otra instancia
        val instance2 = singleton.instance("test")

        // Verifica que las instancias sean iguales
        assertSame(instance1, instance2)
    }
}
