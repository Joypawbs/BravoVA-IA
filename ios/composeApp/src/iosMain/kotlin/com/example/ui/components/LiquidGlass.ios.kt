package com.example.ui.components

import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder

private val liquidGlassEffect: RuntimeEffect? by lazy {
    runCatching { RuntimeEffect.makeForShader(LIQUID_GLASS_SHADER) }.getOrNull()
}

actual fun liquidGlassRefractionEffect(
    width: Float,
    height: Float,
    radius: Float,
    depth: Float,
    dispersion: Float
): RenderEffect? {
    val effect = liquidGlassEffect ?: return null
    return runCatching {
        val builder = RuntimeShaderBuilder(effect)
        builder.uniform("size", width, height)
        builder.uniform("radius", radius)
        builder.uniform("depth", depth)
        builder.uniform("dispersion", dispersion)
        ImageFilter.makeRuntimeShader(builder, "content", null).asComposeRenderEffect()
    }.getOrNull()
}
