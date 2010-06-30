package platform.fullclient.layout;

import bibliothek.gui.dock.DefaultDockable;
import platform.client.navigator.AbstractNavigator;

class NavigatorDockable extends DefaultDockable {

    NavigatorDockable(AbstractNavigator navigator, String caption) {
        super(navigator,caption);

    }
}
