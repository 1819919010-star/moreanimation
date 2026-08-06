package com.github.tartaricacid.moreanimation.compat.util;

import com.github.tartaricacid.moreanimation.Example;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CustomPackInstaller {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PACK_PREFIX = "tlm_custom_pack/";
    private static final String PACK_NAME = "touhou_little_maid-1.0.0";
    private static final String PACK_ROOT = PACK_PREFIX + PACK_NAME + "/";
    private static final Path PACK_FOLDER = FMLPaths.GAMEDIR.get().resolve(PACK_PREFIX);
    private static final Path PACK_DIR = PACK_FOLDER.resolve(PACK_NAME);
    private static final Path PACK_ASSETS_DIR = PACK_DIR.resolve("assets");
    private static final String ASSETS_ROOT = PACK_ROOT + "assets/";

    public static void install() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        int installed = 0;

        try {
            Path modPath = locateModPath();
            if (modPath == null) {
                LOGGER.error("Cannot locate mod path for custom pack installation");
                return;
            }
            if (Files.isRegularFile(modPath)) {
                installed = installFromJar(modPath);
            } else if (Files.isDirectory(modPath)) {
                installed = installFromDir(modPath);
            } else {
                LOGGER.error("Mod path is neither file nor directory: {}", modPath);
                return;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to install custom pack", e);
            return;
        }

        LOGGER.info("Custom pack install complete into {}: {} files installed", PACK_ASSETS_DIR, installed);
    }

    private static Path locateModPath() {
        try {
            if (ModList.get() != null) {
                Path modPath = ModList.get().getModContainerById(Example.MOD_ID)
                        .map(container -> container.getModInfo().getOwningFile().getFile().getFilePath())
                        .orElse(null);
                if (modPath != null && Files.exists(modPath)) {
                    return modPath;
                }
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to resolve mod path via ModList", e);
        }

        try {
            URL codeSource = CustomPackInstaller.class.getProtectionDomain().getCodeSource().getLocation();
            if (codeSource == null) {
                return null;
            }
            String path = codeSource.getPath();
            if (path != null) {
                int index = path.indexOf("%23");
                if (index >= 0) {
                    return Path.of(decodePath(path.substring(0, index)));
                }
                try {
                    return Path.of(codeSource.toURI());
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse code source URI", e);
        }
        return null;
    }

    private static String decodePath(String path) {
        String encoded = path.replace("+", "%2B");
        return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int installFromJar(Path modJar) throws IOException {
        int installed = 0;
        try (JarFile jar = new JarFile(modJar.toFile())) {
            for (JarEntry entry : Collections.list(jar.entries())) {
                String name = entry.getName();
                if (!name.startsWith(ASSETS_ROOT) || entry.isDirectory()) {
                    continue;
                }
                Path target = PACK_DIR.resolve(name.substring(PACK_ROOT.length()));
                target.getParent().toFile().mkdirs();
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    installed++;
                }
            }
        }
        return installed;
    }

    private static int installFromDir(Path modRoot) throws IOException {
        int installed = 0;
        Path root = modRoot.resolve(PACK_ROOT);
        if (!Files.isDirectory(root)) {
            LOGGER.warn("Dev custom pack source not found in: {}", modRoot);
            return 0;
        }
        try (var stream = Files.walk(root)) {
            for (Path file : (Iterable<Path>) stream.filter(Files::isRegularFile)::iterator) {
                Path rel = root.relativize(file);
                if (!rel.startsWith("assets")) {
                    continue;
                }
                Path target = PACK_DIR.resolve(rel.toString());
                target.getParent().toFile().mkdirs();
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                installed++;
            }
        }
        return installed;
    }
}
