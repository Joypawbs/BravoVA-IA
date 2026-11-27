package com.example.platform

import coil3.request.ImageRequest
import coil3.transform.Transformation

/** Coil transformations for product photos (studio white-background cutout on Android). */
expect fun productImageTransformations(): List<Transformation>

/** Pixel-level transformations need software bitmaps on Android; no-op elsewhere. */
expect fun ImageRequest.Builder.softwareBitmapsForTransformations(): ImageRequest.Builder
