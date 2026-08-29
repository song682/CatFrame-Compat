# The CTMUtils Direct-Call Route — Revisited

> Decision record for the first idea we rejected: instead of building our own
> CTM engine, *just call MCPatcherForge's `CTMUtils` from the render extension*.
> This document reconstructs how that route would actually be wired, why it was
> turned down at P0, and whether the gap between it and the shipped solution can
> be bridged.

---

## 1. The route as originally imagined

Before the decision recorded in §1.3 of
`tmps/MCPatcher式CTM资源包兼容-调研与实施方案.md` ("build the CTM engine inside
the compatibility layer"), the obvious shortcut was reuse: MCPatcherForge
(`com.prupe.mcpatcher.ctm.CTMUtils`) already contains a complete, battle-tested
CTM implementation — pack scanning, `.properties` parsing, tile loading,
rule matching and per-method tile selection.

The idea: call `CTMUtils.getBlockIcon(...)` from `CtmRenderExtension.apply()`
and feed the returned `IIcon` into `RenderContext.iconOverride`. Everything
below the call site would be MCPF's responsibility.

What `CTMUtils` provides for free (verified against
`mcpatcherforge-1.0.0-alpha-sources`):

- **Pack scanning**: `TexturePackChangeHandler` (priority 3) rescan
  `mcpatcher/ctm/*.properties` on every pack reload, rebuild the override
  tables in `beforeChange()`, and register icons in `afterChange()`.
- **Parsing + tile loading**: `TileOverride.create(resource, tileLoader)` with
  a `TileLoader("textures/blocks")`, including blank/overlay resource handling.
- **Two lookup tables with tile-class-first iteration**:
  `blockOverrides` (`IdentityHashMap<Block, List<BlockStateMatcher>>`) and
  `tileOverrides` (`HashMap<String, List<ITileOverride>>`), walked by
  `TileOverrideIterator.IJK` (world-aware) or `.Metadata` (world-less).
- **Method dispatch**: ctm / horizontal / vertical / random / repeat / fixed …
  implemented inside the `TileOverride` classes.

## 2. How it would actually be wired

The call site in the render extension:

```java
// IJK variant: needs only world + coords + face. The RenderBlocks parameter
// is unused by the implementation and may be passed as null.
IIcon ctm = CTMUtils.getBlockIcon(baseIcon, null, block, ctx.world,
        ctx.x, ctx.y, ctx.z, ctx.quad.face.ordinal());
if (ctm != null && ctm != baseIcon) {
    ctx.iconOverride = ctm;
}
```

- `CTMUtils` self-initialises in its static block (registers the pack change
  handler), so no explicit init call is needed — but that also means we cannot
  control *when* it runs or whether it runs at all.
- The second overload `getBlockIcon(icon, renderBlocks, block, face, metadata)`
  needs no world at all, but only supports world-less methods
  (fixed/random/repeat), so the IJK variant is the one that matters.
- Dependency: `mcpatcherforge-1.0.0-alpha-dev.jar` (already in `lib/` for dev)
  plus the MCPatcherForge mod at runtime — or fat-jar the library into our mod.

## 3. Why it was rejected at the start

1. **Dependency direction**. CatFrameCompact is positioned as a bridge layer
   *above* CatFrame core; it must not depend on the MCPF family of mods.
   mcpatcherforge is a 1.0.0-alpha snapshot, effectively unmaintained, and
   depending on it at runtime would force every user to install a legacy mod —
   or force us to fat-jar the whole MCPF, which defeats the "thin layer" idea.
2. **Invocation point**. MCPF's CTM hooks live on the RenderBlocks/ISBRH
   paths. CatFrame's VMM has taken over block rendering, so the hooks never
   fire — *we* would have to become the caller (which §2 shows is technically
   possible, but it was far from obvious at P0).
3. **Static mutable state**. `lastOverride`, the reused `BlockOrientation`
   instance, the reused iterators and the shared tables are all static. There
   is no thread-safety story, which collides head-on with the extension-chain
   contract (Beddium-style parallel chunk compilation must see no shared
   mutable state).
4. **Behaviour coupling**. `RenderPassAPI` (overlay passes),
   `skipDefaultRendering` / `blankIcon`, and MCPF's own `Config` switches
   (`standard`/`nonStandard`) leak into our pipeline outside `CtmManager`'s
   control.
5. **Double bookkeeping**. We would still need our own scanner/parser for the
   cases MCPF does not cover (e.g. `optifine/ctm/` packs), producing two
   overlapping registries.

## 4. Gap analysis — can it be bridged?

| Gap | Bridgeable? | How / why not |
|---|---|---|
| Invocation point | **Yes** | The IJK overload accepts world + coords + face; `RenderBlocks` is unused by the implementation and can be `null`. |
| Dependency direction | No | Alpha snapshot, unmaintained, runtime mod required; fat-jar = importing the entire MCPF. |
| Thread safety | No | Static mutable state; synchronising or ThreadLocal-ising it *is* a rewrite of its state management. |
| Lifecycle & config control | No | MCPF owns reload order and configuration; we lose the `CtmManager` switches and debug logging. |
| Behaviour coupling | No | Overlay/blank-icon semantics would have to be stripped — after stripping, little of `CTMUtils` remains. |

**Bottom line**: only the invocation point is bridgeable. Bridging the other
four gaps amounts to rewriting CTMUtils's registry, iterators and lifecycle —
which is precisely what the shipped `CtmTileSelector` already is: a stateless,
dependency-free, configurable port of the same semantics. The original
rejection stands.

## 5. Where this leaves us

- The shipped route (P0–P3) is the "bridged" form of the CTMUtils idea: same
  semantics (tile-class-first matching, 47-grid mapping, pane skip), no
  dependencies, no shared mutable state.
- P4 (method expansion: horizontal / vertical / top / random / repeat / fixed)
  continues along the same route; the reference implementations stay RPM /
  mcpatcherforge / OptiFine sources in `tmps/` (read-only).
- If overlay / emissive / ctm_compact are ever wanted (P5), they get their own
  design document — still without reaching back to `CTMUtils`.
