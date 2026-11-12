package de.rodeprojects.liquidratios.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.rodeprojects.liquidratios.domain.MAX_COMPONENTS
import de.rodeprojects.liquidratios.domain.MIN_COMPONENTS
import de.rodeprojects.liquidratios.domain.MeasureUnit
import de.rodeprojects.liquidratios.domain.Mix
import de.rodeprojects.liquidratios.domain.NumberText
import de.rodeprojects.liquidratios.domain.Quantity
import kotlinx.coroutines.flow.first

internal val Context.mixDataStore by preferencesDataStore(name = "mix")

internal class MixStore(private val dataStore: DataStore<Preferences>) {
    suspend fun load(): Mix = decode(dataStore.data.first())

    suspend fun save(mix: Mix) {
        dataStore.edit { prefs ->
            prefs[Keys.count] = mix.count
            prefs[Keys.unit] = mix.unit.name
            prefs[Keys.maximum] = mix.maximum.toPlainString()
            prefs[Keys.amount] = mix.quantity.amount.toPlainString()
            prefs[Keys.component] = (mix.quantity as? Quantity.Component)?.index ?: -1
            Keys.ratios.forEachIndexed { index, key -> prefs[key] = mix.ratios[index].toPlainString() }
        }
    }

    private fun decode(prefs: Preferences): Mix {
        val defaults = Mix()
        val count = prefs[Keys.count]?.takeIf { it in MIN_COMPONENTS..MAX_COMPONENTS } ?: defaults.count
        val ratios = Keys.ratios.mapIndexed { index, key ->
            prefs[key]?.let { NumberText.parse(it, 12) }?.takeIf { it.signum() > 0 } ?: defaults.ratios[index]
        }
        val unit = MeasureUnit.entries.firstOrNull { it.name == prefs[Keys.unit] } ?: defaults.unit
        val maximum = prefs[Keys.maximum]?.let { NumberText.parse(it, 2) }?.takeIf { it.signum() > 0 }
            ?: defaults.maximum
        val amount = prefs[Keys.amount]?.let { NumberText.parse(it, 2) } ?: defaults.quantity.amount
        val index = prefs[Keys.component]?.takeIf { it in 0 until count }
        val quantity = if (index == null) Quantity.Total(amount) else Quantity.Component(index, amount)
        val mix = Mix(count, ratios, unit, quantity, maximum)
        return if (mix.amounts.total <= maximum) mix else mix.copy(quantity = Quantity.Total(maximum))
    }

    private object Keys {
        val count = intPreferencesKey("component_count")
        val unit = stringPreferencesKey("unit")
        val maximum = stringPreferencesKey("maximum")
        val amount = stringPreferencesKey("amount")
        val component = intPreferencesKey("amount_component")
        val ratios = List(MAX_COMPONENTS) { stringPreferencesKey("ratio_" + it) }
    }
}
