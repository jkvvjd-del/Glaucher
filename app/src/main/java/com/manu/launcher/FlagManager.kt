package com.manu.launcher

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.File
import java.lang.reflect.Method

object FlagManager {

    private const val TARGET_TMP = "/data/local/tmp/ClientAppSettings.json"
    private const val TARGET_ROBLOX =
        "/sdcard/Android/data/com.roblox.client/files/ClientSettings/ClientAppSettings.json"
    private const val TARGET_ROBLOX_VNG =
        "/sdcard/Android/data/com.roblox.client.vnggames/files/ClientSettings/ClientAppSettings.json"

    fun hasRoot(): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor() == 0
    } catch (_: Exception) { false }

    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun accessMode(): String = when {
        hasRoot() -> "Root ✓"
        shizukuReady() -> "Shizuku ✓"
        else -> "нет доступа"
    }

    fun requestShizukuPermission(requestCode: Int = 0) {
        try {
            if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
            ) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) {}
    }

    private fun newShizukuProcess(cmd: Array<String>): Process? = try {
        val method: Method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        method.invoke(null, cmd, null, null) as Process
    } catch (_: Exception) { null }

    fun runElevated(cmd: String, log: (String) -> Unit = {}): Boolean {
        return try {
            val proc = when {
                hasRoot() -> Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                shizukuReady() -> newShizukuProcess(arrayOf("sh", "-c", cmd))
                else -> {
                    log("нет Root / Shizuku")
                    return false
                }
            } ?: return false
            val out = proc.inputStream.bufferedReader().readText().trim()
            val err = proc.errorStream.bufferedReader().readText().trim()
            val code = proc.waitFor()
            log("exit=$code out=$out err=$err")
            code == 0
        } catch (e: Exception) {
            log(e.message ?: "error")
            false
        }
    }

    fun writeFlags(json: String, cacheDir: File, log: (String) -> Unit = {}): Boolean {
        val local = File(cacheDir, "ClientAppSettings.json")
        return try {
            local.writeText(json)
            val path = local.absolutePath
            val cmd = """
                mkdir -p /data/local/tmp
                mkdir -p /sdcard/Android/data/com.roblox.client/files/ClientSettings
                mkdir -p /sdcard/Android/data/com.roblox.client.vnggames/files/ClientSettings
                cp '$path' $TARGET_TMP
                chmod 666 $TARGET_TMP
                cp '$path' $TARGET_ROBLOX 2>/dev/null || true
                chmod 666 $TARGET_ROBLOX 2>/dev/null || true
                cp '$path' $TARGET_ROBLOX_VNG 2>/dev/null || true
                chmod 666 $TARGET_ROBLOX_VNG 2>/dev/null || true
                echo OK
            """.trimIndent().replace("\n", " && ")
            runElevated(cmd, log)
        } catch (e: Exception) {
            log(e.message ?: "error")
            false
        }
    }

    fun clearFlags(log: (String) -> Unit = {}): Boolean {
        val cmd = "rm -f $TARGET_TMP $TARGET_ROBLOX $TARGET_ROBLOX_VNG && echo OK"
        return runElevated(cmd, log)
    }

    /** Kill most background apps (except system / self / Roblox) */
    fun killBackground(log: (String) -> Unit = {}): Boolean {
        val cmd = """
            am kill-all 2>/dev/null || true
            echo OK
        """.trimIndent()
        return runElevated(cmd, log)
    }

    fun forceStopPackage(pkg: String, log: (String) -> Unit = {}): Boolean {
        return runElevated("am force-stop $pkg && echo OK", log)
    }
}
