# CatFrame Compat

The mod compatibility layer for CatFrame, also provide some useful tools for mod develop.

# Compat
## 1. IME Compat

- IngameIME compat: For all of the text input box components let it input CJK
- IMEBackport: Same as IngameIME.

## 2. MCPatcher Compat

- MCPatcherForge series (Except MCPatcherForge), e.g., NotFine, Angelica, OptiFuture: Support CTM methods to let it working on the json-modelized blocks.
- MCPatcherForge (OptiFutrue <= 1.2.3): Because have 8 parameter and one useless parameter is cannot be removed, so this mod support is currently unavailable.
- Right Proper MCPatcher: Developing... Comming soon....

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
