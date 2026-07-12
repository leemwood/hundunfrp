package cn.lemwood.platform

import android.content.Context

object AndroidFrpContext {
    @Volatile
    var appContext: Context? = null
}
