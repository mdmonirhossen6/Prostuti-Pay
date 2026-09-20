package com.example

import com.example.models.PaymentEvent
import com.example.models.PaymentParseResult
import com.example.models.PaymentRequest
import com.example.parsers.BkashParser
import com.example.parsers.NagadParser
import com.example.parsers.Normalizer
import com.example.parsers.PaymentParserEngine
import com.example.parsers.RocketParser
import com.example.parsers.UpayParser
import com.example.services.FirebaseBackendService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentParserTest {

    private val engine = PaymentParserEngine()
    private val backendService = FirebaseBackendService()

    @Test
    fun testBkashParser() {
        val sms = "You have received Tk 500.00 from 01712345678. Fee Tk 0.00. Balance Tk 1,500.00. TrxID BKA8921XYZ at 12/09/2026 14:30"
        val parser = BkashParser()
        val result = parser.parse(sms)

        assertNotNull(result)
        assertEquals("bKash", result!!.method)
        assertEquals("BKA8921XYZ", result.transactionId)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("01712345678", result.sender)
    }

    @Test
    fun testNagadParser() {
        val sms = "Cash In received. Amount: Tk 650.00, Sender: 01812345678, TxnID: 72N0ABCD, Balance: Tk 2,150.00, Time: 12/09/2026 14:32"
        val parser = NagadParser()
        val result = parser.parse(sms)

        assertNotNull(result)
        assertEquals("Nagad", result!!.method)
        assertEquals("72N0ABCD", result.transactionId)
        assertEquals(650.0, result.amount, 0.001)
        assertEquals("01812345678", result.sender)
    }

    @Test
    fun testRocketParser() {
        val sms = "Tk500.00 received from 01712345678-9 to A/C 01987654321-0. Fee Tk 0.00, Balance Tk 2,500.00, TxnId: 198273645 on 12-Sep-2026"
        val parser = RocketParser()
        val result = parser.parse(sms)

        assertNotNull(result)
        assertEquals("Rocket", result!!.method)
        assertEquals("198273645", result.transactionId)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("01712345678", result.sender)
    }

    @Test
    fun testUpayParser() {
        val sms = "Received Tk 500.00 from 01712345678. TxnID: UP789123. Balance Tk 1,200.00. Fee Tk 0.00"
        val parser = UpayParser()
        val result = parser.parse(sms)

        assertNotNull(result)
        assertEquals("Upay", result!!.method)
        assertEquals("UP789123", result.transactionId)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("01712345678", result.sender)
    }

    @Test
    fun testSpamIgnored() {
        val spam = "Congratulations! You have won 1GB internet for dialling *123#."
        val result = engine.parse(spam)
        assertNull(result)
    }

    @Test
    fun testPhoneNormalization() {
        assertEquals("01712345678", Normalizer.normalizePhoneNumber("+8801712345678"))
        assertEquals("01712345678", Normalizer.normalizePhoneNumber("8801712345678"))
        assertEquals("01712345678", Normalizer.normalizePhoneNumber("01712345678"))
        assertEquals("01712345678", Normalizer.normalizePhoneNumber("01712345678-9"))
    }

    @Test
    fun testDeduplicationFingerprint() {
        val res1 = PaymentParseResult(
            method = "bKash",
            transactionId = "bka8921xyz",
            amount = 500.0,
            sender = "01712345678",
            rawText = "text 1"
        )
        val res2 = PaymentParseResult(
            method = "bKash",
            transactionId = "BKA8921XYZ",
            amount = 500.0,
            sender = "01712345678",
            rawText = "text 2 different timestamp"
        )
        assertEquals(res1.createFingerprint(), res2.createFingerprint())
    }

    @Test
    fun testBackendMatchSimulation_Approved() {
        val event = PaymentEvent(
            id = 1,
            fingerprint = "fp1",
            method = "bKash",
            transactionId = "BKA8921XYZ",
            amount = 500.0,
            sender = "01712345678",
            source = "sms",
            rawText = "sms",
            status = "parsed"
        )
        val pending = listOf(
            PaymentRequest(
                id = "req_1",
                userId = "usr_1",
                fromNumber = "01712345678",
                transactionId = "BKA8921XYZ",
                method = "bKash",
                price = 500.0,
                status = "pending"
            )
        )

        val match = backendService.simulateBackendMatch(event, pending)
        assertTrue(match.success)
        assertEquals("approved", match.status)
        assertEquals("req_1", match.matchedRequestId)
    }

    @Test
    fun testBackendMatchSimulation_Ambiguous() {
        val event = PaymentEvent(
            id = 2,
            fingerprint = "fp2",
            method = "Rocket",
            transactionId = "DUP123",
            amount = 300.0,
            sender = "01912345678",
            source = "sms",
            rawText = "sms",
            status = "parsed"
        )
        val pending = listOf(
            PaymentRequest(id = "req_a", transactionId = "DUP123", method = "Rocket", price = 300.0, status = "pending"),
            PaymentRequest(id = "req_b", transactionId = "DUP123", method = "Rocket", price = 300.0, status = "pending")
        )

        val match = backendService.simulateBackendMatch(event, pending)
        assertTrue(match.success)
        assertEquals("ambiguous", match.status)
        assertEquals(2, match.candidateCount)
    }

    @Test
    fun testToleranceMatching_WithinTolerance() {
        val event = PaymentEvent(
            id = 3,
            fingerprint = "fp3",
            method = "bKash",
            transactionId = "BKA_TOL_1",
            amount = 995.0, // ৳5 under expected
            sender = "01712345678",
            source = "sms",
            rawText = "sms",
            status = "parsed"
        )
        val pending = listOf(
            PaymentRequest(
                id = "req_plan_1000",
                userId = "usr_tol_1",
                fromNumber = "01712345678",
                transactionId = "BKA_TOL_1",
                method = "bKash",
                price = 1000.0,
                status = "pending",
                plan = "hsc_annual"
            )
        )

        // Tolerance is 10.0 -> accepted range is 990.0 to 1010.0
        val testConfig = com.example.models.PaymentConfig(
            toleranceEnabled = true,
            globalTolerance = 10.0,
            maxTolerance = 50.0
        )
        val match = backendService.simulateBackendMatch(event, pending, config = testConfig)
        assertTrue(match.success)
        assertEquals("approved", match.status)
        assertEquals("req_plan_1000", match.matchedRequestId)
        assertEquals(1000.0, match.expectedAmount ?: 0.0, 0.01)
        assertEquals(5.0, match.amountDifference ?: 0.0, 0.01)
    }

    @Test
    fun testToleranceMatching_ExceedsTolerance() {
        val event = PaymentEvent(
            id = 4,
            fingerprint = "fp4",
            method = "bKash",
            transactionId = "BKA_TOL_2",
            amount = 985.0, // ৳15 under expected, but tolerance is 10
            sender = "01712345678",
            source = "sms",
            rawText = "sms",
            status = "parsed"
        )
        val pending = listOf(
            PaymentRequest(
                id = "req_plan_1000",
                userId = "usr_tol_2",
                fromNumber = "01712345678",
                transactionId = "BKA_TOL_2",
                method = "bKash",
                price = 1000.0,
                status = "pending",
                plan = "hsc_annual"
            )
        )

        val testConfig = com.example.models.PaymentConfig(
            toleranceEnabled = true,
            globalTolerance = 10.0,
            maxTolerance = 50.0
        )
        val match = backendService.simulateBackendMatch(event, pending, config = testConfig)
        org.junit.Assert.assertFalse(match.success)
        assertEquals("rejected", match.status) // Outside tolerance criteria
        assertEquals("Amount ৳985.0 is below minimum accepted ৳990.0 (Expected: ৳1000.0, Tolerance: ৳10.0)", match.message?.removePrefix("SIMULATION: Rejected: ")?.trim())
    }

    @Test
    fun testBengaliNumeralNormalization() {
        val bengaliAmount = "৳১,৫৫০.৫০"
        val converted = Normalizer.parseAmount(bengaliAmount)
        assertEquals(1550.50, converted, 0.001)

        val bengaliPhone = "০১৭১২৩৪৫৬৭৮"
        val normalizedPhone = Normalizer.normalizePhoneNumber(bengaliPhone)
        assertEquals("01712345678", normalizedPhone)
    }
}
