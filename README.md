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

## 6. Tooltip Compat
- Chromatic Tooltips: Allow Chromatic tooltips rendering on the modern screen.
- NotEnoughItems (Unofficial, GTNH edition): Keeps NEI's single tooltip frame while CatFrame's structured tooltip components (e.g. the bundle grid) are rendered inside it, instead of the two mods stacking two frames.

## Known Limitations
### CTM Part
 **Glass Pane CTM Corner Connection**: When using CTM on glass panes, the corner connections may display incorrect variants.
This is a limitation: CatFrame's VMM rendering bypasses the specialized pane rendering logic (PaneRenderHelper/GlassPaneRenderer) that handles thin-pane connectivity.
The CTM engine uses generic neighbor detection which doesn't account for pane thin-plate geometry. Basic pane CTM connections work correctly; only corner/edge variants may be inaccurate.        
Fixed via the built-in texture packs.
## Backhand
**Third-person offhand block items**: CatFrame's `RenderJsonItemModel.resolveHeadSlotKind` keys head-slot ownership off `stack != entity.getHeldItem()`, and Backhand's third-person pass does no slot swap, so an `ItemBlock` held in the offhand is misclassified as a head-slot render (`ITEM_HEAD` phase + head preTransform) and renders misplaced until the CatFrame core learns to distinguish the offhand.    

## Better Foliage ([Issue #6](https://github.com/song682/CatFrame-Compat/issues/6))

**Not planned.** Better Foliage - Legacy Edition installs its render override inside the body of `RenderBlocks.renderBlockByRenderType`, while CatFrame intercepts that method at its entry for every block it hosts — so on those blocks (vanilla leaves, logs, grass, sand, etc.) Better Foliage's features (Extra Leaves, Round Logs, etc.) never execute.
Both mods want to draw the same blocks, and each block's appearance can only come from one pipeline — making Better Foliage render these blocks would mean yielding them entirely to it, at the cost of CatFrame's rendering on them (CTM, Natural textures, render extensions).
That trade-off is declined, so this compatibility is not planned; Better Foliage keeps working on blocks CatFrame does not host (e.g., modded leaves).

## NotEnoughItems (Tooltip Part)

**Why a bridge is needed.** NEI rewrites the `renderToolTip` call site inside `GuiContainer.drawScreen` with an ASM `MethodReplacer`, so CatFrame's own `renderToolTip` override never runs - while CatFrame's `extractTooltip` still queues its deferred nine-slice tooltip. The result is two frames with the same text.           
Text lines need no work at all: NEI resolves them through `ItemStack.getTooltip`, so everything contributed via `ItemTooltipEvent` (including this mod's tag tooltips) is already shared. Only the frame and the structured components are affected.    
**What the bridge does.** At post-init it registers ahead-of-the-item-tooltip hook with NEI (`GuiContainerManager.addTooltipHandler`). When the hovered stack has a CatFrame tooltip component (`ItemTooltipImages`, e.g. the bundle grid), the component is wrapped into a CodeChickenLib line handler (`GuiDraw.TOOLTIP_HANDLER` + `GuiDraw.getTipLineId`) and inserted at index 1, i.e. right under the item name - the same slot CatFrame uses. The frame is then flagged as NEI-owned and a late mixin cancels CatFrame's `extractTooltip` for that frame, so exactly one frame is painted. 
The bridge is not screen-scoped: components are also injected in *other* mods' container GUIs, where NEI is already the tooltip renderer. Only the cancellation is screen-scoped (CatFrame screens are the only ones that would paint a second frame).

**When Chromatic Tooltips is installed** it wins: CT replaces the tooltip wholesale and draws it on `DrawScreenEvent.Post`, after NEI's frame, so the two cannot be merged. The bridge then suppresses NEI's container tooltip on CatFrame screens (`IContainerObjectHandler.shouldShowTooltip`) instead of translating components into it, and CT renders everything.

> **CatFrame's tooltip style and positioner are not applied.** NEI owns placement (`mousex + 12`, `mousey - 12`)
> and paging in this setup; CatFrame's `TOOLTIP_STYLE` and `ClientTooltipPositioner` only take effect without NEI.
>
> **Components with `showTooltipWithItemInHand() == true` do not show while an item is carried.** NEI's replacement
> keeps the vanilla `carried == null` guard, so the item tooltip branch never runs while something is held on the
> cursor. No current CatFrame component opts into that behaviour, so this is a forward-looking limitation.
>
> **Component height participates in pagination.** CodeChickenLib pages the tooltip using
> `handler.getSize().height` and the tooltip display area height, so a component taller than
> `displaySize().height - 26` is given a page of its own and may overflow it. Components are expected to stay in
> the range of a few item rows.
>
> **One rendering owner per screen.** With NEI present, NEI's frame is extended, not replaced: CatFrame cannot
> re-add its own background/padding. With CT present, NEI's frame is suppressed instead of merged, so NEI-specific
> parts of the item tooltip (its own handlers, count details) do not appear on CatFrame screens in that setup.
> **Handlers are per-frame.** CodeChickenLib clears its line-handler table at the end of `drawMultilineTip`, so the
> bridge builds a fresh handler and re-reads the line id on every frame; the marker cannot be cached across frames.

> PRs that break these limitation are welcomed.

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