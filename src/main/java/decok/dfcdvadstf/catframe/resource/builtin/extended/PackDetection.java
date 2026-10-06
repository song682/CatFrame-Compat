package decok.dfcdvadstf.catframe.resource.builtin.extended;

import decok.dfcdvadstf.catframe.resources.builtin.BuiltinPackDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PackDetection {

    private static final Map<String, BuiltinPackDescriptor> PACKS = new LinkedHashMap<>();

    /**
     * @param id the pack id that registered
     * @return whether the descriptor is registered for {@code id}, or {@code null} when
     *         the id is unknown
     */
    public static synchronized boolean isRegistered(String id) {
        return id != null && PACKS.containsKey(id);
    }
}
