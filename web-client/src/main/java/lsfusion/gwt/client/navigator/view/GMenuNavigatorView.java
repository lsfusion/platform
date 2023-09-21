package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GMenuNavigatorWindow;

import java.util.LinkedHashSet;

public class GMenuNavigatorView extends GNavigatorView<GMenuNavigatorWindow> {
    public GMenuNavigatorView(GMenuNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, new Label(window.caption), navigatorController);
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> newElements) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getHeight() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getWidth() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
