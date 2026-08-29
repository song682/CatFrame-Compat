package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * In-memory representation of a single MCPatcher/OptiFine CTM properties file.
 * <p>
 * Only the fields needed by the current implementation stages are parsed;
 * unknown keys are ignored for forward compatibility. A rule that fails
 * validation is kept with {@code valid=false} and an {@code invalidReason},
 * and the caller decides whether to log or drop it.
 */
@SideOnly(Side.CLIENT)
public final class CtmProperties {

    /** File base name without the .properties suffix (e.g. "glasspane"). */
    public final String name;
    /** Pack-relative directory of the properties file, normalized (e.g. "mcpatcher/ctm"). */
    public final String basePath;
    /** Pack-relative path of the properties file. */
    public final String packPath;
    /** Block registry names this rule applies to (numeric IDs already translated). */
    public final List<String> matchBlocks;
    /** Texture base names this rule applies to (flat keys, e.g. "glass"). */
    public final List<String> matchTiles;
    /** Normalized method: ctm / horizontal / vertical / horizontal+vertical / vertical+horizontal / top / random / repeat / fixed. */
    public final String method;
    /** Expanded tile list; numeric ranges are expanded, "<skip>"/"<default>" kept as-is. */
    public final List<String> tiles;
    /** Connect semantics: block / tile / state; null = derive from match kind (P2). */
    public final String connect;
    /** Raw faces filter, null = all faces. */
    public final String faces;
    /** Raw metadata filter values, empty = no filter. */
    public final List<String> metadatas;
    /** Whether this rule passed validation and can be used for rendering. */
    public final boolean valid;
    /** Human-readable reason when {@link #valid} is false. */
    public final String invalidReason;

    private CtmProperties(String name, String basePath, String packPath,
                          List<String> matchBlocks, List<String> matchTiles,
                          String method, List<String> tiles, String connect,
                          String faces, List<String> metadatas,
                          boolean valid, String invalidReason) {
        this.name = name;
        this.basePath = basePath;
        this.packPath = packPath;
        this.matchBlocks = Collections.unmodifiableList(matchBlocks);
        this.matchTiles = Collections.unmodifiableList(matchTiles);
        this.method = method;
        this.tiles = Collections.unmodifiableList(tiles);
        this.connect = connect;
        this.faces = faces;
        this.metadatas = Collections.unmodifiableList(metadatas);
        this.valid = valid;
        this.invalidReason = invalidReason;
    }

    /**
     * Parse a CTM properties file from an input stream.
     *
     * @param packPath pack-relative path of the properties file
     * @param in       open stream (not closed by this method)
     * @return parsed rule; {@link #valid} is false when the rule is not usable
     * @throws IOException on stream read errors
     */
    public static CtmProperties parse(String packPath, InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);
        return parse(packPath, props);
    }

    /**
     * Parse CTM properties from an already-loaded {@link Properties} instance.
     * <p>
     * Field semantics follow the OptiFine/MCPatcher format (see the compat
     * research document, section 3.2):
     * <ul>
     *   <li>{@code method} defaults to {@code ctm};</li>
     *   <li>{@code matchBlocks} defaults to the file base name (with a leading
     *       {@code block_} prefix stripped), numeric IDs are translated to
     *       registry names;</li>
     *   <li>{@code tiles} supports numeric ranges ({@code 0-4}) and keeps
     *       {@code <skip>}/{@code <default>} placeholders.</li>
     * </ul>
     */
    public static CtmProperties parse(String packPath, Properties props) {
        String name = baseName(packPath);
        String basePath = basePath(packPath);

        String method = props.getProperty("method");
        if (method == null) {
            method = "ctm";
        }
        method = method.trim().toLowerCase();

        List<String> matchBlocks = splitList(props.getProperty("matchBlocks"));
        if (matchBlocks.isEmpty()) {
            String derived = name;
            if (derived.startsWith("block_")) {
                derived = derived.substring("block_".length());
            }
            matchBlocks.add(derived);
        }
        for (int i = 0; i < matchBlocks.size(); i++) {
            matchBlocks.set(i, translateBlockId(matchBlocks.get(i)));
        }

        List<String> matchTiles = splitList(props.getProperty("matchTiles"));
        List<String> tiles = expandTiles(props.getProperty("tiles"));
        String connect = props.getProperty("connect");
        String faces = props.getProperty("faces");
        List<String> metadatas = splitList(props.getProperty("metadata"));

        int minTiles = minTilesFor(method);
        if (minTiles < 0) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas,
                    false, "unsupported method '" + method + "'");
        }
        if (tiles.isEmpty()) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas,
                    false, "no tiles specified");
        }
        if (tiles.size() < minTiles) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas,
                    false, "method " + method + " needs at least " + minTiles
                    + " tiles, got " + tiles.size());
        }
        if (matchBlocks.isEmpty() && matchTiles.isEmpty()) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas,
                    false, "no matchBlocks or matchTiles");
        }
        return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                method, tiles, connect, faces, metadatas, true, null);
    }

    /** Minimum tile count required by each supported method; -1 = unsupported method. */
    private static int minTilesFor(String method) {
        switch (method) {
            case "ctm":
                return 47;
            case "horizontal":
            case "vertical":
                return 4;
            case "horizontal+vertical":
            case "vertical+horizontal":
                return 7;
            case "top":
            case "random":
            case "repeat":
            case "fixed":
                return 1;
            default:
                return -1;
        }
    }

    /** File base name of a pack-relative path, without the .properties suffix. */
    private static String baseName(String packPath) {
        int slash = packPath.lastIndexOf('/');
        String file = slash >= 0 ? packPath.substring(slash + 1) : packPath;
        if (file.endsWith(".properties")) {
            file = file.substring(0, file.length() - ".properties".length());
        }
        return file;
    }

    /** Normalize the pack-relative directory (strips the assets/minecraft/ prefix). */
    static String basePath(String packPath) {
        String p = packPath;
        if (p.startsWith("assets/minecraft/")) {
            p = p.substring("assets/minecraft/".length());
        }
        int slash = p.lastIndexOf('/');
        return slash >= 0 ? p.substring(0, slash) : "";
    }

    /** Split a whitespace/comma-separated list; never returns null. */
    private static List<String> splitList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String token : raw.split("[,\\s]+")) {
            if (!token.isEmpty()) {
                out.add(token);
            }
        }
        return out;
    }

    /** Expand a tiles value: numeric ranges become individual indices, other tokens stay as-is. */
    private static List<String> expandTiles(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        for (String token : raw.split(",")) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }
            int dash = token.indexOf('-');
            if (dash > 0) {
                String lo = token.substring(0, dash).trim();
                String hi = token.substring(dash + 1).trim();
                if (isInteger(lo) && isInteger(hi)) {
                    int a = Integer.parseInt(lo);
                    int b = Integer.parseInt(hi);
                    if (a <= b && b - a < 512) {
                        for (int i = a; i <= b; i++) {
                            out.add(String.valueOf(i));
                        }
                        continue;
                    }
                }
            }
            out.add(token);
        }
        return out;
    }

    /** Translate a numeric block ID to its registry name; other tokens pass through. */
    private static String translateBlockId(String token) {
        if (!isInteger(token)) {
            return token;
        }
        Block block = (Block) Block.blockRegistry.getObjectById(Integer.parseInt(token));
        if (block == null) {
            return token;
        }
        String reg = Block.blockRegistry.getNameForObject(block);
        if (reg == null) {
            return token;
        }
        int colon = reg.indexOf(':');
        return colon >= 0 ? reg.substring(colon + 1) : reg;
    }

    private static boolean isInteger(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
