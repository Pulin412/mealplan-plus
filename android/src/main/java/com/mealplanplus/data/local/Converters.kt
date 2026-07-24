package com.mealplanplus.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.model.DietEntry
import com.mealplanplus.data.model.DietTag
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
    fun fromDietEntries(entries: List<DietEntry>?): String = gson.toJson(entries ?: emptyList<DietEntry>())

    @TypeConverter
    fun toDietEntries(json: String?): List<DietEntry> =
        if (json.isNullOrBlank()) emptyList() else gson.fromJson(json, dietEntryListType)

    @TypeConverter
    fun fromDietTags(tags: List<DietTag>?): String = gson.toJson(tags ?: emptyList<DietTag>())

    @TypeConverter
    fun toDietTags(json: String?): List<DietTag> =
        if (json.isNullOrBlank()) emptyList() else gson.fromJson(json, dietTagListType)

    @TypeConverter
    fun fromStringList(list: List<String>?): String = gson.toJson(list ?: emptyList<String>())

    @TypeConverter
    fun toStringList(json: String?): List<String> =
        if (json.isNullOrBlank()) emptyList() else gson.fromJson(json, stringListType)

    private companion object {
        val gson = Gson()
        val mealItemListType = object : TypeToken<List<MealItem>>() {}.type
        val dietEntryListType = object : TypeToken<List<DietEntry>>() {}.type
        val dietTagListType = object : TypeToken<List<DietTag>>() {}.type
        val stringListType = object : TypeToken<List<String>>() {}.type
    }
}
