package lsfusion.gwt.client.navigator;

import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

import java.io.Serializable;

public abstract class GPropertyNavigator implements Serializable {

    public GPropertyNavigator() {
    }

    public abstract void update(GNavigatorController navigatorController, WindowsController windowsController, PValue value);
}