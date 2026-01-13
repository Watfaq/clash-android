package rs.clash.android

import android.content.ComponentName
import android.content.Intent
import android.net.LocalSocket
import android.net.LocalSocketAddress
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory
import kotlin.reflect.KClass

val KClass<*>.componentName: ComponentName
    get() = ComponentName(Global.application.packageName, this.java.name)

val KClass<*>.intent: Intent
    get() = Intent(Global.application, this.java)

fun createUnixDomainSocketOkHttpClient(socketPath: String): OkHttpClient {
    return OkHttpClient.Builder()
        .socketFactory(object : SocketFactory() {
            override fun createSocket(): Socket {
                return object : Socket() {
                    val localSocket = LocalSocket()
                    override fun connect(endpoint: java.net.SocketAddress?, timeout: Int) {
                        localSocket.connect(LocalSocketAddress(socketPath, LocalSocketAddress.Namespace.FILESYSTEM))
                    }
                    override fun getInputStream() = localSocket.inputStream
                    override fun getOutputStream() = localSocket.outputStream
                    override fun isConnected() = localSocket.isConnected
                    override fun close() = localSocket.close()
                }
            }
            override fun createSocket(host: String?, port: Int) = createSocket()
            override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int) = createSocket()
            override fun createSocket(host: InetAddress?, port: Int) = createSocket()
            override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int) = createSocket()
        })
        .build()
}
