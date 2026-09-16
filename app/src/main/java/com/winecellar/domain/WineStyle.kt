package com.winecellar.domain

/**
 * Broad drinking style inferred from a wine's grape/type string. Drives both the
 * colour used in the UI and the default cellaring window in [DrinkWindow].
 *
 * The offsets are years after the vintage for a *typical* wine of that style,
 * and are only used when the owner hasn't recorded an explicit drinking window
 * on the bottle. They're intentionally broad — a rough guide, not a verdict.
 *
 *   Style        Drink from → through (years after vintage)
 *   Sparkling    0 → 7    (NV is treated as ready now; vintage fizz can age)
 *   White        0 → 3    (most are best young; crisp whites especially)
 *   Rosé         0 → 2    (drink young and fresh)
 *   Light red    1 → 6    (Pinot Noir, Gamay)
 *   Medium red   2 → 10   (GSM/Rhône blends, Grenache, Tempranillo, Merlot…)
 *   Bold red     3 → 18   (Cabernet, Bordeaux blends, Shiraz, Nebbiolo…)
 *   Unknown      0 → 8
 */
enum class WineStyle(
    val minYearsAfterVintage: Int,
    val maxYearsAfterVintage: Int,
) {
    SPARKLING(0, 7),
    WHITE(0, 3),
    ROSE(0, 2),
    RED_LIGHT(1, 6),
    RED_MEDIUM(2, 10),
    RED_BOLD(3, 18),
    UNKNOWN(0, 8);

    companion object {
        /**
         * Classify a grape/type string (e.g. "Grenache;Syrah;Mataro").
         *
         * Priority reflects how a blend ages: a tannic, age-worthy component
         * (Cabernet, Nebbiolo…) sets a bold window even when blended, while a
         * Grenache-led Rhône blend stays medium even though it contains Syrah.
         */
        fun classify(grapeType: String?, name: String? = null): WineStyle {
            val t = "${grapeType.orEmpty()} ${name.orEmpty()}".lowercase()
            if (t.isBlank()) return UNKNOWN

            fun any(vararg keys: String) = keys.any { it in t }

            // Sparkling before rosé so a rosé Champagne is still "sparkling";
            // rosé before the reds so "Cabernet D'Anjou" (a rosé) isn't a bold red.
            if (any("champagne", "sparkling", "prosecco", "cava", "cremant", "crémant", "brut", "cordon")) {
                return SPARKLING
            }
            // Match rosé words whole so "Primrose"/"Rosewood" aren't mistaken for rosé.
            val words = t.split(Regex("[^\\p{L}]+"))
            if (words.any { it == "rose" || it == "rosé" || it == "saignee" || it == "saignée" } ||
                "d'anjou" in t
            ) {
                return ROSE
            }

            // Tannic, long-lived varietals dominate a blend's ageing potential.
            if (any(
                    "cabernet", "cab franc", "bordeaux", "bdx", "malbec",
                    "petit verdot", "nebbiolo", "touriga", "tannat", "aglianico", "alicante",
                )
            ) return RED_BOLD

            // Syrah/Shiraz: age-worthy on its own, but medium in a Grenache-led blend (GSM).
            val rhoneBlend = any("grenache", "gsm", "mataro", "mourvedre", "mourvèdre")
            if (any("shiraz", "syrah")) return if (rhoneBlend) RED_MEDIUM else RED_BOLD

            if (rhoneBlend || any(
                    "tempranillo", "merlot", "sangiovese", "zinfandel", "barbera", "montepulciano", "g&t",
                )
            ) return RED_MEDIUM

            if (any("pinot noir", "gamay")) return RED_LIGHT

            if (any(
                    "chardonnay", "sauvignon blanc", "ssb", "semillon", "riesling", "reisling",
                    "gewurztraminer", "gewürztraminer", "chenin", "verdelho", "viognier",
                    "marsanne", "roussanne", "gruner", "grüner", "albarino", "albariño",
                    "moscato", "pinot gris", "pinot grigio", "blanc", "white",
                )
            ) return WHITE

            return UNKNOWN
        }
    }
}
