package decok.dfcdvadstf.catframe.compact.mixin;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;
import decok.dfcdvadstf.catframe.CompatConfig;
import decok.dfcdvadstf.catframe.compact.CompactBase;

public enum Mixins implements IMixins {

    BLOCK_STATE_VALIDATOR(
        new MixinBuilder("BlockState rotation breaker")
            .addClientMixins(
                "late.MixinBlockstateKeyValidator"
            )
            .setApplyIf(() -> CompatConfig.blockStateRotationFreely)
            .setPhase(Phase.LATE)
    ),

    ITEM_PHYSIC_COMPAT(
        new MixinBuilder("Item physics compatibility")
            .addClientMixins(
                "late.MixinItemPhysics"
            )
            .setApplyIf(CompactBase::isItemPhysicInstalled)
            .setPhase(Phase.LATE)
    );

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Override
    public MixinBuilder getBuilder () {
        return builder;
    }
}
