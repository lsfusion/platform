package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

import java.util.ArrayList;
import java.util.function.Predicate;

public class ExcelMobileNavigatorView extends MobileNavigatorView {

    public ExcelMobileNavigatorView(ArrayList<GNavigatorWindow> navigatorWindows, WindowsController windowsController, GINavigatorController navigatorController) {
        super(navigatorWindows, windowsController, navigatorController);
    }

    protected RootPanels initRootPanels() {
        ResizableComplexPanel navElement = new ResizableComplexPanel(Document.get().createElement("nav"));
        navElement.getElement().setId("lsfNavigatorMenu");

        ComplexPanel navPanel = new ResizableComplexPanel(Document.get().createULElement());
        navElement.add(navPanel);

        return new RootPanels(navElement, new Predicate[] {ANY}, new ComplexPanel[] { navPanel }, new GAbstractWindow[] { null });
    }

    protected ComplexPanel initFolderPanel(NavigatorImageButton button) {
        return new ResizableComplexPanel(Document.get().createULElement());
    }

    @Override
    protected ComplexPanel wrapMenuItem(ComplexPanel panel, int level, ImageButton button) {
        // wrapping into li;
        ResizableComplexPanel liElement = new ResizableComplexPanel(Document.get().createLIElement());
        panel.add(liElement);
        return liElement;
    }

    protected void enable(ComplexPanel navElement) {
        enableMMenu(navElement.getElement(), ClientMessages.Instance.get().navigator());
    }

    public final native void enableMMenu(Element element, String title) /*-{
        menu = new $wnd.MmenuLight(element);
        navigator = menu.navigation({
            title: title
        });
        drawer = menu.offcanvas();
    }-*/;

    @Override
    public native void openNavigatorMenu() /*-{
        drawer.open();
    }-*/;

    @Override
    public native void closeNavigatorMenu() /*-{
        drawer.close();
    }-*/;
}
