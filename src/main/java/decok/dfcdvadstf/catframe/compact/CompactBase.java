package decok.dfcdvadstf.catframe.compact;

import cpw.mods.fml.common.Loader;

public class CompactBase {

    // IME Support
    public static boolean isIGIMEInstalled() {
        return Loader.isModLoaded("ingameime");
    }

    public static boolean isIMEBackportInstalled() {
        return Loader.isModLoaded("ime_input_backport");
    }

    // MCPatcher Format + OptiFine-like mod compact

    @Deprecated
    public static boolean isMCPatcherForgeInstalled() {
        // Sorry This mod is used to have a support, but now after a rethink,
        // I started to not provide it for such a mod that discontinued for 2 years, which contained 8 parameters, while one of it was called zero times.
        // (tl;dr, I won't provide a mod support contains one useless parameter, although it was the upbranch source.)
        return Loader.isModLoaded("mcpatcherforge");
    }

    public static boolean isAngelicaInstalled() {
        return Loader.isModLoaded("angelica");
    }

    public static boolean isNotFineInstalled() {
        return Loader.isModLoaded("notfine");
    }

    public static boolean isRightProperMCPatcherInstalled() {
        return Loader.isModLoaded("mcpatcher");
    }

    public static boolean isOptiFutureInstalled() {
        return Loader.isModLoaded("optifuture");
    }

    /**
     * Whether the installed OptiFuture build carries the Natural Textures API
     * ({@code com.prupe.mcpatcher.natural.NaturalTextures}). Probed by class
     * presence without initialization so builds that predate the natural module
     * are rejected before the bridge class is registered (and thus loaded).
     *
     * @return true when OptiFuture is installed and exposes the natural module
     */
    public static boolean isOptiFutureNaturalAvailable() {
        if (!isOptiFutureInstalled()) {
            return false;
        }
        try {
            Class.forName("com.prupe.mcpatcher.natural.NaturalTextures", false,
                    CompactBase.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            // OptiFuture build without the natural module.
            return false;
        }
    }

    // Tags support
    public static boolean isWolfTagInstalled() {
        return Loader.isModLoaded("pineapple_tag");
    }

    public static boolean isHogsTagInstalled() {
        return Loader.isModLoaded("hogutils");
    }

    // Model support
    public static boolean isItemPhysicInstalled() {
        return Loader.isModLoaded("itemphysic");
    }

    public static boolean isFloatingItemInstalled() {
        return Loader.isModLoaded("floatingitems");
    }
}
