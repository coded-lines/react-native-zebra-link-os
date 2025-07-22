package com.codedlines.reactnativezebralinkos

import android.content.Context
import expo.modules.core.interfaces.Package
import expo.modules.core.interfaces.ReactActivityLifecycleListener

class ReactNativeZebraLinkOsPackage : Package {
    override fun createReactActivityLifecycleListeners(activityContext: Context?): List<ReactActivityLifecycleListener> {
        return listOf(ReactNativeZebraLinkOsReactActivityLifecycleListener())
    }
}