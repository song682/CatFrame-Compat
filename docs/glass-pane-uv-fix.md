# Glass Pane Vertical Bar — Cause, Fix and Rationale

Status: fixed on the template side, built into CatFrameCompat 0.0.4
Scope: `minecraft:glass_pane` rendered through the CatFrame VMM pipeline with RPMCP CTM active
Predecessor: `tmps/MCPatcher式CTM资源包兼容-调研与实施方案.md` (§5.6 lists two routes; this document closes out route 1)

---

## 1. Symptom

A full-height, roughly 2 px wide vertical strip of the glass texture appears on a run of glass
panes; it disappears when the pane is isolated and it is not the pane's top/bottom plate
(`glass_pane_top`, i.e. the `#edge` texture) — it is made of the **edge pixels of the `#pane`
(texture/blocks/glass.png) artwork**. The same scene rendered by a mod that uses the classic
pane renderer (`GlassPaneRenderer` / `PaneRenderHelper`) does not show it.

---

## 2. Current state (what the fix is)

CatFrameCompat now ships two templates that shadow CatFrame core's templates of the same name
inside the mod jar:

`src/main/resources/assets/minecraft/models/block/template_glass_pane_side.json`

```json
{ "from": [ 7, 0, 0 ], "to": [ 9, 16, 7 ],
  "faces": {
    "down":  { "uv": [  7, 9,  9, 16 ], "texture": "#edge", "cullface": "down" },
    "up":    { "uv": [  7, 0,  9,  7 ], "texture": "#edge", "cullface": "up" },
    "north": { "uv": [  7, 0,  9, 16 ], "texture": "#edge", "cullface": "north" },
    "west":  { "uv": [  0, 0,  7, 16 ], "texture": "#pane" },
    "east":  { "uv": [  9, 0, 16, 16 ], "texture": "#pane" } } }
```

`src/main/resources/assets/minecraft/models/block/template_glass_pane_side_alt.json`

```json
{ "from": [ 7, 0, 9 ], "to": [ 9, 16, 16 ],
  "faces": {
    "down":  { "uv": [  7, 9,  9, 16 ], "texture": "#edge", "cullface": "down" },
    "up":    { "uv": [  7, 0,  9,  7 ], "texture": "#edge", "cullface": "up" },
    "south": { "uv": [  7, 0,  9, 16 ], "texture": "#edge", "cullface": "south" },
    "west":  { "uv": [  9, 0, 16, 16 ], "texture": "#pane" },
    "east":  { "uv": [  0, 0,  7, 16 ], "texture": "#pane" } } }
```

Diff against CatFrame core (`tmps/CatFrame-0.8.3-deobf-sources/assets/minecraft/models/block/`):

| file | face | CatFrame core | CatFrameCompat |
|---|---|---|---|
| `..._side.json` | west | `[16, 0, 9, 16]` | **`[0, 0, 7, 16]`** |
| `..._side.json` | down | `[7, 0, 9, 7]` | **`[7, 9, 9, 16]`** |
| `..._side.json` | up / down | no cullface | **`cullface up` / `cullface down`** |
| `..._side_alt.json` | west | `[7, 0, 0, 16]` | **`[9, 0, 16, 16]`** |
| `..._side_alt.json` | down | `[7, 0, 9, 7]` | **`[7, 9, 9, 16]`** |
| `..._side_alt.json` | up / down | no cullface | **`cullface up` / `cullface down`** |

Provenance: the west-UV and down-UV values come from the community "glass pane fix" pack
(credit: `Made By WantedRobot`); the up/down `cullface` entries were added locally afterwards and
are cosmetic only (they are not part of the mechanism described below).

No Java code is involved in the fix.

---

## 3. Root cause

The pane's flat faces are sampled with the wrong orientation relative to the texture the CTM
engine hands us. Two conventions exist and they are mirror images of each other.

### 3.1 What CatFrame's built-in templates do

The templates (`ambientocclusion: false`, cuboid `[7,0,0]–[9,16,7]`) are the modern-vanilla
originals, which use *local / mirrored* arm UVs. With CatFrame's baking convention
(`JsonModelBake.assignUVForFace`: for `WEST`, `s = iz`, hence `u0 = uv[0]` sits at the cuboid's
min-z corner and `u1 = uv[2]` at max-z) this means, on the west-side flat of a north–south run:

