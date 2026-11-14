package jake2.game;

import jake2.qcommon.GameImports;
import jake2.qcommon.exec.cvar_t;
import org.junit.Test;

import java.util.HashMap;

import static jake2.game.GameBase.G_Find;
import static jake2.game.GameBase.findByClassName;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GameBaseTest {
    private static float parseFloatOrDefault(String s, float fallback) {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
    @Test
    public void addTwoSubgameEntityAndDoGFindShouldReturnMatchingEntity(){
        GameImports gameImports = mock(GameImports.class);
        when(gameImports.cvar(anyString(), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    String defaultValue = inv.getArgument(1);
                    cvar_t cv = new cvar_t();
                    cv.name   = inv.getArgument(0);
                    cv.string = defaultValue;
                    cv.flags  = inv.getArgument(2);
                    cv.value  = parseFloatOrDefault(defaultValue, 0f);
                    return cv;
                });

        GameExportsImpl gameExports = new GameExportsImpl(gameImports);

        SubgameEntity se1 = new SubgameEntity(0);
        se1.classname = "he-he";
        se1.inuse = true;

        SubgameEntity se2 = new SubgameEntity(1);
        se2.classname = "ara-ara";
        se2.inuse = true;

        gameExports.g_edicts[0] = se1;
        gameExports.g_edicts[1] = se2;
        gameExports.num_edicts = 2;

        EdictIterator result = G_Find(null, GameBase.findByClassName, "ara-ara", gameExports);

        assertNotNull(result);
        assertEquals("ara-ara", result.o.classname);
        assertSame(se2, result.o);
    }
    @Test
    public void addTwoSubgameEntityAndDoGFindNeedCountEntitys(){
        HashMap<String,Integer> entitys = new HashMap<String,Integer>();

        GameImports gameImports = mock(GameImports.class);
        when(gameImports.cvar(anyString(), anyString(), anyInt()))
                .thenAnswer(inv -> {
                    String defaultValue = inv.getArgument(1);
                    cvar_t cv = new cvar_t();
                    cv.name   = inv.getArgument(0);
                    cv.string = defaultValue;
                    cv.flags  = inv.getArgument(2);
                    cv.value  = parseFloatOrDefault(defaultValue, 0f);
                    return cv;
                });

        GameExportsImpl gameExports = new GameExportsImpl(gameImports);

        SubgameEntity se1 = new SubgameEntity(0);
        se1.classname = "monster";
        se1.inuse = true;

        SubgameEntity se2 = new SubgameEntity(1);
        se2.classname = "monster";
        se2.inuse = true;

        SubgameEntity se3 = new SubgameEntity(2);
        se3.classname = "item";
        se3.inuse = true;

        gameExports.g_edicts[0] = se1;
        gameExports.g_edicts[1] = se2;
        gameExports.g_edicts[2] = se3;
        gameExports.num_edicts = 3;

        EdictIterator entity = null;
        while ((entity = G_Find(entity, GameBase.findByClassName, "monster", gameExports)) != null){
            entitys.merge(entity.o.classname, 1, Integer::sum);
        }

        entity = null;
        while ((entity = G_Find(entity, GameBase.findByClassName, "item", gameExports)) != null){
            entitys.merge(entity.o.classname, 1, Integer::sum);
        }

        assertEquals(Integer.valueOf(2), entitys.get("monster"));
        assertEquals(Integer.valueOf(1), entitys.get("item"));


    }

    @Test
    public void checkClipVelocityNeedOneResult(){
        float[] in = {0.0f,0.0f,5.0f};
        float[] normal = {0.0f,0.0f,1.0f};
        float[] out = {99.0f,99.0f,99.0f};

        assertEquals(1,GameBase.ClipVelocity(in,normal,out,1));
        assertArrayEquals(new float[]{0.0f,0.0f,0.0f}, out, 0.0001f);
    }

}