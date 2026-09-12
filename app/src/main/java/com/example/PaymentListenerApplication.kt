package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.PaymentRepository
import com.example.parsers.PaymentParserEngine
import com.example.services.FirebaseBackendService

class PaymentListenerApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: PaymentRepository
        private set

    lateinit var parserEngine: PaymentParserEngine
        private set

    lateinit var backendService: FirebaseBackendService
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        backendService = FirebaseBackendService()
        repository = PaymentRepository(
            eventDao = database.paymentEventDao(),
            configDao = database.paymentConfigDao(),
            backendService = backendService
        )
        parserEngine = PaymentParserEngine()
    }

    companion object {
        lateinit var instance: PaymentListenerApplication
            private set
    }
}
