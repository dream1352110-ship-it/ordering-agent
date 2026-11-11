package com.sm.orderingagent

import android.app.Application
import com.sm.orderingagent.capability.OrderingCliHandler
import com.sm.osagent.capability.provider.CapabilityProviderRegistry

class OrderingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CapabilityProviderRegistry.installCliHandler(OrderingCliHandler(this))
    }
}
