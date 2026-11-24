package jake2.server;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class GameModLoader {
    public static URLClassLoader loadMod(String basedir, String game){
        URLClassLoader result = null;
        if (basedir != null && game != null){
            File jar = new File(basedir, game.equals("baseq2") ? "baseq2.jar" : game + ".jar");
            if (jar == null){
                return result;
            }
            URL jarUrl;
            try {
                jarUrl = jar.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            result = new URLClassLoader(new URL[]{jarUrl}, ClassLoader.getSystemClassLoader());
        }
        return result;
    }
}
