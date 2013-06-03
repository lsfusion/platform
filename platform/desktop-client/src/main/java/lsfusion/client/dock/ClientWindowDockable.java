package lsfusion.client.dock;

import bibliothek.gui.dock.common.DefaultSingleCDockable;
import lsfusion.client.navigator.ClientAbstractWindow;

import javax.swing.*;

public class ClientWindowDockable extends DefaultSingleCDockable {
    public ClientWindowDockable(ClientAbstractWindow window, JComponent contentComponent) {
        super(window.getSID(), window.caption, contentComponent);

        setTitleShown(window.titleShown);
        setCloseable(true);
    }
}
