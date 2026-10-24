# Fogata de Resurrección — Forge 1.20.1

## Compilar
1. Copia a esta carpeta los archivos del wrapper de Gradle del MDK oficial de Forge 1.20.1
   (`gradlew`, `gradlew.bat`, carpeta `gradle/`) — https://files.minecraftforge.net
2. Java 17, y ejecuta:  `./gradlew build`
3. El .jar queda en `build/libs/`. Ponlo en `mods/` (servidor y cliente deben tenerlo).

Para probar: `./gradlew runClient`

## Probar en singleplayer (rápido)
1. Instala Forge 1.20.1 (47.x), copia el `.jar` a la carpeta `mods/` y crea un mundo.
2. `/give @s revivalcampfire:resurrection_campfire`  (o búscala en la pestaña creativa) y colócala.
3. `/revivir demo` -> ves la animación completa con un jugador ficticio.
4. `/revivir testban Pepe` -> añade a "Pepe" a la lista de baneados. Click derecho en la fogata -> elige a Pepe -> Revivir.
   En singleplayer no hace falta tener trucos activados para usar la fogata; los comandos `/revivir` también funcionan.

## Uso
- Craftea o dale con `/give @s revivalcampfire:resurrection_campfire`
  (receta: tótem en el centro arriba, fogata en el medio, arena de almas alrededor).
- Click derecho en la fogata -> menú con los jugadores baneados -> "Revivir".
- Comando (OP): `/revivir <jugador>` o `/revivir <jugador> <x y z>`
- Config en `config/revivalcampfire-common.toml`:
  - `banOnDeath` (false por defecto): banea al morir; déjalo en false si ya tienes tu propio sistema.
  - `requiredPermissionLevel` (2 = solo OPs, 0 = todos).

## Línea de tiempo del ritual (21 s)
Anillo en el cielo -> estrellas -> ondas teal/amarillas + espirales + arcos -> destello en cruz
-> haz de luz con la figura del jugador ascendiendo -> desbaneo + anuncio -> disipación.
