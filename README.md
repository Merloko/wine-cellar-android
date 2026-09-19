# Wine Cellar

An offline-first Android app for managing a home wine cellar. Add wines, find
exactly where a bottle is stored, and see at a glance what's ready to drink now.

No account, no cloud sync — every bottle lives in an on-device SQLite database.
The only network use is one **opt-in** feature: the "Look up online" button on
the add/edit screen, which sends just a scanned barcode to the free Open Food
Facts API to pre-fill a wine's details. Nothing happens unless you tap it, and
no cellar data is ever uploaded.

## Features

- **Add & edit wines** — a clean Material 3 form capturing winery, vintage,
  cuvée, grape/blend, physical location, origin, bottle count, and an optional
  drinking window. Common locations and shelves are suggested as you type.
- **Find where a wine is** — search across winery, grape, vintage, region and
  location, and filter by location, style or winery. Each wine records its
  place down to the rack coordinate: *Cellar · Top · Col 3, Row 4*.
- **Drink Now** — a dedicated view that buckets the cellar into **Drink up**
  (past peak), **Ready to drink now**, and **Coming up next year**. Windows come
  from what you've recorded on the bottle, or are estimated from the grape style
  and vintage when you haven't.
- **Starter cellar included** — the app is pre-seeded on first launch with the
  108 wines exported from the owner's spreadsheet.
- **Drink history** — tap *Drink a bottle* on any wine to log it (with an
  optional star rating and tasting note); the bottle count drops and the
  **History** tab keeps the record even if the wine is later removed.
- **Barcode scanning** — the Scan action uses the on-device camera + bundled
  ML Kit barcode model (fully offline). A known bottle jumps straight to its
  entry; a new one opens the Add form pre-filled with the barcode, so the next
  scan of that wine is instant.
- **Look up online (opt-in)** — on the add/edit screen, a *Look up online* button
  queries the free Open Food Facts API for the barcode and pre-fills winery /
  name / country. It's the only networked feature and runs only when tapped.
- **Export / import** — share the whole cellar as CSV or JSON via the Android
  share sheet, and import it back (format auto-detected). JSON matches the seed
  schema.

## Data model

Each wine (`data/Wine.kt`) captures:

| Field | Meaning |
|---|---|
| `winery`, `vintage`, `name`, `grapeType` | what the wine is (vintage optional for NV) |
| `location`, `shelf`, `rackColumn`, `rackRow` | where it physically is, coarse → fine |
| `country`, `region` | origin |
| `quantity` | bottles in this spot |
| `drinkFrom`, `drinkTo`, `drinkWindowNote` | recorded drinking window |
| `favorite`, `notes`, `dateAdded` | extras |

## Drinking windows

If a bottle has a recorded `drinkFrom`/`drinkTo`, that's used verbatim. Otherwise
the window is estimated from the wine's style (guessed from the grape/type) and
vintage — see `domain/WineStyle.kt` / `domain/DrinkWindow.kt`:

| Style | Drink from → through (years after vintage) |
|---|---|
| Sparkling | 0 → 7 (non-vintage is treated as ready now) |
| White | 0 → 3 |
| Rosé | 0 → 2 |
| Light red (Pinot Noir, Gamay) | 1 → 6 |
| Medium red (GSM/Rhône blends, Grenache, Tempranillo, Merlot…) | 2 → 10 |
| Bold red (Cabernet, Bordeaux blends, Shiraz, Nebbiolo…) | 3 → 18 |
| Unknown | 0 → 8 |

Classification favours a blend's most age-worthy component (a Cabernet blend is
bold), with one exception: a Grenache-led Rhône blend stays medium even though it
contains Syrah. These are deliberately broad defaults; record a window on a
bottle to override the estimate.

## Architecture

```
com.winecellar
├── data/     Room — Wine entity, WineDao, WineDatabase, WineRepository, SeedLoader
├── domain/   Pure logic — WineStyle, DrinkWindow, WineFilters (fully unit-tested)
└── ui/       Jetpack Compose — screens, WineViewModel, theme
```

- **Database** — Room (SQLite), no network access. Seeded once from
  `assets/wines_seed.json`.
- **State** — a single `WineViewModel` exposes `StateFlow`s the Compose screens
  collect; search/filter/sort and the drink-window logic are pure functions in
  `domain/`, so they're covered by fast JVM tests.

## Build

The CI-installed Gradle is used (the wrapper jar is intentionally not committed,
matching the repo's `.gitignore`). With Gradle 8.11.1 and JDK 17 on your PATH:

```bash
gradle testDebugUnitTest   # run unit + Robolectric tests
gradle assembleDebug       # build a debug APK
```

Requires the Android SDK (API 36) via `ANDROID_SDK_ROOT` or a `local.properties`
with `sdk.dir=...`.

## Publishing to Google Play

Google Play uploads use an **App Bundle** (`.aab`), not an APK (APKs are only
for sideloading). To produce a signed bundle:

1. **Create an upload keystore** (once), and keep it out of the repo:
   ```bash
   keytool -genkeypair -v -keystore upload.keystore -alias wine \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Build the bundle** with the signing config supplied via env vars (the same
   `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` the Gradle config reads):
   ```bash
   gradle bundleRelease        # → app/build/outputs/bundle/release/app-release.aab
   ```
   In CI the release APK **and** AAB are built on every run (R8 validated); they
   are signed and uploaded as artifacts only when the `WINE_KEYSTORE_BASE64`,
   `WINE_KEYSTORE_PASSWORD`, `WINE_KEY_ALIAS`, and `WINE_KEY_PASSWORD` repo
   secrets are set.
3. In the **Play Console**, create the app, enable **Play App Signing**, upload
   the `.aab` to an Internal testing track first, complete the Store listing,
   Data Safety, and content-rating forms, then promote to Production.

Bump `versionCode` (strictly increasing) in `app/build.gradle.kts` for every
upload.

## Requirements

| | |
|---|---|
| Android | 10 (API 29) or higher |
| Internet | Only for the opt-in "Look up online" barcode lookup; not needed for anything else |
