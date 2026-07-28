package com.mealplanplus.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant
import java.time.LocalDate

/**
 * Single source of truth for the Gson used by every Retrofit call.
 *
 * The generated API models use java.time types, but Gson has no built-in
 * adapter for `java.time.*`. The backend serializes dates as ISO-8601 strings
 * (Jackson `write-dates-as-timestamps: false`, matching docs/openapi.yaml).
 * Register every such adapter here so all API traffic shares one consistent,
 * correct configuration.
 *
 * As new screens add DTOs with other wire types (e.g. `LocalDate`, enums),
 * add their adapters here rather than in NetworkModule.
 */
fun apiGson(): Gson = GsonBuilder()
    .registerTypeAdapter(Instant::class.java, InstantIso8601Adapter)
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter)
    .create()

/** `LocalDate` — written as ISO "yyyy-MM-dd"; reads ISO strings or a [year,month,day] array. */
object LocalDateAdapter : TypeAdapter<LocalDate>() {
    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) out.nullValue() else out.value(value.toString())
    }

    override fun read(reader: JsonReader): LocalDate? = when (reader.peek()) {
        JsonToken.NULL -> { reader.nextNull(); null }
        JsonToken.BEGIN_ARRAY -> {
            reader.beginArray()
            val y = reader.nextInt(); val m = reader.nextInt(); val d = reader.nextInt()
            while (reader.peek() != JsonToken.END_ARRAY) reader.skipValue()
            reader.endArray()
            LocalDate.of(y, m, d)
        }
        else -> LocalDate.parse(reader.nextString())
    }
}

/** `Instant` <-> ISO-8601 string — matches the backend's `write-dates-as-timestamps: false`. */
object InstantIso8601Adapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) out.nullValue() else out.value(value.toString())
    }

    override fun read(reader: JsonReader): Instant? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return Instant.parse(reader.nextString())
    }
}
