package com.hackathon.afterlog.data.model

data class PerspectiveGuidePoint(
    val x: Float,
    val y: Float
) {
    fun normalized(): PerspectiveGuidePoint {
        fun clamp(value: Float) = value.coerceIn(0f, 1f)
        return PerspectiveGuidePoint(clamp(x), clamp(y))
    }
}

data class PerspectiveGuideConfig(
    val points: List<PerspectiveGuidePoint>
) {
    init {
        require(points.size == 4) { "Perspective guide must define exactly four corners" }
    }

    fun toSerializedString(): String {
        return points.joinToString(separator = "|") { "${it.x.coerceIn(0f, 1f)},${it.y.coerceIn(0f, 1f)}" }
    }

    companion object {
        private val defaultCorners = listOf(
            PerspectiveGuidePoint(0.1f, 0.1f),
            PerspectiveGuidePoint(0.9f, 0.1f),
            PerspectiveGuidePoint(0.9f, 0.9f),
            PerspectiveGuidePoint(0.1f, 0.9f)
        )

        fun default(): PerspectiveGuideConfig {
            return PerspectiveGuideConfig(defaultCorners)
        }

        fun fromSerializedString(value: String?): PerspectiveGuideConfig? {
            if (value.isNullOrBlank()) return null
            val parsed = value.split("|").mapNotNull { part ->
                val coords = part.split(",")
                if (coords.size != 2) return@mapNotNull null
                val x = coords[0].toFloatOrNull() ?: return@mapNotNull null
                val y = coords[1].toFloatOrNull() ?: return@mapNotNull null
                PerspectiveGuidePoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
            }
            return if (parsed.size == 4) PerspectiveGuideConfig(parsed) else null
        }
    }

    fun withUpdatedPoint(index: Int, point: PerspectiveGuidePoint): PerspectiveGuideConfig {
        if (index !in points.indices) return this
        val updated = points.toMutableList()
        updated[index] = point.normalized()
        return PerspectiveGuideConfig(updated)
    }
}
