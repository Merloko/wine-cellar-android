# Wine Cellar

An offline-first Android app for managing a home wine cellar. Add wines, find
exactly where a bottle is stored, and see at a glance what's ready to drink now.

No account, no cloud, no network permission — every bottle lives in an on-device
SQLite database.

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
- **Export** — share the whole cellar as CSV or JSON via the Android share
  sheet. The JSON matches the seed schema, so an export can be re-imported.

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
matching the repo's `.gitignore`). With Gradle 8.6 and JDK 17 on your PATH:

```bash
cd WineCellar
gradle testDebugUnitTest   # run unit + Robolectric tests
gradle assembleDebug       # build a debug APK
```

Requires the Android SDK (API 34) via `ANDROID_SDK_ROOT` or a `local.properties`
with `sdk.dir=...`.

## Requirements

| | |
|---|---|
| Android | 10 (API 29) or higher |
| Internet | **Never required** |
