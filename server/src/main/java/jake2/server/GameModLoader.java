package jake2.server;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GameModLoader {
    // Cache loaded classes to avoid ClassCastException on map changes
    private static final Map<String, Class<?>> classCache = new HashMap<>();

    public static Class<?> getGameClass(String basedir, String gamedir) throws Exception {
        if (gamedir == null || gamedir.isEmpty()) {
            gamedir = "baseq2";
        }

        // Return cached class if already loaded
        if (classCache.containsKey(gamedir)) {
            return classCache.get(gamedir);
        }

        String jarName = "baseq2".equals(gamedir) ? "baseq2.jar" : gamedir + ".jar";
        File jarFile = new File(basedir, gamedir + "/" + jarName);

        Class<?> gameClass;

        if (jarFile.exists()) {
            URL jarUrl = jarFile.toURI().toURL();
            // Parent classloader provides access to engine classes (qore.jar)
            URLClassLoader customLoader = new URLClassLoader(new URL[]{jarUrl}, ClassLoader.getSystemClassLoader());

            try (InputStream is = customLoader.getResourceAsStream("qore-module.properties")) {
                if (is == null) {
                    throw new IllegalStateException("Where is qore-module.properties in " + jarName);
                }
                Properties props = new Properties();
                props.load(is);
                String className = props.getProperty("entry.class");
                if (className == null || className.trim().isEmpty()) {
                    throw new IllegalStateException("Where 'entry.class' key in qore-module.properties?");
                }
                gameClass = customLoader.loadClass(className);
            }
        } else {
            // Fallback for IntelliJ development mode
            String fallbackClass = "baseq2".equals(gamedir) ? "jake2.game.GameExportsImpl" : "flee.dh.GameExportsImpl";
            gameClass = Class.forName(fallbackClass);
        }

        // Store in cache
        classCache.put(gamedir, gameClass);
        return gameClass;
    }
}