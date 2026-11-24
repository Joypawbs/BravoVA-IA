package com.example.platform

import coil3.request.ImageRequest
import coil3.transform.Transformation

actual fun productImageTransformations(): List<Transformation> = emptyList()

actual fun ImageRequest.Builder.softwareBitmapsForTransformations(): ImageRequest.Builder = this
