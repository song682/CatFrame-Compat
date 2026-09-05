package decok.dfcdvadstf.catframe.ui.extended.notifications;

import decok.dfcdvadstf.catframe.ui.components.AbstractComponent;
import decok.dfcdvadstf.catframe.ui.navigation.ScreenRectangle;
import decok.dfcdvadstf.catframe.ui.overlay.Overlay;
import decok.dfcdvadstf.catframe.ui.overlay.ScreenAnchor;

public class AbstractNotification extends AbstractComponent implements Notification, Overlay {
    @Override
    public ScreenAnchor getAnchor() {
        return null;
    }

    @Override
    public int getOffsetX() {
        return 0;
    }

    @Override
    public int getOffsetY() {
        return 0;
    }

    @Override
    public ScreenRectangle setScreenRectangle() {
        return null;
    }

    @Override
    public void getScreenRectangle(ScreenRectangle screenRectangle) {

    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public void setTitle(String title) {

    }

    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public void setMessage(String message) {

    }

    @Override
    public String getID() {
        return "";
    }

    @Override
    public void setID(String id) {

    }

    @Override
    public int getIconColor() {
        return 0;
    }

    @Override
    public void setIconColor(int iconColor) {

    }

    @Override
    public int getBackgroundColor() {
        return 0;
    }

    @Override
    public void setBackgroundColor(int backgroundColor) {

    }
}
