# Déploiement des récitateurs Coran — Tawkit Mobile

Ce document couvre les deux façons d'ajouter des récitateurs **après l'installation** de l'app (sans passer par un serveur HTTP externe) : import depuis une **clé USB**, et téléchargement depuis un **cloud gratuit** avec suivi/pause/reprise. Les récitateurs ajoutés ainsi sont lus **nativement** (fichier local `file://`), en **OGG Opus**.

## 1. Comment ça marche — il y a deux systèmes distincts

**Système historique (ancien, optionnel)** — les 10 récitateurs définis dans `JS_CUSTOM_DEFAULTS.ucReciters` (Abdulbasit, Husary, Tablawi, Ghamidi...) ne sont **pas** stockés sur l'appareil : l'app va chercher chaque fichier MP3 sur un **serveur HTTP** que vous devez faire tourner vous-même (script `server_cors.py` déjà présent à la racine du projet). Concrètement, pour que ces 10-là fonctionnent il faut :
1. Un PC (ou une box) allumé sur le même réseau Wi-Fi que la tablette/TV, avec un dossier contenant un sous-dossier par récitateur nommé **`C1` à `C10`** (correspondance fixée dans le code : C1=Abdulbasit, C2=Husary... C4=Ghamidi, etc.), chacun avec `001.mp3` à `114.mp3`.
2. Lancer sur ce PC : `python server_cors.py 8080 "C:\chemin\vers\CORAN"` (le dossier qui contient C1...C10).
3. Dans l'app : Réglages → Lecteur Coran → cocher **"Serveur"** et renseigner son adresse, ex. `http://192.168.1.50:8080` (l'IP du PC sur le réseau local — pas `127.0.0.1`, sauf si le serveur tourne sur l'appareil lui-même).

**Ce système est entièrement optionnel.** Si vous ne voulez pas faire tourner un serveur en permanence, **décochez "Serveur"** dans les réglages : l'app continuera de fonctionner normalement, simplement ces 10 récitateurs historiques seront indisponibles. Vous pouvez les remplacer par les mêmes (ou d'autres) récitateurs importés via USB ou cloud — voir ci-dessous — qui eux ne nécessitent aucun serveur.

**Nouveau système (recommandé, sans serveur)** — tout récitateur importé par USB ou téléchargé devient un récitateur **"device"** : il s'ajoute à la liste dans l'app (Réglages → Lecteur Coran → Gestionnaire de récitateurs), fichiers copiés une fois sur l'appareil, lecture 100 % locale, aucune configuration serveur/réseau requise.
- Fichiers stockés sur l'appareil dans : `Android/data/net.tawkit.mobile/files/reciters/<id>/`
- Téléchargement géré par WorkManager : pause = arrêt propre (le `.part` est gardé), reprise = repart du même octet (`Range: bytes=...`), annulation = nettoyage complet.

## 2. Format audio requis

Chaque sourate doit être un fichier **OGG Opus**, nommé sur 3 chiffres :

```
001.ogg  002.ogg  003.ogg  ...  114.ogg
```

Conversion depuis MP3 (ou autre) avec ffmpeg — qualité voix, fichiers légers :

```bash
ffmpeg -i 001.mp3 -c:a libopus -b:a 48k -vbr on -application voip 001.ogg
```

`48k` (VBR) donne une qualité très correcte pour de la récitation parlée, pour une taille proche de la moitié d'un MP3 128 kbps.

## 3. Clé USB — structure attendue

**Important : tout doit être sous un dossier nommé `CORAN`, jamais directement à la racine de la clé.** C'est désormais imposé par le code (`ReciterManager.importFromTree`) : si ce dossier est absent, l'import échoue avec un message explicite plutôt que d'importer n'importe quoi à la racine.

```
MaCleUSB/
└── CORAN/
    ├── Cheikh_Al_Ghamidi/
    │   ├── meta.json          (optionnel)
    │   ├── 001.ogg
    │   ├── 002.ogg
    │   ├── ...
    │   └── 114.ogg
    └── Cheikh_Al_Husary/
        ├── 001.ogg
        ├── ...
        └── 114.ogg
```

- Le **nom du dossier** (sous `CORAN/`) devient l'identifiant du récitateur (nettoyé automatiquement : minuscules, `_` à la place des caractères spéciaux).
- `meta.json` est optionnel, juste pour afficher un nom plus joli :
  ```json
  { "name": "Cheikh Saad Al-Ghamidi" }
  ```
- **Cas particulier** : si `CORAN/` contient directement les fichiers `001.ogg…114.ogg` (pas de sous-dossiers), il est traité comme **un seul** récitateur (structure plate) — pratique pour importer un seul nom à la fois.
- Un récitateur incomplet (sourates manquantes) reste importable ; seules les sourates présentes seront jouables.

