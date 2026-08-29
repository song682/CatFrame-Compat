package decok.dfcdvadstf.catframe.compact.mcpatcher.ctm;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

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
    /**
     * Resolved connect semantics for the tile selector:
     * 1=block (same Block), 2=tile (same base texture name), 3=material,
     * 4=state (same block + metadata); 0 = no connections (P2 selector
     * never joins neighbours). Null/unknown values fall back to
     * detectConnect (matchBlocks wins over matchTiles), mirroring
     * OptiFine/MCPatcher.
     */
    public final int connectType;
    /**
     * Faces bit mask (bit 0-5 = DOWN/UP/NORTH/SOUTH/WEST/EAST ordinal),
     * -1 = all faces. Parsed from the faces property (numeric or name
     * tokens); unknown tokens are ignored.
     */
    public final int facesMask;
    /** Expanded metadata filter values; empty = no filter. */
    public final int[] metadataValues;
    /** Whether this rule passed validation and can be used for rendering. */
    public final boolean valid;
    /** Human-readable reason when {@link #valid} is false. */
    public final String invalidReason;
    /** Method-specific properties (repeat grid, random seeding, filters). */
    public final MethodProps methodProps;

    private CtmProperties(String name, String basePath, String packPath,
                          List<String> matchBlocks, List<String> matchTiles,
                          String method, List<String> tiles, String connect,
                          String faces, List<String> metadatas,
                          int connectType, int facesMask, int[] metadataValues,
                          MethodProps methodProps,
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
        this.connectType = connectType;
        this.facesMask = facesMask;
        this.metadataValues = metadataValues;
        this.methodProps = methodProps;
        this.valid = valid;
        this.invalidReason = invalidReason;
    }

    /**
     * Method-specific properties (P4): repeat grid size, random weights and
     * seeding, y-height and biome filters. Parsed for every rule with
     * OptiFine 1.17.1 {@code ConnectedProperties} defaults; only the fields
     * used by the rule's method are consulted.
     */
    static final class MethodProps {

        /** repeat: tile grid width; -1 = unset. */
        final int width;
        /** repeat: tile grid height; -1 = unset. */
        final int height;
        /** random: per-tile weights, null = uniform distribution. */
        final int[] weights;
        /** random: prefix sums over {@link #weights} (normalized to the tile count). */
        final int[] sumWeights;
        /** random: total weight, always >= 1. */
        final int sumAllWeights;
        /** random: extra hash rounds on the position seed (0-9). */
        final int randomLoops;
        /** random: 1 = none, 2 = opposite faces, 6 = all faces (OptiFine parseSymmetry). */
        final int symmetry;
        /** random: seed from the bottom of the same-block column below. */
        final boolean linked;
        /** y-height filter as flattened [lo, hi] pairs; null = no filter. */
        final int[] heights;
        /** biome name filter (normalized); null = no filter. */
        final String[] biomes;
        /** true when {@link #biomes} is an exclusion list (leading '!'). */
        final boolean biomesInvert;

        private MethodProps(int width, int height, int[] weights, int[] sumWeights,
                            int sumAllWeights, int randomLoops, int symmetry,
                            boolean linked, int[] heights, String[] biomes,
                            boolean biomesInvert) {
            this.width = width;
            this.height = height;
            this.weights = weights;
            this.sumWeights = sumWeights;
            this.sumAllWeights = sumAllWeights;
            this.randomLoops = randomLoops;
            this.symmetry = symmetry;
            this.linked = linked;
            this.heights = heights;
            this.biomes = biomes;
            this.biomesInvert = biomesInvert;
        }

        /** Defaults for rules that failed validation (never used for rendering). */
        static final MethodProps NONE = new MethodProps(-1, -1, null, null, 1,
                0, 1, false, null, null, false);
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
        int connectType = parseConnectType(connect, matchBlocks, matchTiles);
        int facesMask = parseFaces(faces);
        int[] metadataValues = parseMetadatas(metadatas);
        MethodProps methodProps = parseMethodProps(props, tiles);

        int minTiles = minTilesFor(method);
        if (minTiles < 0) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "unsupported method '" + method + "'");
        }
        if (tiles.isEmpty()) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "no tiles specified");
        }
        if (tiles.size() < minTiles) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "method " + method + " needs at least " + minTiles
                    + " tiles, got " + tiles.size());
        }
        if (matchBlocks.isEmpty() && matchTiles.isEmpty()) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "no matchBlocks or matchTiles");
        }
        if ("repeat".equals(method) && (methodProps.width <= 0 || methodProps.height <= 0)) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "repeat needs positive width and height");
        }
        if ("repeat".equals(method) && tiles.size() != methodProps.width * methodProps.height) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "repeat tile count does not equal width x height");
        }
        if ("random".equals(method) && (methodProps.randomLoops < 0 || methodProps.randomLoops > 9)) {
            return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                    method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                    methodProps, false, "randomLoops must be between 0 and 9");
        }
        return new CtmProperties(name, basePath, packPath, matchBlocks, matchTiles,
                method, tiles, connect, faces, metadatas, connectType, facesMask, metadataValues,
                methodProps, true, null);
    }

    /**
     * True when the given metadata passes the metadata filter
     * (an empty filter matches every value).
     */
    public boolean matchesMetadata(int meta) {
        if (metadataValues.length == 0) {
            return true;
        }
        for (int value : metadataValues) {
            if (value == meta) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the given face (Direction ordinal 0-5) passes the faces
     * filter (-1 = all faces). The side is remapped through the pillar axis
     * first (OptiFine fixSideByAxis), so faces written for Y-axis blocks
     * also apply to X/Z-axis pillars.
     */
    public boolean matchesFace(int side, int vertAxis) {
        if (facesMask < 0) {
            return true;
        }
        if (vertAxis != 0) {
            side = fixSideByAxis(side, vertAxis);
        }
        return ((1 << side) & facesMask) != 0;
    }

    /** Remap a face ordinal onto a rotated pillar axis (OptiFine fixSideByAxis). */
    private static int fixSideByAxis(int side, int vertAxis) {
        switch (vertAxis) {
            case 1:
                switch (side) {
                    case 0:
                        return 2;
                    case 1:
                        return 3;
                    case 2:
                        return 1;
                    case 3:
                        return 0;
                    default:
                        return side;
                }
            case 2:
                switch (side) {
                    case 0:
                        return 4;
                    case 1:
                        return 5;
                    case 4:
                        return 1;
                    case 5:
                        return 0;
                    default:
                        return side;
                }
            default:
                return side;
        }
    }

    /**
     * True when the block y passes the heights filter (flattened [lo, hi]
     * pairs; null = no filter).
     */
    public boolean matchesHeight(int y) {
        int[] heights = methodProps.heights;
        if (heights == null) {
            return true;
        }
        for (int i = 0; i < heights.length; i += 2) {
            if (y >= heights[i] && y <= heights[i + 1]) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when the biome at (x, z) passes the biomes filter (normalized
     * names; null = no filter). A leading '!' in the property makes the
     * list an exclusion list (OptiFine parseBiomes semantics).
     */
    public boolean matchesBiome(IBlockAccess world, int x, int z) {
        String[] biomes = methodProps.biomes;
        if (biomes == null) {
            return true;
        }
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        String name = biome == null ? null : normalizeBiomeName(biome.biomeName);
        boolean found = false;
        if (name != null) {
            for (String b : biomes) {
                if (b.equals(name)) {
                    found = true;
                    break;
                }
            }
        }
        return methodProps.biomesInvert != found;
    }

    /**
     * Resolve the connect property to its numeric semantics; null or
     * unknown values fall back to detection (matchBlocks wins, then
     * matchTiles), matching OptiFine's detectConnect.
     */
    private static int parseConnectType(String connect, List<String> matchBlocks, List<String> matchTiles) {
        if (connect != null) {
            String c = connect.trim().toLowerCase();
            if (c.equals("block")) {
                return 1;
            }
            if (c.equals("tile")) {
                return 2;
            }
            if (c.equals("material")) {
                return 3;
            }
            if (c.equals("state")) {
                return 4;
            }
        }
        if (!matchBlocks.isEmpty()) {
            return 1;
        }
        return matchTiles.isEmpty() ? 0 : 2;
    }

    /**
     * Parse the faces property into a bit mask; -1 when absent or when no
     * token resolves (all faces). Supports numeric 0-5 and face names.
     */
    private static int parseFaces(String raw) {
        if (raw == null) {
            return -1;
        }
        int mask = 0;
        for (String token : raw.split("[,\\s]+")) {
            if (token.isEmpty()) {
                continue;
            }
            int side = faceSide(token);
            if (side >= 0) {
                mask |= 1 << side;
            }
        }
        return mask == 0 ? -1 : mask;
    }

    /** Face ordinal for a faces token, or -1 when not a valid face. */
    private static int faceSide(String token) {
        if (isInteger(token)) {
            int n = Integer.parseInt(token);
            return n >= 0 && n < 6 ? n : -1;
        }
        switch (token.toLowerCase()) {
            case "down":
                return 0;
            case "up":
                return 1;
            case "north":
                return 2;
            case "south":
                return 3;
            case "west":
                return 4;
            case "east":
                return 5;
            default:
                return -1;
        }
    }

    /**
     * Expand the metadata filter into individual values; numeric ranges
     * ("0-3") are expanded, non-numeric tokens are ignored.
     */
    private static int[] parseMetadatas(List<String> metadatas) {
        List<Integer> out = new ArrayList<>();
        for (String token : metadatas) {
            int dash = token.indexOf('-');
            if (dash > 0) {
                String lo = token.substring(0, dash).trim();
                String hi = token.substring(dash + 1).trim();
                if (isInteger(lo) && isInteger(hi)) {
                    int a = Integer.parseInt(lo);
                    int b = Integer.parseInt(hi);
                    if (a <= b && b - a < 256) {
                        for (int i = a; i <= b; i++) {
                            out.add(i);
                        }
                        continue;
                    }
                }
            }
            if (isInteger(token)) {
                out.add(Integer.parseInt(token));
            }
        }
        int[] values = new int[out.size()];
        for (int i = 0; i < out.size(); i++) {
            values[i] = out.get(i);
        }
        return values;
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

    /**
     * Parse the method-specific properties (P4), with OptiFine 1.17.1
     * {@code ConnectedProperties} defaults: repeat width/height, random
     * weights (normalized to the tile count), randomLoops, symmetry, linked,
     * heights (with minHeight/maxHeight fallback) and biomes.
     */
    private static MethodProps parseMethodProps(Properties props, List<String> tiles) {
        int width = parseInt(props.getProperty("width"), -1);
        int height = parseInt(props.getProperty("height"), -1);
        int[] weights = parseIntList(props.getProperty("weights"));
        int[] sumWeights = null;
        int sumAllWeights = 1;
        if (weights != null) {
            if (weights.length > tiles.size()) {
                int[] trimmed = new int[tiles.size()];
                System.arraycopy(weights, 0, trimmed, 0, trimmed.length);
                weights = trimmed;
            } else if (weights.length < tiles.size()) {
                int[] expanded = new int[tiles.size()];
                System.arraycopy(weights, 0, expanded, 0, weights.length);
                int avg = average(weights);
                for (int i = weights.length; i < expanded.length; i++) {
                    expanded[i] = avg;
                }
                weights = expanded;
            }
            sumWeights = new int[weights.length];
            int total = 0;
            for (int i = 0; i < weights.length; i++) {
                total += weights[i];
                sumWeights[i] = total;
            }
            sumAllWeights = total > 0 ? total : 1;
        }
        int randomLoops = parseInt(props.getProperty("randomLoops"), 0);
        int symmetry = parseSymmetry(props.getProperty("symmetry"));
        boolean linked = parseBoolean(props.getProperty("linked"), false);
        int[] heights = parseHeights(props);

        String biomesRaw = props.getProperty("biomes");
        String[] biomes = null;
        boolean biomesInvert = false;
        if (biomesRaw != null) {
            String s = biomesRaw.trim();
            biomesInvert = s.startsWith("!");
            if (biomesInvert) {
                s = s.substring(1);
            }
            List<String> list = new ArrayList<>();
            for (String token : splitList(s)) {
                String name = resolveBiomeName(token);
                if (name != null) {
                    list.add(name);
                }
            }
            biomes = list.toArray(new String[list.size()]);
        }
        return new MethodProps(width, height, weights, sumWeights, sumAllWeights,
                randomLoops, symmetry, linked, heights, biomes, biomesInvert);
    }

    /** Integer property with a default (OptiFine ConnectedParser.parseInt). */
    private static int parseInt(String raw, int def) {
        if (raw == null) {
            return def;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Boolean property with a default (OptiFine ConnectedParser.parseBoolean). */
    private static boolean parseBoolean(String raw, boolean def) {
        if (raw == null) {
            return def;
        }
        String s = raw.trim().toLowerCase();
        if (s.equals("true") || s.equals("on") || s.equals("yes")) {
            return true;
        }
        if (s.equals("false") || s.equals("off") || s.equals("no")) {
            return false;
        }
        return def;
    }

    /** OptiFine parseSymmetry: opposite = 2, all = 6, unknown = 1. */
    private static int parseSymmetry(String raw) {
        if (raw == null) {
            return 1;
        }
        String s = raw.trim();
        if (s.equals("opposite")) {
            return 2;
        }
        if (s.equals("all")) {
            return 6;
        }
        return 1;
    }

    /**
     * Numeric list property ("0-3 7") into individual values; null when the
     * property is absent (OptiFine ConnectedParser.parseIntList).
     */
    private static int[] parseIntList(String raw) {
        if (raw == null) {
            return null;
        }
        List<Integer> out = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
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
                            out.add(i);
                        }
                        continue;
                    }
                }
                continue;
            }
            if (isInteger(token)) {
                out.add(Integer.parseInt(token));
            }
        }
        int[] values = new int[out.size()];
        for (int i = 0; i < out.size(); i++) {
            values[i] = out.get(i);
        }
        return values;
    }

    /**
     * Parse the heights filter: the heights list, or the minHeight/maxHeight
     * fallback (only when either differs from the OptiFine defaults).
     */
    private static int[] parseHeights(Properties props) {
        int[] heights = parseRangeListInt(props.getProperty("heights"));
        if (heights != null) {
            return heights;
        }
        int min = parseInt(props.getProperty("minHeight"), -1);
        int max = parseInt(props.getProperty("maxHeight"), 1024);
        if (min == -1 && max == 1024) {
            return null;
        }
        return new int[] {min, max};
    }

    /**
     * Range list property ("0-64 100") into flattened [lo, hi] pairs; null
     * when absent or when any token is invalid (OptiFine parseRangeListInt).
     */
    private static int[] parseRangeListInt(String raw) {
        if (raw == null) {
            return null;
        }
        List<Integer> out = new ArrayList<>();
        for (String token : raw.split("[,\\s]+")) {
            if (token.isEmpty()) {
                continue;
            }
            int[] range = parseRangeInt(token);
            if (range == null) {
                return null;
            }
            out.add(range[0]);
            out.add(range[1]);
        }
        if (out.isEmpty()) {
            return null;
        }
        int[] values = new int[out.size()];
        for (int i = 0; i < out.size(); i++) {
            values[i] = out.get(i);
        }
        return values;
    }

    /** Single range token ("lo-hi" or a plain value) into [lo, hi]; null when invalid. */
    private static int[] parseRangeInt(String token) {
        int dash = token.indexOf('-');
        if (dash > 0) {
            String lo = token.substring(0, dash).trim();
            String hi = token.substring(dash + 1).trim();
            if (isInteger(lo) && isInteger(hi)) {
                int a = Integer.parseInt(lo);
                int b = Integer.parseInt(hi);
                if (a <= b) {
                    return new int[] {a, b};
                }
            }
            return null;
        }
        if (isInteger(token)) {
            int v = Integer.parseInt(token);
            return new int[] {v, v};
        }
        return null;
    }

    /**
     * Resolve a biomes token (biome name or numeric id) to a normalized name;
     * null when the biome is not registered. The registry is accessed through
     * the public accessors (biomeList itself is not visible in 1.7.10).
     */
    private static String resolveBiomeName(String token) {
        if (isInteger(token)) {
            BiomeGenBase biome = BiomeGenBase.getBiome(Integer.parseInt(token));
            return biome == null ? null : normalizeBiomeName(biome.biomeName);
        }
        String norm = normalizeBiomeName(token);
        for (BiomeGenBase biome : BiomeGenBase.getBiomeGenArray()) {
            if (biome != null && norm.equals(normalizeBiomeName(biome.biomeName))) {
                return norm;
            }
        }
        return null;
    }

    /** Lowercase biome name without the "minecraft:" prefix. */
    private static String normalizeBiomeName(String name) {
        String s = name == null ? "" : name.trim().toLowerCase();
        if (s.startsWith("minecraft:")) {
            s = s.substring("minecraft:".length());
        }
        return s;
    }

    /** Integer average of a non-empty array (OptiFine MathUtils.getAverage). */
    private static int average(int[] values) {
        long sum = 0;
        for (int v : values) {
            sum += v;
        }
        return (int) (sum / values.length);
    }
}
