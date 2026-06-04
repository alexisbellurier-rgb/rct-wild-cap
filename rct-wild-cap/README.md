# RCT Wild Level Cap

Mod Fabric pour Minecraft 1.21.1 qui **empêche les Pokémon sauvages de spawner
au-dessus du level cap RCT du joueur le plus proche**.

## Fonctionnement

Quand Cobblemon tente de faire spawner un Pokémon sauvage, ce mod :
1. Identifie le joueur le plus proche dans un rayon de 128 blocs
2. Lit son level cap via l'API RCT (`TrainerManager.getData(player).getLevelCap()`)
3. Compare le niveau maximum possible du Pokémon au level cap
4. **Annule le spawn** si le niveau max >= level cap

> Si aucun joueur n'est à moins de 128 blocs, le spawn se fait normalement.

## Dépendances requises (sur le serveur)

| Mod | Version minimale | Lien |
|---|---|---|
| Fabric Loader | 0.16.5 | https://fabricmc.net |
| Fabric API | 0.102.0+1.21.1 | Modrinth |
| Cobblemon | 1.6.1+1.21.1 | Modrinth |
| RCT API (`rctapi`) | 0.10.6-beta | Modrinth |
| RCT Mod (`rctmod`) | 0.16.x | Modrinth |

---

## Compilation

### Prérequis
- **Java 21** (JDK, pas JRE) — https://adoptium.net
- **Git** (optionnel, pour cloner)

### Étapes

```bash
# 1. Se placer dans le dossier du projet
cd rct-wild-cap

# 2. Lancer le build (télécharge automatiquement Gradle, MC, mappings)
#    Première compilation : ~5-10 min selon la connexion
./gradlew build          # Linux / macOS
gradlew.bat build        # Windows

# 3. Le .jar compilé se trouve dans :
#    build/libs/rct-wild-cap-1.0.0.jar
```

> **Note Windows :** si `gradlew.bat` n'existe pas, télécharge le wrapper :
> ```
> gradle wrapper
> ```
> Ou utilise IntelliJ IDEA qui gère ça automatiquement.

### Avec IntelliJ IDEA (recommandé)

1. Ouvre le dossier `rct-wild-cap/` comme projet Gradle
2. Laisse IntelliJ résoudre les dépendances (barre de progression en bas)
3. Dans le panneau **Gradle** (à droite) → `Tasks > build > build`
4. Le `.jar` apparaît dans `build/libs/`

---

## Installation

1. Copier `build/libs/rct-wild-cap-1.0.0.jar` dans le dossier `mods/` du serveur
2. S'assurer que `rctapi`, `rctmod` et `cobblemon` sont aussi dans `mods/`
3. Redémarrer le serveur

Ce mod est **server-side uniquement** — les clients n'ont pas besoin de l'installer.

---

## Dépannage

### Erreur `NoSuchMethodException: getCtx()`
Les noms des méthodes Kotlin compilées peuvent varier selon la version de Cobblemon.
Ouvre le `.jar` de Cobblemon avec un décompilateur (ex. [Recaf](https://github.com/Col-E/Recaf))
et vérifie le nom exact dans `PokemonSpawnAction`. Remplace `getCtx()` dans le
Mixin par le nom trouvé.

### Erreur `getLevelCap()` introuvable
Même démarche : ouvre le `.jar` de RCT API et inspecte `TrainerPlayerData`.
La méthode peut s'appeler `levelCap()` ou `getPlayerLevelCap()` selon la version.

### Les Pokémon spawnent encore trop forts
Vérifie dans les logs serveur qu'on voit bien les messages `[RCT Wild Cap] Spawn bloqué`.
Si non, le mod ne charge pas — vérifie que toutes les dépendances sont présentes.

---

## Structure du projet

```
rct-wild-cap/
├── build.gradle                          ← dépendances et config build
├── gradle.properties                     ← versions (modifier ici si besoin)
├── settings.gradle
└── src/main/
    ├── java/com/rctWildCap/
    │   ├── RctWildCapMod.java            ← point d'entrée du mod
    │   └── mixin/
    │       └── PokemonSpawnActionMixin.java  ← logique principale
    └── resources/
        ├── fabric.mod.json               ← métadonnées Fabric
        └── rct_wild_cap.mixins.json      ← déclaration du mixin
```
