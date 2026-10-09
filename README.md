# CatFrame Compat

The mod compatibility layer for [CatFrame](https://github.com/song682/CatFrame), also provide some useful tools for mod develop.

# Compat
## 1. IME Compat

- IngameIME compat: For all the text input box components let it input CJK
- IMEBackport: Same as IngameIME.

## 2. MCPatcher Compat

- MCPatcherForge series (Except MCPatcherForge), e.g., NotFine, Angelica, OptiFuture: Support CTM methods to let it working on the json-modelized blocks.
- MCPatcherForge (OptiFutrue <= 1.2.3): Because have 8 parameter and one useless parameter is cannot be removed, so this mod support is currently unavailable.
- OptiFuture: Support the Natural textures.
- Right Proper MCPatcher: Support CTM methods to let it working on the json-modelized blocks, and natural textures. 

> [!IMPORTANT]
> Known Limitations       
> **Glass Pane CTM Corner Connection (ANCP vs MNOP)**: When using CTM on glass panes, the corner connections may display incorrect variants (e.g., ANCP instead of MNOP). This is an architectural limitation: CatFrame's VMM rendering bypasses the specialized pane rendering logic (PaneRenderHelper/GlassPaneRenderer) that handles thin-pane connectivity via `canPaneConnectToBlock`. The CTM engine uses generic neighbor detection which doesn't account for pane thin-plate geometry. Basic pane CTM connections work correctly; only corner/edge variants may be inaccurate.        
> Fixed via the built-in texture packs.

## 3. Tag Compat

- PineappleTags: For the tags is can be registered and used by CatFrame. 
- HogUtils (HogTags): Developing...

## 4. Model for physics

- Item Physic:    
  (Official version) is not available and potentially causing a short in my item transformation, and cannot avoid because the asm short is fully short which means hard to retrieve, so this version will cause a crash.    
  (Unofficial version) is tested fine and the rotation and spin will be fully applied by Mixin. 
- FloatingItems: Developing...

## 5. Offhand Compat
- Backhand (GTNH edition 1.8.x): Let the CatFrame hand things render correctly in the offhand/left hand (For right-handed person, but for the left-handed person is right hand). 
  Backhand replays the whole hand pass under a mirrored GL state, under which the vanilla/Forge anchor chain auto-conjugates into the left-hand anchor; the bridge — a pure `IModelRenderExtension` on CatFrame's public extension chain — then replaces the builtin display transform with the modern (26.1.2) left-hand matrix for the offhand passes only: resolve the authored `firstperson_lefthand` / `thirdperson_lefthand` entry, falling back to the corresponding right-hand entry verbatim (`ItemTransforms.Deserializer` semantics), then negate `translation.x / rotation.y / rotation.z` (`ItemTransform.apply` left-hand fix). 
  Animations, arm anchor and the preTransform cancellation all stay Backhand's and CatFrame's. The item keeps Backhand's mirrored chirality; the lateral position deviates from exact-modern by `2×translation.x` (invisible for vanilla-convention models) because Backhand's outer mirror is forced GL state.    
  
> [!IMPORTANT]
> Known upstream limitations (reported, fixed on the CatFrame core side; this bridge does not patch them):    
> **Third-person offhand block items**: CatFrame's `RenderJsonItemModel.resolveHeadSlotKind` keys head-slot ownership off `stack != entity.getHeldItem()`, and Backhand's third-person pass does no slot swap, so an `ItemBlock` held in the offhand is misclassified as a head-slot render (`ITEM_HEAD` phase + head preTransform) and renders misplaced until the CatFrame core learns to distinguish the offhand.    
> **Builtin generated model data**: CatFrame's builtin `generated` carries a placeholder `firstperson_lefthand` with rotation `{0, 0, 0}`; modern `generated.json` authors **no** left-hand entry at all (the verbatim right-hand fallback above is the mechanism), so the placeholder should be removed upstream — until then the bridge honors it as authored data and generated-style items face wrong in the offhand first person.

## 6. Tooltip Compat
- Chromatic Tooltips: Allow Chromatic tooltips rendering on the modern screen.

# Tools
## Model

- ModelBound: Derives block bounds from CatFrame JSON block models.
- BlockState Transformation: Transformation for Block State From Modern NeoForge.

## UI

- Theme systems for modern UI developing. 
- Animation for modern UI developing. (Just a few functions with a lot of limitation.)
- HTML Browser Like UI Creating. (Developing...)

## Localization

- This module is a widened tool that let JSON language that in any package loading.

# Dependency

[CatFrame](https://github.com/song682/CatFrame/releases/latest) (Over 0.9.0), JarUtils (over 0.0.2).

# License

**Source Code**: [MIT License](LICENSE)      
**Binary Jars**: [Redistribution License](LICENSE-OF-MC_MOD-REDISTRIBUTION)     