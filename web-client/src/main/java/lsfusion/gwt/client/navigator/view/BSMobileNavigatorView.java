package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;

public class BSMobileNavigatorView implements MobileNavigatorView {
    public static final String OFFCANVAS_ID = "mobileMenuOffcanvas";
    private final GINavigatorController navigatorController;

    public BSMobileNavigatorView(GNavigatorElement root, GINavigatorController navigatorController) {

        this.navigatorController = navigatorController;

        FlexPanel navBarPanel = new FlexPanel(true);
        navBarPanel.addStyleName("offcanvas offcanvas-start navbar p-0");
        Element navBarPanelElement = navBarPanel.getElement();
        
        navBarPanelElement.setId(OFFCANVAS_ID);
        navBarPanelElement.getStyle().setOverflowY(Style.Overflow.AUTO);
        navBarPanelElement.getStyle().setOverflowX(Style.Overflow.HIDDEN);

        FlexPanel navPanel = new FlexPanel(true);
        navPanel.addStyleName("navbar-nav navbar-nav-vert");
        for (GNavigatorElement child : root.children) {
            createMenuItem(navPanel, child, 0);
        }
        navBarPanel.add(navPanel);
        
        RootLayoutPanel.get().getElement().appendChild(navBarPanelElement);
        
        init(navBarPanelElement);
    }

    private void createMenuItem(FlexPanel panel, GNavigatorElement navigatorElement, int level) {
        ImageButton button = new NavigatorImageButton(navigatorElement, false);
        button.addStyleName("nav-item nav-link navbar-text nav-link-horz");
        button.addStyleName("nav-link-horz-" + level);

        FlexPanel subMenuPanel = new FlexPanel(true);
        subMenuPanel.setVisible(false);

        panel.add(button);
        panel.add(subMenuPanel);

        boolean isFolder = navigatorElement.children.size() > 0;
        if (isFolder) {
            button.addStyleName("nav-folder collapsed");
            
            for (GNavigatorElement child : navigatorElement.children) {
                createMenuItem(subMenuPanel, child, level + 1);
            }
        }

        Event.sinkEvents(button.getElement(), Event.ONCLICK);
        Event.setEventListener(button.getElement(), event -> {
            if (Event.ONCLICK == event.getTypeInt()) {
                if (isFolder) {
                    boolean wasVisible = subMenuPanel.isVisible();
                    subMenuPanel.setVisible(!wasVisible);
                    button.setStyleName("collapsed", wasVisible);
                } else {
                    navigatorController.openElement(navigatorElement, event);
                    closeNavigatorMenu();
                }
            }
        });
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