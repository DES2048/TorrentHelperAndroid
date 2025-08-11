package com.yae.torrenthelper

import com.yae.torrenthelper.network.CopyState
import com.yae.torrenthelper.network.copyWithProgress
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import java.io.IOException

class FlowSampleTest {
    @Test
    fun testCopyWithProgress() = runTest {
        val data = "hello world!".toByteArray()
        val iStream = ByteArrayInputStream(data)
        val oStream = ByteArrayOutputStream()

        val statesList = copyWithProgress(iStream, oStream).toList()
        // test that only two states emitted
        assertEquals(2, statesList.size)
        // test progress state
        assertEquals(CopyState.Progress(data.size), statesList[0])
        // test that latest state is Finished
        assertEquals(CopyState.Finished, statesList[1])
        // test that data is equal
        assertEquals(data.decodeToString(), oStream.toString())

    }

    @Test
    fun testCopyWithProgressFailed() = runTest {
        val sampleData = "sample".toByteArray()
        var callCount = 0
        val exc = IOException("Failed")
        val iStream = object : ByteArrayInputStream(sampleData) {
            override fun read(b: ByteArray?): Int {
                if (callCount == 0) {
                    callCount++
                    return super.read(b)
                } else {
                    throw exc
                }
            }
        }
        val oStream = ByteArrayOutputStream()
        val statesList = copyWithProgress(iStream, oStream).toList()
        // test that only two states emitted
        assertEquals(2, statesList.size)
        // test progress state
        assertEquals(CopyState.Progress(sampleData.size), statesList[0])
        // test that latest state is Failed
        assertEquals(CopyState.Failed(exc), statesList[1])

    }

    @Test
    fun testCopyWithProgressCancellable() = runTest {

    }
}