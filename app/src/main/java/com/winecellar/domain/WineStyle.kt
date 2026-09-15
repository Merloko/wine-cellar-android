package com.winecellar.domain

/**
 * Broad drinking style inferred from a wine's grape/type string. Drives both the
 * colour used in the UI and the default cellaring window in [DrinkWindow].
 *
 * The offsets are years after the vintage for a *typical* wine of that style —
 * deliberately conservative, and only used when the owner hasn't recorded an
 * explicit drinking window on the bottle.
 */
enum class WineStyle(
    val label: String,
    val minYearsAfterVintage: Int,
    val maxYearsAfterVintage: Int,
) {
    SPARKLING("Sparkling", 0, 6),
    WHITE("White", 0, 3),
    ROSE("Rosé", 0, 2),
    RED_LIGHT("Light red", 1, 6),
    RED_MEDIUM("Medium red", 2, 10),
    RED_BOLD("Bold red", 3, 15),
    UNKNOWN("Wine", 0, 8);

    companion object {
        /** Classify a grape/type string (e.g. "Grenache;Syrah;Mataro"). */
        fun classify(grapeType: String?, name: String? = null): WineStyle {
            val t = "${grapeType.orEmpty()} ${name.orEmpty()}".lowercase()
            if (t.isBlank()) return UNKNOWN

            fun any(vararg keys: String) = keys.any { it in t }

            // Order matters: check sparkling/rosé before the varietal reds/whites.
            if (any("champagne", "sparkling", "prosecco", "cava", "brut", "cordon")) return SPARKLING
            if (any("rose", "rosé", "saignee", "saignée", "d'anjou", "rapsodhy", "velvet rose")) return ROSE

            if (any(
                    "cabernet", "bordeaux", "malbec", "petit verdot", "shiraz", "syrah",
                    "touriga", "alicante", "zinfandel", "bdx", "nebbiolo", "sangiovese",
                )
            ) return RED_BOLD

            if (any("grenache", "gsm", "mataro", "mourvedre", "mourvèdre", "tempranillo", "merlot", "g&t"))
                return RED_MEDIUM

            if (any("pinot noir", "gamay")) return RED_LIGHT

            if (any(
                    "chardonnay", "sauvignon blanc", "ssb", "semillon", "riesling", "reisling",
                    "gewurztraminer", "gewürztraminer", "blanc", "verdelho", "viognier",
                    "pinot gris", "pinot grigio", "white",
                )
            ) return WHITE

            return UNKNOWN
        }
    }
}
