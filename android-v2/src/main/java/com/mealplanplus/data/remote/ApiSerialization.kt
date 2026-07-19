package com.mealplanplus.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.time.Instant

/**
 * Single source of truth for the Gson used by every Retrofit call.
 *
 * The generated API models use java.time types, but the backend's JSON wire
 * format (Jackson, `write-dates-as-timestamps`) doesn't match Gson's defaults —
 * Gson has no built-in adapter for `java.time.*`. Register every such adapter
 * here so all API traffic shares one consistent, correct configuration.
 *
 * As new screens add DTOs with other wire types (e.g. `LocalDate`, enums),
 * add their adapters here rather than in NetworkModule.
 */
fun apiGson(): Gson = GsonBuilder()
    .registerTypeAdapter(Instant::class.java, InstantEpochMillisAdapter)
    .create()

/** `Instant` <-> epoch millis — matches the backend's timestamp serialization. */
object InstantEpochMillisAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant?) {
        if (value == null) out.nullValue() else out.value(value.toEpochMilli())
    }

    override fun read(reader: JsonReader): Instant? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        return Instant.ofEpochMilli(reader.nextLong())
    }
}
