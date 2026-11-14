package jake2.game.flee;

import jake2.game.GameExportsImpl;
import jake2.qcommon.GameImports;
import jake2.qcommon.edict_t;

import java.util.List;

import static jake2.qcommon.Defines.PRINT_HIGH;

public class FleeGameExports extends GameExportsImpl {
    public FleeGameExports(GameImports imports) {
        super(imports);
        System.out.println("flee started!");
    }

    @Override
    public void G_RunFrame() {
        super.G_RunFrame();
    }

    @Override
    public void ClientCommand(edict_t player, List<String> args) {
        if (args.isEmpty()) return;
        String cmd = args.get(0).toLowerCase();

        switch (cmd){
            case "yo":
                gameImports.cprintf(player,PRINT_HIGH,"\nYo from flee game!");
                return;
        }

        super.ClientCommand(player, args);
    }
}
