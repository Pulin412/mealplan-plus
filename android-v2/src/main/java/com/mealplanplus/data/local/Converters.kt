package com.mealplanplus.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.model.MealItem
import java.time.LocalDate

class Converters {
    @TypeConverter fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }
    @TypeConverter fun toEpochDay(date: LocalDate?): Long?    = date?.toEpochDay()

    @TypeConverter
    fun fromMealItems(items: List<MealItem>?): String = gson.toJson(items ?: emptyList<MealItem>())

    @TypeConverter
    fun toMealItems(json: String?): List<MealItem> =
        if (json.isNullOrBlank()) emptyList() else gson.fromJson(json, mealItemListType)

    @TypeConverter
    fun fromStringList(list: List<String>?): String = gson.toJson(list ?: emptyList<String>())

    @TypeConverter
    fun toStringList(json: String?): List<String> =
        if (json.isNullOrBlank()) emptyList() else gson.fromJson(json, stringListType)

    private companion object {
        val gson = Gson()
        val mealItemListType = object : TypeToken<List<MealItem>>() {}.type
        val stringListType = object : TypeToken<List<String>>() {}.type
    }
}
