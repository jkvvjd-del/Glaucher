package com.manu.launcher

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var contentHost: FrameLayout
    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    private val state = FlagBuilder.State()
    private val switches = mutableMapOf<String, SwitchCompat>()

    // Game mode toggles (system)
    private var turbo = false
    private var keepScreenOn = false
    private var dnd = false
    private var killBg = false
    private var cleanRam = false
    private var noBatteryOpt = false

    private val pages = mutableMapOf<String, View>()
    private var currentTab = "ff"

    private val shizukuListener =
        Shizuku.OnRequestPermissionResultListener { _, result ->
            updateStatus()
            toast(if (result == PackageManager.PERMISSION_GRANTED) "Shizuku OK" else "Shizuku отказано")
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Shizuku.addRequestPermissionResultListener(shizukuListener)
            FlagManager.requestShizukuPermission(0)
        } catch (_: Exception) {}

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.bg))
        }

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(8))
        }
        header.addView(TextView(this).apply {
            text = "MANU Launcher"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(this).apply {
            text = "FastFlag и Оптимизация игр"
            setTextColor(color(R.color.text_sec))
            textSize = 12f
        })
        statusText = TextView(this).apply {
            setTextColor(color(R.color.accent2))
            textSize = 12f
            setPadding(0, dp(6), 0, 0)
        }
        header.addView(statusText)
        root.addView(header)

        // Content
        contentHost = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        root.addView(contentHost)

        // Bottom nav
        root.addView(buildBottomNav())

        setContentView(root)

        pages["ff"] = buildFastFlagPage()
        pages["settings"] = buildSettingsPage()
        pages["game"] = buildGameModePage()
        pages["presets"] = buildPresetsPage()

        showTab("ff")
        updateStatus()
    }

    override fun onDestroy() {
        try { Shizuku.removeRequestPermissionResultListener(shizukuListener) } catch (_: Exception) {}
        super.onDestroy()
    }

    // ───────────────────── PAGES ─────────────────────

    private fun buildFastFlagPage(): View {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(100))
        }
        scroll.addView(col)

        // Status + request
        col.addView(card {
            addView(label("Доступ к системным флагам"))
            addView(TextView(this@MainActivity).apply {
                text = "Статус смотри в шапке. Без Root/Shizuku флаги не применятся."
                setTextColor(color(R.color.text_sec))
                textSize = 12f
                setPadding(0, 0, 0, dp(8))
            })
            addView(btn("Запросить Shizuku", secondary = true) {
                FlagManager.requestShizukuPermission(0)
                updateStatus()
            })
        })

        // FPS
        col.addView(card {
            addView(label("Частота кадров (FPS)"))
            val fpsLabel = TextView(this@MainActivity).apply {
                text = "${state.fps}"
                setTextColor(color(R.color.accent))
                textSize = 40f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
            }
            addView(fpsLabel)
            addView(SeekBar(this@MainActivity).apply {
                max = 180
                progress = state.fps - 60
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                        state.fps = 60 + p
                        fpsLabel.text = "${state.fps}"
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
        })

        // Main toggles
        col.addView(card {
            addView(section("Производительность"))
            toggle("show_fps", "Счётчик FPS на экране", state.showFps) { state.showFps = it }
            toggle("one_click", "Ускорение графики в 1 клик", state.oneClickBoost) { state.oneClickBoost = it }
            toggle("min_gfx", "Минимальная графика по умолчанию", state.minGraphics) { state.minGraphics = it }
            toggle("low_tex", "Снизить качество текстур", state.lowTextures) { state.lowTextures = it }
            toggle("simplify", "Упростить 3D-модели", state.simplifyModels) { state.simplifyModels = it }
            toggle("mesh", "Оптимизация мешей", state.meshOpt) { state.meshOpt = it }
            toggle("preload", "Предзагрузка 3D-моделей", state.preloadModels) { state.preloadModels = it }
        })

        col.addView(card {
            addView(section("Рендеринг"))
            toggle("gray", "Серое небо (SkyGray)", state.graySky) { state.graySky = it }
            toggle("no_msaa", "Отключить сглаживание (MSAA)", state.noMsaa) { state.noMsaa = it }
            toggle("no_post", "Отключить пост-эффекты", state.noPostFx) { state.noPostFx = it }
            toggle("no_grass", "Отключить траву", state.noGrass) { state.noGrass = it }
            toggle("less_sh", "Уменьшить нагрузку теней", state.lessShadows) { state.lessShadows = it }
            toggle("no_vfx", "NO VFX (без лишних эффектов)", state.noVfx) { state.noVfx = it }
            toggle("no_tex", "Без текстур", state.noTextures) { state.noTextures = it }
        })

        col.addView(card {
            addView(section("Интерфейс"))
            toggle("hide_names", "Скрыть имена игроков", state.hideNames) { state.hideNames = it }
            toggle("no_blur", "Убрать размытие фона меню", state.noBlurMenu) { state.noBlurMenu = it }
            toggle("reduce", "Уменьшить эффекты движения", state.reduceMotion) { state.reduceMotion = it }
            toggle("haptic", "Виброотклик в игре", state.haptic) { state.haptic = it }
            toggle("anim", "Кэширование анимаций", state.animCache) { state.animCache = it }
            toggle("phys60", "Адаптивная физика 60 Гц", state.physics60) { state.physics60 = it }
        })

        col.addView(btn("ПРИМЕНИТЬ ФЛАГИ") { applyFlagsOnly() })
        col.addView(btn("СБРОСИТЬ ФЛАГИ", secondary = true) {
            FlagManager.clearFlags { runOnUiThread { appendLog(it) } }
            toast("Сброшено")
        })
        col.addView(btn("Редактор JSON (профи)", secondary = true) { showJsonEditor() })

        logText = TextView(this).apply {
            setTextColor(0xFF666680.toInt())
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(12), 0, 0)
        }
        col.addView(logText)
        return scroll
    }

    private fun buildSettingsPage(): View {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(100))
        }
        scroll.addView(col)

        col.addView(card {
            addView(section("Качество графики"))
            addView(dropdownRow("Сглаживание (MSAA)", listOf("Авто", "Выкл", "2x", "4x", "8x")) { idx ->
                state.msaaLevel = when (idx) { 1 -> 0; 2 -> 2; 3 -> 4; 4 -> 8; else -> -1 }
                state.noMsaa = state.msaaLevel == 0
            })
            addView(dropdownRow("Уровень FRM", listOf("Авто", "Уровень 1", "Уровень 10", "Уровень 21")) { idx ->
                state.frmLevel = when (idx) { 1 -> 1; 2 -> 10; 3 -> 21; else -> -1 }
            })
            addView(dropdownRow("Режим рендеринга", listOf("Авто", "OpenGL", "Vulkan")) { idx ->
                state.renderMode = idx
            })
            addView(dropdownRow("Динам. разрешение", listOf("Авто", "144p", "240p", "360p", "480p", "720p", "1080p")) { idx ->
                state.dynamicRes = listOf("auto", "144", "240", "360", "480", "720", "1080")[idx]
            })
        })

        col.addView(card {
            addView(section("Быстрые пресеты качества"))
            addView(btn("Potato (макс. FPS)", secondary = true) {
                state.fps = 60; state.oneClickBoost = true; state.graySky = true
                state.noMsaa = true; state.noGrass = true; state.noPostFx = true
                state.lowTextures = true; state.minGraphics = true
                toast("Potato применён — жми ПРИМЕНИТЬ")
            })
            addView(btn("Balanced", secondary = true) {
                state.fps = 120; state.oneClickBoost = false; state.graySky = false
                state.noMsaa = false; state.frmLevel = 10
                toast("Balanced — жми ПРИМЕНИТЬ")
            })
            addView(btn("Quality", secondary = true) {
                state.fps = 60; state.frmLevel = 21; state.msaaLevel = 4
                state.oneClickBoost = false; state.graySky = false
                toast("Quality — жми ПРИМЕНИТЬ")
            })
        })

        col.addView(btn("ПРИМЕНИТЬ И ЗАПУСТИТЬ") { applyAndLaunch() })
        return scroll
    }

    private fun buildGameModePage(): View {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(100))
        }
        scroll.addView(col)

        col.addView(card {
            addView(section("Игровой режим"))
            sysToggle("turbo", "Включить Turbo игровой режим") { turbo = it }
            sysToggle("screen", "Экран всегда включён") { keepScreenOn = it }
            sysToggle("dnd", "Отключать уведомления (Не беспокоить)") { dnd = it }
            sysToggle("battery", "Без ограничений питания") { noBatteryOpt = it }
            sysToggle("ram", "Умная очистка RAM перед запуском") { cleanRam = it }
            sysToggle("kill", "Закрыть фоновые приложения (Shizuku)") { killBg = it }
        })

        col.addView(card {
            addView(section("Системные (Shizuku/Root)"))
            addView(TextView(this@MainActivity).apply {
                text = "Эти функции требуют Shizuku или Root. Без них просто пропускаются."
                setTextColor(color(R.color.text_sec))
                textSize = 12f
                setPadding(0, 0, 0, dp(8))
            })
            addView(btn("Оптимизировать Roblox сейчас", secondary = true) {
                FlagManager.runElevated(
                    "cmd package compile -m speed com.roblox.client 2>/dev/null; echo OK"
                ) { appendLog(it) }
                toast("Команда отправлена")
            })
            addView(btn("Убить фоновые приложения", secondary = true) {
                FlagManager.killBackground { appendLog(it) }
                toast("Kill background")
            })
        })

        col.addView(btn("ПРИМЕНИТЬ РЕЖИМ + ЗАПУСК") {
            applyGameMode()
            applyAndLaunch()
        })
        return scroll
    }

    private fun buildPresetsPage(): View {
        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(100))
        }
        scroll.addView(col)

        data class Preset(val name: String, val desc: String, val apply: () -> Unit)

        val presets = listOf(
            Preset("FPS 240", "Максимальный FPS, минимум эффектов") {
                state.fps = 240; state.showFps = true; state.oneClickBoost = true
                state.graySky = true; state.noMsaa = true; state.noGrass = true
                state.noPostFx = true; state.lowTextures = true
            },
            Preset("Potato", "Слабые устройства") {
                state.fps = 60; state.oneClickBoost = true; state.graySky = true
                state.noMsaa = true; state.noGrass = true; state.noPostFx = true
                state.lowTextures = true; state.noVfx = true; state.noTextures = true
                state.minGraphics = true
            },
            Preset("Gray Sky + FPS", "Серое небо + счётчик") {
                state.fps = 120; state.graySky = true; state.showFps = true
            },
            Preset("Low GFX", "Низкая графика без экстремала") {
                state.fps = 90; state.lowTextures = true; state.lessShadows = true
                state.noPostFx = true; state.frmLevel = 1
            },
            Preset("Сброс всего", "Чистые флаги") {
                val s = FlagBuilder.State()
                state.fps = s.fps; state.showFps = s.showFps; state.graySky = s.graySky
                state.noMsaa = s.noMsaa; state.oneClickBoost = false
                state.minGraphics = false; state.lowTextures = false
                state.noGrass = false; state.noPostFx = false; state.noVfx = false
            }
        )

        presets.forEach { p ->
            col.addView(card {
                addView(TextView(this@MainActivity).apply {
                    text = p.name
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@MainActivity).apply {
                    text = p.desc
                    setTextColor(color(R.color.text_sec))
                    textSize = 12f
                    setPadding(0, dp(4), 0, dp(8))
                })
                addView(btn("Применить пресет") {
                    p.apply()
                    toast("${p.name} готов — жми ЗАПУСК")
                })
            })
        }
        return scroll
    }

    // ───────────────────── ACTIONS ─────────────────────

    private fun applyFlagsOnly() {
        FlagManager.requestShizukuPermission(0)
        val json = FlagBuilder.build(state)
        val dir = externalCacheDir ?: cacheDir
        val ok = FlagManager.writeFlags(json, dir) { appendLog(it) }
        toast(if (ok) "Флаги применены" else "Нужен Root/Shizuku")
        updateStatus()
    }

    private fun applyAndLaunch() {
        applyGameMode()
        applyFlagsOnly()
        launchRoblox()
    }

    private fun applyGameMode() {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (dnd) enableDnd()
        if (noBatteryOpt) requestBatteryOpt()
        if (killBg || cleanRam) {
            FlagManager.killBackground { appendLog(it) }
        }
    }

    private fun enableDnd() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            toast("Выдай доступ «Не беспокоить»")
            return
        }
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    private fun requestBatteryOpt() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        } catch (_: Exception) {}
    }

    private fun launchRoblox() {
        for (pkg in listOf("com.roblox.client", "com.roblox.client.vnggames")) {
            packageManager.getLaunchIntentForPackage(pkg)?.let {
                startActivity(it)
                return
            }
        }
        toast("Roblox не установлен")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.roblox.client")))
        } catch (_: Exception) {}
    }

    private fun showJsonEditor() {
        val edit = android.widget.EditText(this).apply {
            setText(FlagBuilder.build(state))
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF12141F.toInt())
            typeface = Typeface.MONOSPACE
            minLines = 14
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("JSON редактор")
            .setView(edit)
            .setPositiveButton("Применить") { _, _ ->
                val dir = externalCacheDir ?: cacheDir
                FlagManager.writeFlags(edit.text.toString(), dir) { appendLog(it) }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ───────────────────── NAV ─────────────────────

    private fun buildBottomNav(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_nav)
            setPadding(dp(8), dp(10), dp(8), dp(16))
            gravity = Gravity.CENTER
        }

        fun tab(id: String, title: String): TextView {
            return TextView(this).apply {
                text = title
                setTextColor(color(R.color.text_sec))
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(8), dp(6), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { showTab(id) }
            }
        }

        bar.addView(tab("ff", "FastFlag"))
        bar.addView(tab("settings", "Настройки"))

        // Center launch button
        val launch = TextView(this).apply {
            text = "▶"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 22f
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_launch)
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply {
                marginStart = dp(6); marginEnd = dp(6)
            }
            setOnClickListener { applyAndLaunch() }
        }
        bar.addView(launch)

        bar.addView(tab("game", "Игровой"))
        bar.addView(tab("presets", "Пресеты"))
        return bar
    }

    private fun showTab(id: String) {
        currentTab = id
        contentHost.removeAllViews()
        contentHost.addView(pages[id])
    }

    private fun updateStatus() {
        statusText.text = "Доступ: ${FlagManager.accessMode()}"
    }

    private fun appendLog(msg: String) {
        logText.append("\n$msg")
    }

    // ───────────────────── UI HELPERS ─────────────────────

    private fun card(block: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_card)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(12)
            layoutParams = lp
            block()
        }
    }

    private fun section(t: String) = TextView(this).apply {
        text = t
        setTextColor(color(R.color.accent))
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, dp(8))
    }

    private fun label(t: String) = TextView(this).apply {
        text = t
        setTextColor(color(R.color.text_sec))
        textSize = 12f
        setPadding(0, 0, 0, dp(6))
    }

    private fun LinearLayout.toggle(key: String, title: String, default: Boolean, onChange: (Boolean) -> Unit) {
        val row = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this@MainActivity).apply {
            text = title
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sw = SwitchCompat(this@MainActivity).apply {
            isChecked = default
            setOnCheckedChangeListener { _, v -> onChange(v) }
        }
        switches[key] = sw
        row.addView(sw)
        addView(row)
    }

    private fun LinearLayout.sysToggle(key: String, title: String, onChange: (Boolean) -> Unit) {
        toggle(key, title, false, onChange)
    }

    private fun dropdownRow(title: String, options: List<String>, onSelect: (Int) -> Unit): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        row.addView(TextView(this).apply {
            text = title
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 14f
        })
        val value = TextView(this).apply {
            text = options[0]
            setTextColor(color(R.color.accent2))
            textSize = 13f
            setPadding(0, dp(4), 0, 0)
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(title)
                    .setItems(options.toTypedArray()) { _, which ->
                        text = options[which]
                        onSelect(which)
                    }
                    .show()
            }
        }
        row.addView(value)
        return row
    }

    private fun btn(text: String, secondary: Boolean = false, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setTextColor(if (secondary) 0xFFCCCCDD.toInt() else 0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(
                this@MainActivity,
                when {
                    secondary -> R.drawable.bg_btn_secondary
                    else -> R.drawable.bg_btn_primary
                }
            )
            setPadding(dp(16), dp(14), dp(16), dp(14))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(8)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun color(id: Int) = ContextCompat.getColor(this, id)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
