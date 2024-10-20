package com.yae.torrenthelper.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import com.yae.torrenthelper.TestSettings
import java.io.InputStream
import java.io.OutputStream

object TestSettingsSerializer : Serializer<TestSettings> {
    override val defaultValue: TestSettings = TestSettings.getDefaultInstance()
    override suspend fun readFrom(input: InputStream): TestSettings {
        try {
            return TestSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: TestSettings, output: OutputStream) = t.writeTo(output)
}
/*
val Context.testSettingsDataStore: DataStore<TestSettings> by dataStore(
    fileName = "test_settings.pb",
    serializer = TestSettingsSerializer
) */