package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

actual fun liquidGlassRefractionEffect(
    width: Float,
    height: Float,
    radius: Float,
    depth: Float,
    dispersion: Float
): RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    return runCatching {
        val shader = RuntimeShader(LIQUID_GLASS_SHADER)
        shader.setFloatUniform("size", width, height)
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("depth", depth)
        shader.setFloatUniform("dispersion", dispersion)
        android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }.getOrNull()
}
