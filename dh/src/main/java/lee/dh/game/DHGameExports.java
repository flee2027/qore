// mygame/src/main/java/lee/qore/game/MyGameExports.java
package lee.dh.game;

import jake2.game.GameExportsImpl;
import jake2.qcommon.Com;
import jake2.qcommon.GameImports;

/**
 * MyGameExports
 *
 * Custom game logic that extends original Quake 2 logic.
 * Inherits all original entities and functions, but allows adding custom ones.
 */
public class DHGameExports extends GameExportsImpl {

    public DHGameExports(GameImports imports) {
        super(imports);
    }


    /*
     * ================
     * SpawnEntities
     *
     * Called when a map is loaded.
     * ================
     */
    @Override
    public void SpawnEntities(String mapname, String entstring, String entfilename) {
        // Вызываем оригинальный спавн (все сущности Quake 2)
        super.SpawnEntities(mapname, entstring, entfilename);

        Com.Printf("MyGame: Map loaded: " + mapname + "\n");

        // Здесь можно добавить пост-обработку карты
        // Например, найти все "info_horrorspawn" и инициализировать их
    }
}