# Feature Request: Expose Blockstate Properties in RenderContext for Pane CTM Corner Connection

## Problem

When using CatFrameCompat's MCPatcher CTM bridge on glass panes, the corner connections display incorrect CTM variants (e.g., ANCP instead of MNOP). This is an architectural limitation:

- CatFrame's VMM rendering bypasses the specialized pane rendering logic (PaneRenderHelper/GlassPaneRenderer) that handles thin-pane connectivity via `BlockPane.canPaneConnectToBlock`
- The CTM engine (CTMEngine.getCTMIconMultiPass) uses generic neighbor detection which doesn't account for pane thin-plate geometry
- CatFrame already computes correct connection states (north/east/south/west) via `VanillaBlockResolvers.PANE` and stores them in blockstate properties, but extension layers cannot access this information

## Root Cause

`RenderContext` currently exposes:
- `world` (IBlockAccess), `x/y/z` (coordinates), `block` (Block), `metadata` (int)
- `quad` (BakedQuad) with face and icon information

It does **not** expose:
- Blockstate properties (north/east/south/west for panes, shape for stairs, etc.)

Extension layers like CatFrameCompact's `RpmcpRenderExtension` and `CtmRenderExtension` have no way to access the pre-computed connection states that CatFrame already calculates.

## Proposed Solution

Add a `blockstateProps` field to `RenderContext` that exposes the blockstate properties computed by CatFrame's dynamic property resolvers:

```java
public final class RenderContext {
    // ... existing fields ...

    /**
     * Blockstate properties computed by CatFrame's dynamic property resolvers
     * (e.g., VanillaBlockResolvers.PANE for glass panes).
     * 
     * For panes: contains "north", "east", "south", "west" with values "true"/"false".
     * For stairs: contains "facing", "half", "shape".
     * For redstone wire: contains "north", "east", "south", "west", "power", etc.
     * 
     * May be null if no dynamic properties were computed for this block.
     */
    @Nullable
    public final Map<String, String> blockstateProps;
    
    // ... constructor ...
}
```

## Benefits

1. **Extension layers can access pre-computed connection states** without re-computing them
2. **Enables accurate pane CTM corner connections** by using `BlockPane.canPaneConnectToBlock` results
3. **General purpose**: useful for any extension that needs blockstate information (stairs shape, redstone wire power, etc.)
4. **Minimal change**: only exposes data, doesn't change rendering logic
5. **Preserves high-version features**: blockstate properties remain intact

## Example Usage

In CatFrameCompact's `RpmcpRenderExtension`:

```java
if (ctx.block instanceof BlockPane && ctx.blockstateProps != null) {
    // Read pre-computed connection states
    boolean north = "true".equals(ctx.blockstateProps.get("north"));
    boolean east = "true".equals(ctx.blockstateProps.get("east"));
    boolean south = "true".equals(ctx.blockstateProps.get("south"));
    boolean west = "true".equals(ctx.blockstateProps.get("west"));
    
    // Compute CTM index using these connection states
    // (replicating the 47-tile index calculation logic)
    int ctmIndex = computePaneCTMIndex(north, east, south, west, ctx.quad.face);
    
    // Get icon from tileIcons[ctmIndex]
    // ...
}
```

## Alternative Approaches Considered

1. **Re-compute connection states in extension layer**: Duplicates work already done by CatFrame, and requires accessing `BlockPane.canPaneConnectToBlock` which may have side effects
2. **Add pane-specific CTM API to CatFrame**: Too invasive, requires deep integration with CTM mods
3. **Implement full pane renderer in CatFrame**: Exceeds scope of compatibility layer

## Priority

Medium-High: This is a known limitation that affects visual quality for users with CTM resource packs. Basic pane CTM connections work correctly; only corner/edge variants are inaccurate.
