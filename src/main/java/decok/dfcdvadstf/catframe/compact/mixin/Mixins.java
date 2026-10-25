package decok.dfcdvadstf.catframe.compact.mixin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;

public enum Mixins implements IMixins {

    BLOCK_STATE_VALIDATOR(
        new MixinBuilder("BlockState rotation breaker")
            .addClientMixins(
                "MixinBlockstateKeyValidator"
            )
            .setApplyIf(() -> CompatConfig.blockStateRotationFreely)
            .setPhase(Phase.LATE)
    ),

    ITEM_PHYSIC_COMPAT(
        new MixinBuilder("Item physics compatibility")
            .addClientMixins(
                "MixinItemPhysics"
            )
            .setApplyIf(CompactBase::isItemPhysicInstalled)
            .setPhase(Phase.LATE)
    ),

    CHROMATIC_TOOLTIPS_COMPAT(
        new MixinBuilder("Chromatic Tooltips rendering compatibility")
            .addClientMixins(
                "MixinGuiGraphicsExtractor",
                "MixinAbstractContainerScreen"
            )
            .setApplyIf(() -> CompatConfig.chromaticTooltipsCompat && CompactBase.isChromaticTooltipsInstalled())
            .setPhase(Phase.LATE)
    ),

    NEI_TOOLTIPS_COMPAT(
        new MixinBuilder("NEI tooltip coexistence")
            .addClientMixins(
                "MixinAbstractContainerScreenNei"
            )
            .setApplyIf(() -> CompatConfig.neiTooltipsCompat && CompactBase.isNEIInstalled())
            .setPhase(Phase.LATE)
    ),

    // Early phase: the target is vanilla GuiScreen, loaded long before late mixins
    // are queued. No mod/config gating is possible here (neither the mod list nor
    // the config exists yet), so the mixin always applies and checks the switch and
    // the NEI / Chromatic Tooltips precedence itself, per tooltip call.
    VANILLA_TOOLTIPS_COMPAT(
        new MixinBuilder("CatFrame tooltip rendering on vanilla-style screens")
            .addClientMixins(
                "MixinGuiScreenTooltip"
            )
            .setPhase(Phase.EARLY)
    );

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
