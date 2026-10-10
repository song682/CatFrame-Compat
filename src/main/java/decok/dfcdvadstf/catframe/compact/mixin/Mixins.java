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
            .setApplyIf(() -> CompatConfig.chromaticTooltipsCompat & CompactBase.isChromaticTooltipsInstalled())
            .setPhase(Phase.LATE)
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
