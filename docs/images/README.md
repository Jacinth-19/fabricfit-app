# App screenshots and photo credits

## Current README: Heritage Luxe build

The main README uses the following **unedited screenshots of the installed Android app**. They are not generated UI mockups.

Source: GitHub Actions run [38155674595](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38155674595), commit `111856b`, captured-files check `114520293283`, **Android 33** (`app-journey (33, google_apis)`). That journey passed. The **Android 16 (API 36)** journey failed: the app process crashed during its first test, so no Android 16 captures are used here. The downloadable APK was not refreshed by this run.

| File | Screen | Git blob SHA |
|---|---|---|
| `heritage-luxe/00-home-light.png` | Home, light mode | `47d852ac7e836216026fc5222b8b87fdf0d64dfa` |
| `heritage-luxe/00-home-dark.png` | Home, dark mode | `822e80212c0183f4616a38eaa42b06674f1f5714` |
| `heritage-luxe/03-selected-fabric.png` | Fabric selection on the Adowa photograph | `819542bcc69d9a931248284339ae11bf681b52db` |
| `heritage-luxe/05-template.png` | Flat outfit template with the selected texture | `689e00da4fd5e25487b2e6fd9bf507a6c6819ff3` |
| `heritage-luxe/08-tryon-light.png` | Try On, light mode | `59efbdd5998b4156f71e1143e7e0687369e4875c` |
| `heritage-luxe/09-discover-dark.png` | Discover, dark mode | `dfccb40a02e2509ab9091fd74ba2797ba240aa0a` |

Known gap: the Profile capture in dark mode is byte-identical to the light-mode capture, so the Profile theme switch is **not shown as verified** and no Profile dark-mode screenshot is used.

### Photography

- Home: **“Kente fashion in Ghana”**, photographed by **Fquasie** at Afrochella Festival 2021. [Original source](https://commons.wikimedia.org/wiki/File:Kente_fashion_in_Ghana.jpg).
- Fabric selection and template: **“Adowa Dancer in Kente Cloth”**, photographed by **Bright Kwame Ayisi (Kwameghana)**. [Original source](https://commons.wikimedia.org/wiki/File:Adowa_Dancer_in_Kente_Cloth.jpg).
- Both photographs are licensed **[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/)**. The app crops/resizes the photos for display and uses selected photo pixels for the template. The photographic adaptations retain that licence. See the [bundled credits](../NtomaStudio/app/src/main/assets/editorial/CREDITS.md).
- These are attributed cultural photographs, not endorsements by their subjects. Subject/model-release status is unverified.

Screenshots are displayed smaller in the README; their original PNG pixels are preserved. Garment templates are flat layouts, not photoreal try-on or fit predictions.

## Earlier editorial-redesign screenshots (not shown in the main README)

`00-home-light.png`, `00-home-dark.png`, `03-selected-fabric.png` and `05-template.png` at this folder's top level are from the earlier editorial redesign, run [38147477400](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38147477400), commit `5a36be6`, Android 33. They do not show the Heritage Luxe design.

## Historical screenshots (not shown in the main README)

The older `select-fabric.png`, `fabric-analysis.png`, `outfit-template.png` and `texture-repeats.png` remain for historical reference only. They depict the **previous UI and a synthetic test illustration**, not real fashion photographs or the new design.

Source: successful [Actions run 38140615211](https://github.com/Jacinth-19/ntoma-studio/actions/runs/38140615211), commit `78266f0b56487fad61ebe0d4012d1f918bcacdc5`, captured-files check `114474572269` (Android 33).

| Historical file | Original capture | Git blob SHA |
|---|---|---|
| `select-fabric.png` | `03-selected-fabric.png` | `d7f2471b17e5746f99ce53c88637acd6de1ec837` |
| `fabric-analysis.png` | `04-analysis.png` | `c0f62a6210861812d82315f1795108c41ab9e6d4` |
| `outfit-template.png` | `05-template.png` | `f918be3c1b7bb81ceffb9a3898ed456076ee4404` |
| `texture-repeats.png` | `07-texture-scale.png` | `2bc9da7389ae72885dd034f364e1a679f850c8b8` |
