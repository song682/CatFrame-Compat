package decok.dfcdvadstf.catframe.ui.extended.animation;

public interface AnimationInterface {
    float getStart(AnimationInterface animation);

    float getEnd(AnimationInterface animation);

    float getDuration(AnimationInterface animation);

    void startAnimation(AnimationInterface animation);

    void endAnimation(AnimationInterface animation);
}
