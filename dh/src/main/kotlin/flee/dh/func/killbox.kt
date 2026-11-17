package flee.dh.func

import flee.dh.GameExportsImpl
import flee.dh.GameUtil
import flee.dh.SubgameEntity
import flee.dh.adapters.SuperAdapter.Companion.registerUse
import jake2.qcommon.Defines

/**
 * QUAKED func_killbox (1 0 0) ? Kills everything inside when fired,
 * irrespective of protection.
 */
fun funcKillbox(self: SubgameEntity, game: GameExportsImpl) {
    game.gameImports.setmodel(self, self.model)
    self.use = killboxUse
    self.svflags = Defines.SVF_NOCLIENT
}

private val killboxUse = registerUse("use_killbox") { self, _, _, game ->
    GameUtil.KillBox(self, game)
}
