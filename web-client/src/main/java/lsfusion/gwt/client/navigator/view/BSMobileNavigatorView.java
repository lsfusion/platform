package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

import java.util.ArrayList;
import java.util.function.Predicate;

public class BSMobileNavigatorView extends MobileNavigatorView {
    public static final String OFFCANVAS_ID = "mobileMenuOffcanvas";

    public BSMobileNavigatorView(ArrayList<GNavigatorWindow> navigatorWindows, WindowsController windowsController, GINavigatorController navigatorController) {
        super(navigatorWindows, windowsController, navigatorController);
    }

    protected RootPanels initRootPanels() {
        FlexPanel navWindowsPanel = new FlexPanel(true);
        GwtClientUtils.addClassNames(navWindowsPanel, "offcanvas", "offcanvas-start");

        Element navBarPanelElement = navWindowsPanel.getElement();
        navBarPanelElement.setId(OFFCANVAS_ID);
        navBarPanelElement.getStyle().setOverflowY(Style.Overflow.AUTO);
        navBarPanelElement.getStyle().setOverflowX(Style.Overflow.HIDDEN);

        Predicate<GNavigatorWindow>[] windows = new Predicate[3];
        ComplexPanel[] windowPanels = new ComplexPanel[3];
        GAbstractWindow[] cssWindows = new GAbstractWindow[3];

        for(int i=0;i<3;i++) {
            GNavigatorWindow cssWindow;
            Predicate<GNavigatorWindow> filterWindow;
            boolean flex = false;
            if(i == 0) {
                cssWindow = logo;
                filterWindow = navigatorWindow -> navigatorWindow == logo;
            } else if(i == 1) {
                cssWindow = toolbar;
                filterWindow = navigatorWindow -> navigatorWindow != logo && navigatorWindow != system;
                flex = true;
            } else {
                cssWindow = system;
                filterWindow = navigatorWindow -> navigatorWindow == system;
            }

            ToolbarPanel main = new ToolbarPanel(true, toolbar);

            if(flex)
                navWindowsPanel.addFill(main);
            else
                navWindowsPanel.addStretched(main);

            navigatorController.initMobileNavigatorView(cssWindow, main);
            windowsController.registerMobileWindow(cssWindow);

            windows[i] = filterWindow;
            windowPanels[i] = main.panel;
            cssWindows[i] = cssWindow;
        }

        return new RootPanels(navWindowsPanel, windows, windowPanels, cssWindows);
    }

    protected ComplexPanel initFolderPanel(NavigatorImageButton button) {
        FlexPanel subMenuPanel = new FlexPanel(true);

        subMenuPanel.setVisible(false);
        GwtClientUtils.addClassNames(button, "nav-bs-mobile-folder", "collapsed");
        button.addClickHandler(event -> {
            boolean wasVisible = subMenuPanel.isVisible();
            subMenuPanel.setVisible(!wasVisible);
            if(wasVisible)
                GwtClientUtils.addClassName(button, "collapsed");
            else
                GwtClientUtils.removeClassName(button, "collapsed");
        });

        return subMenuPanel;
    }

    protected ComplexPanel wrapMenuItem(ComplexPanel panel, int level, ImageButton button) {
        return panel;
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