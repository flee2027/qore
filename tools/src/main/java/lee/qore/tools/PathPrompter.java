package lee.qore.tools;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * Ensures a set of filesystem paths is configured.
 *
 * On first run (or when a path is missing / points to a nonexistent file),
 * opens a Swing dialog to pick the file or directory.
 * Results are stored in paths.properties next to this file,
 * so subsequent runs don't prompt.
 *
 * Usage: PathPrompter <config-dir> <key1> [key2] [key3...]
 *   Keys ending with ".dir" open a directory picker; all others open a file picker.
 *
 * If the environment is headless, prints manual instructions and exits(1).
 */
public class PathPrompter {

    private static final String CONFIG_FILE = "paths.properties";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: PathPrompter <config-dir> <key1> [key2...]");
            System.exit(1);
        }

        File configDir = new File(args[0]);
        File configFile = new File(configDir, CONFIG_FILE);

        Properties props = new Properties();
        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
            }
        }

        // Detect missing keys first.
        boolean anyMissing = false;
        for (int i = 1; i < args.length; i++) {
            String existing = props.getProperty(args[i]);
            if (existing == null || existing.isBlank() || !new File(existing).exists()) {
                anyMissing = true;
                break;
            }
        }

        // Headless fallback: no dialogs possible, print instructions.
        if (anyMissing && GraphicsEnvironment.isHeadless()) {
            System.err.println("Headless environment — cannot prompt for paths.");
            System.err.println("Create " + configFile.getAbsolutePath() + " manually, e.g.:");
            for (int i = 1; i < args.length; i++) {
                System.err.println("  " + args[i] + "=/absolute/path/here");
            }
            System.exit(1);
        }

        boolean changed = false;
        for (int i = 1; i < args.length; i++) {
            String key = args[i];
            String existing = props.getProperty(key);
            if (existing == null || existing.isBlank() || !new File(existing).exists()) {
                String value = promptForPath(key);
                if (value == null) {
                    System.err.println("Cancelled by user: " + key);
                    System.exit(1);
                }
                props.setProperty(key, value);
                changed = true;
                System.out.println("  [SET] " + key);
                System.out.println("        " + value);
            } else {
                System.out.println("  [OK]  " + key);
                System.out.println("        " + existing);
            }
        }

        if (changed) {
            if (!configDir.exists()) configDir.mkdirs();
            try (OutputStream out = new FileOutputStream(configFile)) {
                props.store(out, "qore paths - auto-generated, do not commit");
            }
            System.out.println();
            System.out.println("Saved: " + configFile.getAbsolutePath());
        }
    }


    private static String promptForPath(String key) {
        final boolean isDir = key.endsWith(".dir");
        final String[] result = new String[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select path for: " + key);
                chooser.setFileSelectionMode(isDir
                        ? JFileChooser.DIRECTORIES_ONLY
                        : JFileChooser.FILES_ONLY);
                chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
                int res = chooser.showOpenDialog(null);
                if (res == JFileChooser.APPROVE_OPTION) {
                    result[0] = chooser.getSelectedFile().getAbsolutePath();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }
}