* `side` north arm, z ∈ [0, 7/16] → `u` runs **16 → 9** (i.e. decreases along +z)
* centre 2 px (noside, rotated y270) → `u` runs 9 → 7
* `side_alt` south arm, z ∈ [9/16, 1] → `u` runs 7 → 0

So the tile's right edge lands on the north junction and its left edge on the next junction:
the artwork is laid down **mirrored**.

### 3.2 What the CTM icon expects: RPMCP `PaneRenderHelper`

`tmps/Right-Proper-MCPatcher-master/.../internal/modules/ctm/PaneRenderHelper.java` is the
renderer the CTM icon set was authored for. Its constants and emitters state the convention
(`PaneRenderHelper.png` is that class' own Javadoc figure):

* `PANE_BELOW_UV = 7.0`, `PANE_ABOVE_UV = 9.0` (L48-49) — the post/cross boundaries
* block edges map to `u = 0 / 16` (`minX/minZ/maxX/maxZ`)
* `renderXNeg(x, zMin, zMax, uMin, uMax, ...)` (L437-442) puts `uMin` at `zMin` and `uMax` at
  `zMax`, i.e. **`u` increases along the arm**
* the arm segments are emitted as `uMin→uBelow` (block edge → post) and `uAbove→uMax`
  (post → far block edge) (L409-435)
* sides facing the other way use a mirrored icon range via `SideIconData.update(icon, negU)`
  (L833-841)

### 3.3 Point-by-point comparison

| sample span | `PaneRenderHelper` | CatFrame core | CatFrameCompat |
|---|---|---|---|
| Z-run, north arm west face | `u` 0 → 7 (`renderXNegMinBelow`) | `[16,0,9,16]` → 16 → 9 | `[0,0,7,16]` → 0 → 7 |
| Z-run, south arm west face | `u` 9 → 16 (`renderXNegAboveMax`) | `[7,0,0,16]` → 7 → 0 | `[9,0,16,16]` → 9 → 16 |
| X-run, east arm (`side` y90, world north face) | `u` 7 → 0 (`renderZNegAboveMax`) | 9 → 16 | 7 → 0 |
| X-run, west arm (`side_alt` y90, world north face) | `u` 16 → 9 (`renderZNegMinBelow`) | 0 → 7 | 16 → 9 |
| centre 2 px (noside) | 7 → 9 (`renderXNegBelowAbove`) | 9 → 7 | still 9 → 7 (residual) |
| down face (v axis) | 7 / 9 (`TopIconData.vBelow/vAbove`) | `[7,0,9,7]` | `[7,9,9,16]` |

Every value the fix introduces equals the `PaneRenderHelper` requirement; every value CatFrame
core shipped is its mirror. That is the entire fix.

### 3.4 Why a mirrored sampling is visible at all

The icon written into `RenderContext#iconOverride` is **not** a free-standing, freely tileable
texture. It is one of 47 **composed** tiles produced by `CompactCTMDecoder`: 5 base images, each
split into 4 quadrants, give 20 quarter-tiles `a…t` (the `CompactCTM.png` figure — `ab / cd`,
`ef / gh`, `ij / kl`, `mn / op`, `qr / st`), and a 47-entry LUT assembles one 16×16 tile per
connection state.

Only 5 of the 47 LUT entries are single-source (`abcd`, `efgh`, `ijkl`, `mnop`, `qrst`); the other
42 mix quadrants from *different* base images — entry 1 is `ancp`, i.e. left half from image 0 and
right half from image 3. Therefore the `u` direction along the arm is **semantic**: sampling it
backwards draws the other half's content, including the border/edge column that belongs at the
arm's outer end, across a span whose neighbours are connected. That is the bar.

---

## 4. The multipart / rotation factor

The pane is a multipart block and its arms are rotated by the blockstate, which is why one
template-side fix is enough to cover all four orientations:

* `blockstates/glass_pane.json` applies arms as `north→side`, `south→side_alt`,
  `east→side y90`, `west→side_alt y90`; the nosides as `y0 / y90 / y270`.
* `JsonModelBake.applyYRotation` (L345-377) documents and implements "keep UVs unchanged": it
  rotates vertex positions, `faceNormal` and `cullface`, then re-winds the quad, so UV rides along
  rigidly with the geometry.
* Consequence, re-derived to confirm it: on an east–west run the east arm (`side` y90) covers
  x ∈ [9/16, 1] and the fixed UV yields `u` 7 → 0, equal to `renderZNegAboveMax`; the west arm
  (`side_alt` y90) yields `u` 16 → 9, equal to `renderZNegMinBelow`. Both orientations match, so
  the fix does not trade one axis for the other.
* Residual: the pieces that actually get rotated are the nosides (the centre 2 px). They still
  carry CatFrame's mirrored orientation, so the panel is not 100 % aligned to `PaneRenderHelper`
  — currently invisible because the composed icon's centre columns carry no artwork, but it is the
  first suspect if a seam ever appears exactly in the middle of a pane.

---

## 5. Why this approach was chosen

A simple, basic, containable change was preferred over an engine-side one:

* Java untouched — no engine-side re-normalisation of pane side-face UVs, therefore none of the
  copy-on-write hazard that comes with mutating `BakedQuad`s sourced from a shared bake cache.
* The existing `iconOverride` plumbing (`RpmcpRenderExtension`) already delivers the right icon per
  face (`NORTH→ZNeg`, `SOUTH→ZPos`, `WEST→XNeg`, `EAST→XPos`); only the template's sampling
  convention had to be brought in line with the icon's composer.
* The earlier CTM research document had already reached the same conclusion for route 1
  ("usable immediately, no development needed") and parked route 2 (engine-side linear
  re-normalisation of `q.up/q.vp`) as P3.
* Rejected alternative: patch `apply`/`CtmRenderExtension` to rescale pane side-face UVs at render
  time — more capable, more risk, and unnecessary while the built-in template covers it.

---

## 6. Verification

Controlled-variable experiments, single variable = the arm west UV.

| group | setup | result |
|---|---|---|
| A | west face reverted to CatFrame core values (`[16,0,9,16]` / `[7,0,0,16]`), cullface and down unchanged | **the bar comes back** ⇒ the west UV is the cause |
| B | all external resource packs disabled, only the built-in CatFrameCompat 0.0.4 jar | **correct** ⇒ the mod jar's `assets/minecraft/models/block` does override CatFrame core's same-named templates |

Build: `gradlew build` succeeds and the 0.0.4 jars contain both templates.

---

## 7. A correction to earlier reasoning (do not repeat)

An earlier derivation concluded that the two UV variants were "mathematically equivalent — the
tile's two edge columns just swap left/right at the junction" and therefore that the west UV could
not be the culprit. That was wrong; experiment A refutes it. The error was modelling the CTM icon
as a symmetric, freely tileable texture, which §3.4 shows it is not.

---

## 8. Open items

* `.gitignore`: the bare `minecraft` pattern is gone (`# minecraft`, then `.minecraft`, `eclipse`,
  `run`), so `src/main/resources/assets/minecraft/**` is trackable without `git add -f`. If a
  root-level run directory named `minecraft/` ever reappears, prefer the rooted pattern
  `/minecraft/` rather than restoring the unrooted one.
* Optional pixel-level dump: compose the LUT entry for the observed connection state out of the 5
  base images and print its columns, to close out "which column lands where" exactly.
* Corner/edge variants (ANCP vs MNOP) remain an acknowledged limitation (README): CatFrame's
  generic neighbour detection does not use `canPaneConnectToBlock`, so some corner states pick a
  different composite. Related to, but distinct from, the direction issue fixed here.

---

## 9. Anchors

* CatFrame core templates: `tmps/CatFrame-0.8.3-deobf-sources/assets/minecraft/models/block/template_glass_pane_{post,side,side_alt,noside,noside_alt}.json` and `.../blockstates/glass_pane.json`
* Baking: `.../decok/dfcdvadstf/catframe/model/core/baking/JsonModelBake.java` — `assignUVForFace` (WEST: `s = iz`), `applyYRotation` (L345-377), `recalculateWinding` (L516-552)
* RPMCP: `.../modules/ctm/PaneRenderHelper.java` (L45-49 constants, L92-106 mask, L138-166 icon query, L409-551 quad emitters, L824-862 icon data) and `.../modules/ctm/CompactCTMDecoder.java` (L56-104 LUT + composition)
* CatFrameCompat: `src/main/java/decok/dfcdvadstf/catframe/compact/mcpatcher/RpmcpRenderExtension.java`
