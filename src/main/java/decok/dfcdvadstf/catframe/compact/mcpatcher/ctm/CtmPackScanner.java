package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Enumerates CTM properties files across every active resource pack.
 * <p>
 * 1.7.10's {@link net.minecraft.client.resources.IResourceManager} has no
 * directory-listing API (resources can only be fetched by exact path), so the
 * pack directories must be inspected by introspecting pack instances:
 * <ul>
 *   <li>enabled packs from {@code ResourcePackRepository.getRepositoryEntriesAll()};</li>
 *   <li>{@code Minecraft.mcDefaultResourcePack} (public field);</li>
 *   <li>the private static {@code Minecraft.defaultResourcePacks} array
 *       (mod jars etc.).</li>
 * </ul>
 * Folder packs are walked via {@code AbstractResourcePack.resourcePackFile},
 * zip packs via {@code FileResourcePack.resourcePackZipFile}. Both are private
 * fields that we only read (never modify), which is the sole accepted
 * exception to the project's no-reflection rule.
 */
@SideOnly(Side.CLIENT)
public final class CtmPackScanner {

    /** Supported CTM property locations, from newest to oldest convention. */
    private static final String[] CTM_PREFIXES = {
            "assets/minecraft/mcpatcher/ctm/",
            "assets/minecraft/optifine/ctm/",
            "mcpatcher/ctm/"
    };

    /** Field lookup cache keyed by the starting class. */
    private static final Map<Class<?>, Field> FIELD_CACHE = new HashMap<>();

    private CtmPackScanner() {
    }

    /**
     * Scan all active resource packs for CTM properties files.
     *
     * @return one {@link CtmPackFile} per pack that contains at least one
     *         CTM properties file; empty list when the client is not ready
     */
    public static List<CtmPackFile> scan() {
        List<CtmPackFile> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return result;
        }
        if (mc.getResourcePackRepository() != null) {
            for (Object obj : mc.getResourcePackRepository().getRepositoryEntriesAll()) {
                if (obj instanceof ResourcePackRepository.Entry) {
                    collect(((ResourcePackRepository.Entry) obj).getResourcePack(), result, seen);
                }
            }
        }
        if (mc.mcDefaultResourcePack != null) {
            collect(mc.mcDefaultResourcePack, result, seen);
        }
        IResourcePack[] defaults = readDefaultResourcePacks();
        if (defaults != null) {
            for (IResourcePack pack : defaults) {
                collect(pack, result, seen);
            }
        }
        return result;
    }

    /** Collect CTM properties of one pack (deduplicated by pack name). */
    private static void collect(IResourcePack pack, List<CtmPackFile> out, Set<String> seen) {
        if (pack == null) {
            return;
        }
        String packName = pack.getPackName();
        if (packName == null || !seen.add(packName)) {
            return;
        }
        File folder = readFolder(pack);
        if (folder != null && folder.isDirectory()) {
            List<String> paths = new ArrayList<>();
            walkFolder(folder, "", paths);
            if (!paths.isEmpty()) {
                out.add(new CtmPackFile(packName, folder, null, paths));
            }
            return;
        }
        ZipFile zip = readZip(pack);
        if (zip != null) {
            List<String> paths = new ArrayList<>();
            walkZip(zip, paths);
            if (!paths.isEmpty()) {
                out.add(new CtmPackFile(packName, null, zip, paths));
            }
        }
    }

    /** Recursively walk a folder pack and collect CTM properties paths. */
    private static void walkFolder(File dir, String rel, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            String childRel = rel.isEmpty() ? child.getName() : rel + "/" + child.getName();
            if (child.isDirectory()) {
                walkFolder(child, childRel, out);
            } else if (childRel.endsWith(".properties") && isCtmPath(childRel)) {
                out.add(childRel);
            }
        }
    }

    /** Walk a zip pack and collect CTM properties entries. */
    private static void walkZip(ZipFile zip, List<String> out) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String name = entry.getName();
            if (name.endsWith(".properties") && isCtmPath(name)) {
                out.add(name);
            }
        }
    }

    private static boolean isCtmPath(String packPath) {
        for (String prefix : CTM_PREFIXES) {
            if (packPath.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /** Read the private {@code AbstractResourcePack.resourcePackFile} field (File type). */
    private static File readFolder(IResourcePack pack) {
        Field f = findField(pack.getClass(), File.class);
        if (f == null) {
            return null;
        }
        try {
            return (File) f.get(pack);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /** Read the private {@code FileResourcePack.resourcePackZipFile} field (ZipFile type). */
    private static ZipFile readZip(IResourcePack pack) {
        Field f = findField(pack.getClass(), ZipFile.class);
        if (f == null) {
            return null;
        }
        try {
            return (ZipFile) f.get(pack);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /** Read the private static {@code Minecraft.defaultResourcePacks} array. */
    private static IResourcePack[] readDefaultResourcePacks() {
        Field f = findStaticField(Minecraft.class, IResourcePack[].class);
        if (f == null) {
            return null;
        }
        try {
            return (IResourcePack[]) f.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /** Find a field of the given type on the class or any superclass (cached). */
    private static Field findField(Class<?> owner, Class<?> fieldType) {
        Field cached = FIELD_CACHE.get(owner);
        if (cached != null) {
            return cached;
        }
        Field found = null;
        for (Class<?> c = owner; c != null && found == null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == fieldType) {
                    found = f;
                    break;
                }
            }
        }
        if (found != null) {
            try {
                found.setAccessible(true);
            } catch (Exception ignored) {
                // SecurityManager or module restrictions: treat as absent
            }
            FIELD_CACHE.put(owner, found);
        }
        return found;
    }

    /** Find a static field of the given type (cached). */
    private static Field findStaticField(Class<?> owner, Class<?> fieldType) {
        Field cached = FIELD_CACHE.get(owner);
        if (cached != null) {
            return cached;
        }
        Field found = null;
        for (Field f : owner.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == fieldType) {
                found = f;
                break;
            }
        }
        if (found != null) {
            try {
                found.setAccessible(true);
            } catch (Exception ignored) {
                // SecurityManager or module restrictions: treat as absent
            }
            FIELD_CACHE.put(owner, found);
        }
        return found;
    }

    /**
     * A pack that contains CTM properties, with the means to open them.
     * Folder packs use {@code folder}; zip packs use {@code zip}.
     */
    public static final class CtmPackFile {
        /** Pack display name (deduplication key). */
        public final String packName;
        private final File folder;
        private final ZipFile zip;
        /** Pack-relative paths of all CTM properties files in this pack. */
        public final List<String> properties;

        CtmPackFile(String packName, File folder, ZipFile zip, List<String> properties) {
            this.packName = packName;
            this.folder = folder;
            this.zip = zip;
            this.properties = properties;
        }

        /**
         * Open a pack-relative path for reading.
         *
         * @throws IOException when the path is missing or unreadable
         */
        public InputStream open(String packPath) throws IOException {
            if (folder != null) {
                return new FileInputStream(new File(folder, packPath));
            }
            ZipEntry entry = zip.getEntry(packPath);
            if (entry == null) {
                throw new IOException("Missing entry " + packPath + " in pack " + packName);
            }
            return zip.getInputStream(entry);
        }
    }
}
