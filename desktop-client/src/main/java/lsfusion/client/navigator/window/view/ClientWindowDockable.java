package lsfusion.client.navigator.window.view;

import bibliothek.gui.dock.common.DefaultSingleCDockable;
import lsfusion.client.navigator.window.ClientAbstractWindow;

import javax.swing.*;

public class ClientWindowDockable extends DefaultSingleCDockable {
    public ClientWindowDockable(ClientAbstractWindow window, JComponent contentComponent) {
        super(window.canonicalName, window.caption, contentComponent);

        setTitleShown(window.titleShown);
        setCloseable(true);
    }
}
