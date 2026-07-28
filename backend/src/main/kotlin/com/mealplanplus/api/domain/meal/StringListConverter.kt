package com.mealplanplus.api.domain.meal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/** Stores a List<String> (e.g. a meal's slots) as a JSON text column — keeps a meal one row. */
@Converter
class StringListConverter : AttributeConverter<List<String>, String> {
    override fun convertToDatabaseColumn(attribute: List<String>?): String =
        mapper.writeValueAsString(attribute ?: emptyList<String>())

    override fun convertToEntityAttribute(dbData: String?): List<String> =
        if (dbData.isNullOrBlank()) emptyList() else mapper.readValue(dbData)

    private companion object {
        val mapper = ObjectMapper()
    }
}
