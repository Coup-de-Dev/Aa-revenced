# AA-ReVanced — Android Auto Compatibility Patch

Custom ReVanced patch that makes **any APK** compatible with **Android Auto**.

## What it does

This patch modifies the APK's `AndroidManifest.xml` to inject Android Auto support:

1. **Car app metadata** — Adds `com.google.android.gms.car.application` meta-data pointing to an automotive app descriptor
2. **Automotive app descriptor** — Injects `res/xml/automotive_app_desc.xml` declaring media and notification support
3. **Car dock/mode categories** — Adds `CAR_DOCK` and `CAR_MODE` categories to the main launcher activity so Android Auto can discover and launch the app
4. **Automotive feature flag** — Declares `android.hardware.type.automotive` as optional (won't block phone installs)
5. **Foreground service permission** — Ensures the app can run media playback in the background on Auto

## Installation dans ReVanced Manager

### Méthode 1 : Depuis le stockage (recommandé)

1. Télécharge le fichier `.rvp` depuis les [Releases](../../releases/latest)
2. Ouvre **ReVanced Manager** → **Patch bundles** (ou Sources)
3. Appuie sur **"Add from storage"** / **"Ajouter depuis le stockage"**
4. Sélectionne le fichier `.rvp` téléchargé
5. Choisis ton APK cible (YouTube, Spotify, etc.)
6. Active le patch **"Android Auto compatibility"**
7. Patche et installe !

### Méthode 2 : Depuis une URL (nécessite GitHub Pages)

1. Active GitHub Pages sur ce repo (Settings → Pages → Source: `docs/` folder)
2. Dans ReVanced Manager → Patch bundles → Add from URL :
   ```
   https://coup-de-dev.github.io/Aa-revenced/patches.json
   ```
3. Le manager téléchargera automatiquement les patches

## Build from source

```bash
./gradlew :patches:jar
```

Le JAR sera dans `patches/build/libs/aa-revanced-patches-1.0.0.jar`

## Créer une release

```bash
git tag v1.0.0
git push origin v1.0.0
```

Le workflow GitHub Actions build et publie automatiquement la release avec les fichiers `.jar` et `.rvp`.

## Use case

Parfait pour les apps comme **YouTube ReVanced** (avec les patchs anti-pub) que tu veux utiliser sur l'écran Android Auto de ta voiture à l'arrêt.

## Notes

- Ce patch fonctionne au mieux avec les apps qui ont déjà des capacités de lecture média
- Android Auto peut nécessiter que l'app implémente `MediaBrowserService` pour une intégration média complète
- Le patch est non-destructif : il ajoute des entrées, n'en supprime jamais
- `android:required="false"` sur la feature automotive garantit que l'app s'installe toujours normalement sur le téléphone
