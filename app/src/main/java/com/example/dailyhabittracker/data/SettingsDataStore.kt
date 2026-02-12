package com.example.dailyhabittracker.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val darkMode = booleanPreferencesKey("dark_mode")
        val sounds = booleanPreferencesKey("sounds")
        val focusMode = booleanPreferencesKey("focus_mode")
        val haptics = booleanPreferencesKey("haptics")
        val stepBaseDate = stringPreferencesKey("step_base_date")
        val stepBaseValue = stringPreferencesKey("step_base_value")
        val stepCountDate = stringPreferencesKey("step_count_date")
        val stepCountValue = stringPreferencesKey("step_count_value")
    }

    fun darkModeEnabled(): Flow<Boolean> = context.dataStore.data.map { it[Keys.darkMode] ?: false }
    fun soundsEnabled(): Flow<Boolean> = context.dataStore.data.map { it[Keys.sounds] ?: true }
    fun focusModeEnabled(): Flow<Boolean> = context.dataStore.data.map { it[Keys.focusMode] ?: false }
    fun hapticsEnabled(): Flow<Boolean> = context.dataStore.data.map { it[Keys.haptics] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.darkMode] = enabled }
    }

    suspend fun setSounds(enabled: Boolean) {
        context.dataStore.edit { it[Keys.sounds] = enabled }
    }

    suspend fun setFocusMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.focusMode] = enabled }
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[Keys.haptics] = enabled }
    }

    suspend fun getStepBaseline(date: LocalDate): Int? {
        val prefs = context.dataStore.data.first()
        val storedDate = prefs[Keys.stepBaseDate] ?: return null
        if (storedDate != date.toString()) return null
        return prefs[Keys.stepBaseValue]?.toIntOrNull()
    }

    suspend fun setStepBaseline(date: LocalDate, value: Int) {
        context.dataStore.edit {
            it[Keys.stepBaseDate] = date.toString()
            it[Keys.stepBaseValue] = value.toString()
        }
    }

    suspend fun getStepCount(date: LocalDate): Int? {
        val prefs = context.dataStore.data.first()
        val storedDate = prefs[Keys.stepCountDate] ?: return null
        if (storedDate != date.toString()) return null
        return prefs[Keys.stepCountValue]?.toIntOrNull()
    }

    suspend fun setStepCount(date: LocalDate, value: Int) {
        context.dataStore.edit {
            it[Keys.stepCountDate] = date.toString()
            it[Keys.stepCountValue] = value.toString()
        }
    }

    suspend fun setLastRestoreDate(habitId: Long, date: LocalDate) {
        val key = stringPreferencesKey("restore_date_$habitId")
        context.dataStore.edit { it[key] = date.toString() }
    }

    suspend fun getLastRestoreDate(habitId: Long): LocalDate? {
        val key = stringPreferencesKey("restore_date_$habitId")
        val prefs = context.dataStore.data.first()
        return prefs[key]?.let { LocalDate.parse(it) }
    }

    suspend fun setPreviousStreak(habitId: Long, streak: Int) {
        val key = stringPreferencesKey("prev_streak_$habitId")
        context.dataStore.edit { it[key] = streak.toString() }
    }

    suspend fun getPreviousStreak(habitId: Long): Int? {
        val key = stringPreferencesKey("prev_streak_$habitId")
        val prefs = context.dataStore.data.first()
        return prefs[key]?.toIntOrNull()
    }

    suspend fun clearPreviousStreak(habitId: Long) {
        val key = stringPreferencesKey("prev_streak_$habitId")
        context.dataStore.edit { it.remove(key) }
    }

    suspend fun setPausedStreak(habitId: Long, streak: Int) {
        val key = stringPreferencesKey("paused_streak_$habitId")
        context.dataStore.edit { it[key] = streak.toString() }
    }

    suspend fun getPausedStreak(habitId: Long): Int? {
        val key = stringPreferencesKey("paused_streak_$habitId")
        val prefs = context.dataStore.data.first()
        return prefs[key]?.toIntOrNull()
    }

    suspend fun clearPausedStreak(habitId: Long) {
        val key = stringPreferencesKey("paused_streak_$habitId")
        context.dataStore.edit { it.remove(key) }
    }
}
