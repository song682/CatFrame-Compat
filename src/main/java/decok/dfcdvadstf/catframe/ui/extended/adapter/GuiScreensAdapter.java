package decok.dfcdvadstf.catframe.ui.extended.adapter;

import decok.dfcdvadstf.catframe.ui.Text;
import decok.dfcdvadstf.catframe.ui.screens.Screen;

public class GuiScreensAdapter extends Screen {

    protected GuiScreensAdapter(Text title) {
        super(title);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseScrolled(int delta) {
        super.mouseScrolled(delta);
    }
}