**Procédure dans l'app :**
1. Brancher la clé USB sur l'appareil Android (via adaptateur OTG si besoin).
2. Réglages → Lecteur Coran → Gestionnaire de récitateurs → **"Importer depuis clé USB"**.
3. Dans le sélecteur Android, choisir **soit le dossier `CORAN` lui-même, soit son dossier parent** (ex. la racine `MaCleUSB`) — l'app détecte automatiquement `CORAN/` dans les deux cas. Si aucun dossier `CORAN` n'est trouvé, l'import est refusé avec un message d'erreur.
4. L'app copie chaque sous-dossier vers son stockage interne et affiche la progression ; aucune connexion internet n'est nécessaire.

## 4. Cloud gratuit (Google) — téléchargement avec suivi / pause / reprise

### Pourquoi Firebase Hosting (et pas Firebase Storage)

Depuis février 2026, **Cloud Storage for Firebase exige désormais un compte de facturation (plan Blaze)**, même si l'usage reste à 0 €. Le plan **Spark (gratuit, sans carte bancaire)** ne permet plus du tout d'utiliser les buckets de stockage.

→ Solution 100 % gratuite et sans carte : **Firebase Hosting**, qui reste sur le plan Spark gratuit avec :
- **10 Go de stockage**
- **10 Go de transfert/mois**
- HTTPS + CDN global, support natif des requêtes par plages d'octets (`Range`), indispensable pour la reprise de téléchargement.

C'est largement suffisant pour héberger une dizaine de récitateurs complets (~150-250 Mo chacun en Opus) et leurs téléchargements par les utilisateurs.

### Mise en place (une seule fois)

1. Créer un compte Google si besoin, puis un projet sur [console.firebase.google.com](https://console.firebase.google.com) (plan **Spark**, gratuit — ne pas cliquer sur "passer à Blaze").
2. Installer les outils (sur un PC) :
   ```bash
   npm install -g firebase-tools
   firebase login
   ```
3. Dans un dossier de travail :
   ```bash
   firebase init hosting
   ```
   - Choisir le projet créé à l'étape 1.
   - Dossier public : `public` (par défaut).
   - "Configure as a single-page app" → **Non**.
4. Préparer les fichiers dans `public/` :
   ```
   public/
   ├── catalog.json
   ├── ghamidi.zip
   ├── husary.zip
   └── ...
   ```
   Chaque `.zip` contient directement (ou dans un seul sous-dossier) `meta.json` + `001.ogg…114.ogg` pour **un seul** récitateur.
5. `catalog.json` — c'est exactement le format lu par l'app (`_rmLoadCatalog()` dans `custom.js`) :
   ```json
   {
     "reciters": [
       { "id": "ghamidi", "name": "Saad Al-Ghamidi", "url": "https://VOTRE-PROJET.web.app/ghamidi.zip" },
       { "id": "husary",  "name": "Mahmoud Al-Husary", "url": "https://VOTRE-PROJET.web.app/husary.zip" }
     ]
   }
   ```
   `id` doit être unique (il sert de nom de dossier sur l'appareil) ; `url` doit pointer vers le `.zip` une fois déployé.
6. Déployer :
   ```bash
   firebase deploy --only hosting
   ```
   Firebase donne une URL du type `https://VOTRE-PROJET.web.app/`.

### Configuration côté app

Réglages → Lecteur Coran → Gestionnaire de récitateurs → champ URL du catalogue → coller :
```
https://VOTRE-PROJET.web.app/catalog.json
```
Puis **"Mettre à jour la liste de téléchargement"**. Chaque récitateur du catalogue apparaît avec un bouton Télécharger / Pause-Reprise / Annuler et une barre de progression (%, ou Mo si la taille est inconnue) — tout est déjà géré nativement (`ReciterDownloadWorker`).

### Mettre à jour la liste plus tard

Ajouter un nouveau récitateur = déposer son `.zip` dans `public/`, ajouter une ligne dans `catalog.json`, puis `firebase deploy --only hosting`. Aucune modification de l'app n'est nécessaire — elle relit le catalogue à chaque clic sur "Mettre à jour".

### Alternative sans Google : GitHub Releases

Si la CLI Firebase semble lourde, **GitHub Releases** est une alternative gratuite, sans carte, sans limite de bande passante annoncée (limite de 2 Go par fichier) :
1. Créer un repo GitHub (public ou privé).
2. Créer une "Release", y glisser les `.zip` de chaque récitateur (utiliser `gh release upload` si un fichier dépasse 25 Mo via l'interface web).
3. Récupérer les liens directs des assets (clic droit → copier le lien) et les mettre dans `catalog.json`, hébergé lui aussi en asset de release ou sur Firebase Hosting.

### À éviter : Google Drive

Les liens "partage" de Google Drive ne supportent pas correctement les requêtes par plages (`Range`) et affichent une page d'avertissement antivirus au-delà d'une certaine taille — la reprise de téléchargement ne fonctionnera pas de façon fiable. À ne pas utiliser pour ce cas d'usage.

## 5. Récapitulatif

| Besoin | Solution |
|---|---|
| Ajouter des récitateurs sans connexion internet | Clé USB (§3) |
| Distribuer des récitateurs à tous les utilisateurs, gratuitement, avec suivi/pause/reprise | Firebase Hosting + `catalog.json` (§4) |
| Alternative simple sans outil Google | GitHub Releases (§4) |
| À éviter | Google Drive (liens de partage) |
