# Feature Request: Expose State Properties in RenderContext for CTM and CIT

## Problem

CatFrame's VMM rendering pipeline computes rich state information (blockstate properties for blocks, item properties for items) but does not expose it to extension layers. This creates two issues:

### Issue 1: Pane CTM Corner Connection (ANCP vs MNOP)
When using CatFrameCompat's MCPatcher CTM bridge on glass panes, corner connections display incorrect CTM variants (e.g., ANCP instead of MNOP). CatFrame already computes correct connection states (north/east/south/west) via `VanillaBlockResolvers.PANE`, but extension layers cannot access this information.

### Issue 2: Custom Item Textures (CIT) Support
CatFrame implements item property system (`ItemProperties`, `ItemPropertyProvider`) for items/ JSON decision tree rendering. Extension layers like CatFrameCompact cannot access these pre-computed item properties (damage, using_item, use_duration, display_context) to implement CIT (Custom Item Textures) compatibility.

## Root Cause

`RenderContext` currently exposes:
- `world` (IBlockAccess), `x/y/z` (coordinates), `block` (Block), `metadata` (int)
- `stack` (ItemStack), `phase` (RenderPhase)
- `quad` (BakedQuad) with face and icon information

It does **not** expose:
- **Blockstate properties** (north/east/south/west for panes, shape for stairs, etc.)
- **Item properties** (damage, using_item, use_duration, display_context, etc.)

Extension layers like `RpmcpRenderExtension` and `CtmRenderExtension` have no way to access the pre-computed state information that CatFrame already calculates.

## Proposed Solution

Add two fields to `RenderContext` that expose state properties computed by CatFrame:

```java
public final class RenderContext {
    // ... existing fields ...

    /**
     * Blockstate properties computed by CatFrame's dynamic property resolvers.
     * Available during BLOCK_WORLD phase.
     *
     * For panes: contains "north", "east", "south", "west" with values "true"/"false".
     * For stairs: contains "facing", "half", "shape".
     * For redstone wire: contains "north", "east", "south", "west", "power", etc.
     *
     * May be null if no dynamic properties were computed for this block.
     */
    @Nullable
    public final Map<String, String> blockstateProps;

    /**
     * Item properties computed by CatFrame's item property system.
     * Available during ITEM_* phases (ITEM_ENTITY, ITEM_HAND_FIRST_PERSON, etc.).
     *
     * Contains: "damage", "max_damage", "using_item", "use_duration", "display_context", etc.
     * Values are Comparable<?> (Integer, Boolean, String, Enum).
     *
     * May be null if no item properties were computed.
     */
    @Nullable
    public final Map<String, Comparable<?>> itemProps;

    // ... constructor ...
}
```

## Benefits

1. **Extension layers can access pre-computed state information** without re-computing
2. **Enables accurate pane CTM corner connections** by using `BlockPane.canPaneConnectToBlock` results
3. **Enables CIT (Custom Item Textures) support** by exposing item properties (damage, using_item, etc.)
4. **General purpose**: useful for any extension that needs state information (stairs shape, redstone power, item display context, etc.)
5. **Minimal change**: only exposes data, doesn't change rendering logic
6. **Preserves high-version features**: blockstate and item property systems remain intact

## Example Usage: Pane CTM

In CatFrameCompact's `RpmcpRenderExtension`:

```java
if (ctx.block instanceof BlockPane && ctx.blockstateProps != null) {
    // Read pre-computed connection states
    boolean north = "true".equals(ctx.blockstateProps.get("north"));
    boolean east = "true".equals(ctx.blockstateProps.get("east"));
    boolean south = "true".equals(ctx.blockstateProps.get("south"));
    boolean west = "true".equals(ctx.blockstateProps.get("west"));

    // Compute CTM index using these connection states
    int ctmIndex = computePaneCTMIndex(north, east, south, west, ctx.quad.face);

    // Get icon from tileIcons[ctmIndex]
}
```

## Example Usage: CIT (Custom Item Textures)

In a future CIT extension:

```java
if (ctx.phase.isItemPhase() && ctx.itemProps != null) {
    // Read item properties
    int damage = (Integer) ctx.itemProps.getOrDefault("damage", 0);
    boolean usingItem = (Boolean) ctx.itemProps.getOrDefault("using_item", false);
    int useDuration = (Integer) ctx.itemProps.getOrDefault("use_duration", 0);

    // Match against CIT properties file
    CITProperties citProps = CITMatcher.match(ctx.stack, damage, usingItem, useDuration);
    if (citProps != null) {
        ctx.iconOverride = citProps.getIcon();
    }
}
```

## Alternative Approaches Considered

1. **Re-compute state in extension layer**: Duplicates work already done by CatFrame, and may have side effects (world access, player state queries)
2. **Add CTM/CIT-specific APIs to CatFrame**: Too invasive, requires deep integration with MCPatcher/CIT mods
3. **Implement full pane/item renderer in CatFrame**: Exceeds scope of compatibility layer

## Priority

Medium-High: This enables two important features:
- **Pane CTM corner connections**: Affects visual quality for users with CTM resource packs
- **CIT support**: Enables custom item textures (damage-based, use-based, display-context-based)

Both are common features in resource packs and mod packs.
