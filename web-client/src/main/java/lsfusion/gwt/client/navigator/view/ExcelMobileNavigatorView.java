package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;

public class ExcelMobileNavigatorView extends MobileNavigatorView {

    public ExcelMobileNavigatorView(GNavigatorElement root, GINavigatorController navigatorController) {
        super(root, navigatorController);
    }

    protected ComplexPanel initRootPanel() {
        ResizableComplexPanel navElement = new ResizableComplexPanel(Document.get().createElement("nav"));
        navElement.getElement().setId("lsfNavigatorMenu");
        return navElement;
    }

    protected ComplexPanel initSubMenuPanel() {
        return new ResizableComplexPanel(Document.get().createULElement());
    }

    @Override
    protected void initSubRootPanel(ComplexPanel rootPanel) {
    }

    @Override
    protected void initMenuItem(int level, ImageButton button) {
    }

    @Override
    protected void initSubMenuItem(ImageButton button, ComplexPanel subMenuPanel) {
    }

    protected ComplexPanel wrapNavigatorItem(ComplexPanel menuULElement) {
        // wrapping into li;
        ResizableComplexPanel liElement = new ResizableComplexPanel(Document.get().createLIElement());
        menuULElement.add(liElement);
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
