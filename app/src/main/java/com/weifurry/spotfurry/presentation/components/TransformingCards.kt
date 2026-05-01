package com.weifurry.spotfurry.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.SurfaceTransformation as surfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Composable
internal fun TransformingLazyColumnItemScope.ActionRowButton(
    label: String,
    detail: String,
    onClick: () -> Unit,
    transformationSpec: TransformationSpec
) {
    TitleCard(
        title = { Text(label) },
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .transformedHeight(this, transformationSpec),
        transformation = surfaceTransformation(transformationSpec)
    ) {
        Text(detail)
    }
}

@Composable
internal fun TransformingLazyColumnItemScope.TrackCard(
    title: String,
    body: String,
    onClick: () -> Unit,
    transformationSpec: TransformationSpec
) {
    TitleCard(
        title = { Text(title) },
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .transformedHeight(this, transformationSpec),
        transformation = surfaceTransformation(transformationSpec)
    ) {
        Text(body)
    }
}
