# CatFrame Compat

The mod compatibility layer for [CatFrame](https://github.com/song682/CatFrame), also provide some useful tools for mod develop.

# Compat
## 1. IME Compat

- IngameIME compat: For all the text input box components let it input CJK
- IMEBackport: Same as IngameIME.

## 2. MCPatcher Compat

- MCPatcherForge series (Except MCPatcherForge), e.g., NotFine, Angelica, OptiFuture: Support CTM methods to let it working on the json-modelized blocks.
- MCPatcherForge (OptiFutrue <= 1.2.3): Because have 8 parameter and one useless parameter is cannot be removed, so this mod support is currently unavailable.
- Right Proper MCPatcher: Support CTM methods to let it working on the json-modelized blocks.

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
- Backhand: Let the catframe hand things render correctly in the offhand/left hand (For right-handed person, but for the left-handed person is right hand. Developing...)

# Tools
## Model

- ModelBound: Derives block bounds from CatFrame JSON block models.
- BlockState Transformation: Transformation for Block State From Modern NeoForge.

## UI

- Theme systems for modern UI developing. 
- Animation for modern UI developing. (Just a few functions with a lot of limitation.)
- HTML Browser Like UI Creating. (Developing...)

# Dependency

[CatFrame](https://github.com/song682/CatFrame/releases/latest) (Over 0.9.0), JarUtils (over 0.0.2).

# License

**Source Code**: [MIT License](LICENSE)      
**Binary Jars**: [Redistribution License](LICENSE-OF-MC_MOD-REDISTRIBUTION)     