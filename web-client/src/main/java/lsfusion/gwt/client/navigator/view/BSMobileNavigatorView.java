package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;

public class BSMobileNavigatorView extends MobileNavigatorView {
    public static final String OFFCANVAS_ID = "mobileMenuOffcanvas";

    public BSMobileNavigatorView(GNavigatorElement root, GINavigatorController navigatorController) {
        super(root, navigatorController);
    }

    protected FlexPanel initRootPanel() {
        FlexPanel navBarPanel = new FlexPanel(true);
        navBarPanel.addStyleName("offcanvas offcanvas-start navbar p-0");

        Element navBarPanelElement = navBarPanel.getElement();
        navBarPanelElement.setId(OFFCANVAS_ID);
        navBarPanelElement.getStyle().setOverflowY(Style.Overflow.AUTO);
        navBarPanelElement.getStyle().setOverflowX(Style.Overflow.HIDDEN);
        return navBarPanel;
    }

    protected ComplexPanel initSubMenuPanel() {
        return new FlexPanel(true);
    }

    protected void initSubRootPanel(ComplexPanel navPanel) {
        navPanel.addStyleName("navbar-nav navbar-nav-vert");
    }

    protected void initSubMenuItem(ImageButton button, ComplexPanel subMenuPanel) {
        subMenuPanel.setVisible(false);
        button.addStyleName("nav-folder collapsed");
        button.addClickHandler(event -> {
            boolean wasVisible = subMenuPanel.isVisible();
            subMenuPanel.setVisible(!wasVisible);
            button.setStyleName("collapsed", wasVisible);
        });
    }

    protected ComplexPanel wrapNavigatorItem(ComplexPanel panel) {
        return panel;
    }

    protected void initMenuItem(int level, ImageButton button) {
        button.addStyleName("nav-item nav-link navbar-text nav-link-horz");
        button.addStyleName("nav-link-horz-" + level);
    }

    protected void enable(ComplexPanel navBarPanel) {
        init(navBarPanel.getElement());
    }

    public native void init(Element offcanvas_el) /*-{
        offcanvas = new $wnd.bootstrap.Offcanvas(offcanvas_el)
    }-*/;
    
    @Override
    public native void openNavigatorMenu() /*-{
        offcanvas.show();
    }-*/;

    @Override
    public native void closeNavigatorMenu() /*-{
        offcanvas.hide();
    }-*/;
}