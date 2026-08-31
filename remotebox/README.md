# RemoteBox

Petite application Java (Swing + FlatLaf) pour piloter mes box Android sur le
tailnet : liste toutes les machines Tailscale avec leurs infos, et lance
**scrcpy** avec les bons paramètres par box (encodeur logiciel vs matériel,
bitrate, taille…).

## Prérequis

- **JDK 17+** (le wrapper Gradle télécharge Gradle tout seul).
- **Tailscale** installé (`C:\Program Files\Tailscale\tailscale.exe`) et connecté.
- **scrcpy** + **adb** (installés via `winget install Genymobile.scrcpy`, ou
  n'importe où dans le `PATH`).

## Lancer

```powershell
cd remotebox
.\gradlew.bat run
```

Ou construire une distribution autonome :

```powershell
.\gradlew.bat installDist
.\build\install\remotebox\bin\remotebox.bat
```

### Raccourci bureau

`RemoteBox.vbs` est un lanceur silencieux (pas de console) : il construit la
distribution au premier appel puis démarre l'app via `javaw` (JAVA_HOME).
Un raccourci **`RemoteBox.lnk`** (icône `icon/remotebox.ico`) est posé sur le
Bureau ; pour le recréer :

```powershell
$base = "$PWD"
$s = (New-Object -ComObject WScript.Shell).CreateShortcut("$([Environment]::GetFolderPath('Desktop'))\RemoteBox.lnk")
$s.TargetPath = "$env:SystemRoot\System32\wscript.exe"
$s.Arguments = "`"$base\RemoteBox.vbs`""
$s.WorkingDirectory = $base
$s.IconLocation = "$base\icon\remotebox.ico"
$s.Save()
```

## Configuration

Au premier lancement, `%APPDATA%\remotebox\` est créé avec :

| Fichier       | Contenu |
|---------------|---------|
| `config.json` | token API Tailscale, tailnet, chemins des exécutables, thème, intervalle de rafraîchissement |
| `boxes.json`  | profil scrcpy par box (clé = nom court MagicDNS, ex. `z6-aboubaker-ksibet`) |

### Token API Tailscale (optionnel mais recommandé)

Réglages → *Token API Tailscale*. À générer dans l'admin console Tailscale :
**Settings → Keys → Generate access token**. Sans token, l'app fonctionne
quand même via `tailscale status --json` local — elle a juste moins de
métadonnées (version du client, MAJ dispo, tags, expiration de clé).

Le token est stocké en clair dans `config.json` (modèle de confiance
personnel, comme le reste du repo).

## Indicateur d'état (pastille)

| Pastille | Sens |
|----------|------|
| ● vert   | En ligne selon le plan de contrôle Tailscale (`Online`) |
| ◐ ambre  | `Online=false` **mais** tunnel actif / handshake WireGuard < 3 min → **joignable quand même** |
| ○ gris   | Aucun signe de vie |

L'état ambre existe parce que le client Tailscale **Android** de ces box
signale souvent `Online=false` au plan de contrôle alors que le tunnel
fonctionne (c'est le cas de `z6-aboubaker-ksibet`). Après un lancement scrcpy
réussi, la liste se rafraîchit automatiquement ; sinon rafraîchissement toutes
les 30 s (configurable) + bouton **Rafraîchir**.

## Utilisation

- La liste se rafraîchit au démarrage puis toutes les 60 s (configurable).
- **Bouton `▶ scrcpy`** sur chaque ligne Android : fait `adb connect <ip>:5555`
  puis lance scrcpy avec le profil de la box.
- Barre d'outils sur la sélection : `▶ scrcpy`, `adb shell`, `Profil scrcpy…`
  (éditeur d'arguments avec préréglages matériel / logiciel), `Copier la commande`.
- Le journal en bas montre les commandes exécutées et leur sortie.

## Encodeur vidéo & repli automatique

Beaucoup de box Android bon marché (Allwinner en particulier) ont un encodeur
H.264 **matériel** qui refuse la configuration de scrcpy —
`MediaCodec.configure` lève `IllegalArgumentException`, scrcpy réessaie en
`-m1024`/`-m800` puis abandonne (« Demuxer error ») : **aucune fenêtre
n'apparaît**.

RemoteBox surveille les premières secondes de scrcpy dans le journal :

- succès détecté (`INFO: Texture: …`) → fenêtre affichée, rien à faire ;
- échec d'encodage détecté (et le profil ne fixe pas d'encodeur) → **relance
  automatique avec l'encodeur logiciel** `c2.android.avc.encoder`, et ce choix
  est **enregistré dans le profil** de la box (les lancements suivants sont
  directs).

Cas connu : `z6-aboubaker-ksibet` (Allwinner H618) — encodeur HW cassé pour
scrcpy, profil pré-réglé en logiciel. `al-hidaya` (Amlogic) — encodeur HW OK.

## Profils par défaut pré-remplis

| Box (clé MagicDNS)        | Encodeur | Raison |
|--------------------------|----------|--------|
| `z6-aboubaker-ksibet`    | logiciel | H618 — encodeur HW cassé pour scrcpy (vérifié) |
| `al-hidaya`              | matériel | Amlogic — encodeur HW OK (vérifié) |
| `sambox-mosquee-youssef`, `x96q-pro1`, `mediouni` | matériel | non testé ; repli auto si l'encodeur HW échoue |
| *(toute autre box)*      | matériel | défaut, repli auto |

Le profil « logiciel » ajoute
`--video-codec=h264 --video-encoder=c2.android.avc.encoder` et réduit
taille / bitrate / fps.

## Arborescence

```
src/main/java/net/tawkit/remotebox/
  App.java                  point d'entrée, thème FlatLaf
  core/                     AppPaths, Json, ProcessRunner
  config/                   AppConfig, ConfigStore, Tools (localisation des exe)
  model/                    Device, BoxProfile, BoxProfiles
  tailscale/                TailscaleCli, TailscaleApi, DeviceService (fusion)
  scrcpy/                   ScrcpyService (adb connect + spawn scrcpy)
  ui/                       MainFrame, DeviceTableModel, ButtonColumn,
                            SettingsDialog, ProfileDialog
src/main/resources/icons/   remotebox-{16..256}.png — icône de fenêtre (setIconImages)
icon/remotebox.ico          icône multi-résolution du raccourci Windows
```

L'icône (écran + triangle *play* + arcs de diffusion = mirroring à distance)
est régénérée par `scripts/make-icon.ps1` si besoin.
