package com.manu.launcher

import org.json.JSONObject

/**
 * Builds ClientAppSettings.json from UI state.
 * All values are proper JSON types (boolean / int), never "True" strings.
 */
object FlagBuilder {

    data class State(
        var fps: Int = 120,
        var showFps: Boolean = true,
        var graySky: Boolean = false,
        var noMsaa: Boolean = false,
        var msaaLevel: Int = 0,          // 0=off, 1,2,4,8
        var frmLevel: Int = -1,          // -1=auto, 1,10,21
        var renderMode: Int = 0,         // 0=auto, 1=OpenGL, 2=Vulkan
        var dynamicRes: String = "auto", // auto / 144 / 240 / ...
        var minGraphics: Boolean = false,
        var reduceMotion: Boolean = false,
        var haptic: Boolean = true,
        var oneClickBoost: Boolean = false,
        var simplifyModels: Boolean = false,
        var lowTextures: Boolean = false,
        var meshOpt: Boolean = false,
        var preloadModels: Boolean = false,
        var lessShadows: Boolean = false,
        var noPostFx: Boolean = false,
        var noGrass: Boolean = false,
        var hideNames: Boolean = false,
        var noBlurMenu: Boolean = false,
        var noVfx: Boolean = false,
        var noTextures: Boolean = false,
        var animCache: Boolean = false,
        var physics60: Boolean = false
    )

    fun build(s: State): String {
        val o = JSONObject()

        // FPS
        o.put("DFIntTaskSchedulerTargetFps", s.fps)

        if (s.showFps) o.put("FFlagDebugDisplayFPS", true)

        if (s.graySky) o.put("FFlagDebugSkyGray", true)

        // MSAA
        if (s.noMsaa || s.msaaLevel == 0) {
            o.put("FIntDebugForceMSAASamples", 0)
            o.put("FFlagDebugForceMSAADisabled", true)
        } else if (s.msaaLevel > 0) {
            o.put("FIntDebugForceMSAASamples", s.msaaLevel)
            o.put("FFlagDebugForceMSAADisabled", false)
        }

        // FRM quality
        if (s.frmLevel > 0) {
            o.put("DFIntDebugFRMQualityLevelOverride", s.frmLevel)
        } else if (s.minGraphics || s.oneClickBoost) {
            o.put("DFIntDebugFRMQualityLevelOverride", 1)
        }

        // Render mode
        when (s.renderMode) {
            1 -> o.put("FFlagDebugGraphicsPreferOpenGL", true)
            2 -> o.put("FFlagDebugGraphicsPreferVulkan", true)
        }

        // Dynamic resolution (best-effort flags)
        if (s.dynamicRes != "auto") {
            o.put("DFIntDebugFRMQualityLevelOverride", when (s.dynamicRes) {
                "144", "240" -> 1
                "360", "480" -> 5
                "720" -> 10
                else -> 15
            })
        }

        if (s.lowTextures || s.oneClickBoost || s.noTextures) {
            o.put("DFFlagTextureQualityOverrideEnabled", true)
            o.put("DFIntTextureQualityOverride", if (s.noTextures) 0 else 1)
        }

        if (s.simplifyModels || s.meshOpt) {
            o.put("FFlagDebugDisableMeshLODs", false)
            o.put("DFIntDebugFRMQualityLevelOverride", o.optInt("DFIntDebugFRMQualityLevelOverride", 1))
        }

        if (s.lessShadows || s.oneClickBoost) {
            o.put("FIntRenderShadowIntensity", 0)
            o.put("FFlagDebugShadowMapCascadeCount", true)
        }

        if (s.noPostFx || s.oneClickBoost || s.noVfx) {
            o.put("FFlagDisablePostFx", true)
        }

        if (s.noGrass) {
            o.put("FIntFRMMinGrassDistance", 0)
            o.put("FIntFRMMaxGrassDistance", 0)
        }

        if (s.hideNames) {
            o.put("FFlagDebugDisplayNames", false)
        }

        if (s.noBlurMenu) {
            o.put("FFlagDebugDisableMenuBlur", true)
        }

        if (s.reduceMotion) {
            o.put("FFlagDebugForceReducedMotion", true)
        }

        if (s.physics60) {
            o.put("DFIntTaskSchedulerTargetFps", minOf(s.fps, 60))
        }

        if (s.animCache) {
            o.put("FFlagDebugEnableAnimationCache", true)
        }

        if (s.haptic) {
            o.put("FFlagDebugEnableHaptics", true)
        }

        return o.toString(2)
    }
}
