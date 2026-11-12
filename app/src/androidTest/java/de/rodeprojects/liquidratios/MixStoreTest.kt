package de.rodeprojects.liquidratios

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import de.rodeprojects.liquidratios.data.MixStore
import de.rodeprojects.liquidratios.domain.MeasureUnit
import de.rodeprojects.liquidratios.domain.Mix
import de.rodeprojects.liquidratios.domain.NumberText
import de.rodeprojects.liquidratios.domain.Quantity
import java.io.File
import java.math.BigDecimal
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MixStoreTest {
    @Test
    fun reopensDiskStoreWithHiddenRatiosUnitLimitAndExactComponentAnchor() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb")
        val persisted = Mix(
            ratios = listOf("1", "1", "3.25", "8.5").map(::BigDecimal),
            unit = MeasureUnit.MILLILITERS,
            quantity = Quantity.Component(1, BigDecimal("0.03")),
            maximum = BigDecimal("900.75"),
        )
        try {
            withStore(file) {
                assertEquals(Mix(), it.load())
                it.save(persisted)
            }
            withStore(file) { reopened ->
                val actual = reopened.load()
                assertEquals(persisted, actual)
                assertEquals(listOf("0,03", "0,03"), actual.amounts.components.map(NumberText::display))
                assertEquals(listOf("1", "1", "3,25", "8,5"), actual.changeCount(4).activeRatios.map(NumberText::display))
            }
        } finally {
            file.delete()
        }
    }

    private suspend fun withStore(file: File, block: suspend (MixStore) -> Unit) {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        try {
            val preferences = PreferenceDataStoreFactory.create(scope = scope) { file }
            block(MixStore(preferences))
        } finally {
            scope.coroutineContext[Job]?.cancelAndJoin()
        }
    }
}
