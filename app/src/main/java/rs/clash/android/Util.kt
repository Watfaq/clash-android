package rs.clash.android

import android.content.ComponentName
import android.content.Intent
import android.net.LocalSocket
import android.net.LocalSocketAddress
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory
import kotlin.reflect.KClass

val KClass<*>.componentName: ComponentName
    get() = ComponentName(Global.application.packageName, this.java.name)

val KClass<*>.intent: Intent
    get() = Intent(Global.application, this.java)


