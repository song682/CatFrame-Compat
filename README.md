# CatFrame Compat

The mod compatibility layer for CatFrame, also provide some useful tools for mod develop.

# Compat
## 1. IME Compat

- IngameIME compat: For all of the text input box components let it input CJK
- IMEBackport: Same as IngameIME.

## 2. MCPatcher Compat

- MCPatcherForge series (Except MCPatcherForge), e.g., NotFine, Angelica, OptiFuture: Support CTM methods to let it working on the json-modelized blocks.
- MCPatcherForge (OptiFutrue <= 1.2.3): Because have 8 parameter and one useless parameter is cannot be removed, so this mod support is currently unavailable.
- Right Proper MCPatcher: Support CTM methods to let it working on the json-modelized blocks.

### Known Limitations

- **Glass Pane CTM Corner Connection (ANCP vs MNOP)**: When using CTM on glass panes, the corner connections may display incorrect variants (e.g., ANCP instead of MNOP). This is an architectural limitation: CatFrame's VMM rendering bypasses the specialized pane rendering logic (PaneRenderHelper/GlassPaneRenderer) that handles thin-pane connectivity via `canPaneConnectToBlock`. The CTM engine uses generic neighbor detection which doesn't account for pane thin-plate geometry. Basic pane CTM connections work correctly; only corner/edge variants may be inaccurate.
## 3. Tag compat:

- PineappleTags: For the tags is can be registered and used by CatFrame
- HogUtils (HogTags): Developing...

## 4. Model for physics

- Item Physic:    
  (Official version) is not available and protentially causing a short in my item transformation, and cannot avoid because the asm short is fully short which means hard to retrieve, so this version will cause a crash.    
  (Unofficial version) is fully tested and the rotation and spin will be fully applied by Mixin. 
- FloatingItems: Developing...

# Tools
## Model

- ModelBound: Derives block bounds from CatFrame JSON block models.

# Dependency

CatFrame (Over 0.6.7), JarUtils (over 0.0.2).
