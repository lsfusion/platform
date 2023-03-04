package lsfusion.gwt.client.navigator.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;

import java.util.ArrayList;
import java.util.function.Predicate;

public class BSMobileNavigatorView extends MobileNavigatorView {
    public static final String OFFCANVAS_ID = "mobileMenuOffcanvas";

    public BSMobileNavigatorView(GNavigatorElement root, ArrayList<GNavigatorWindow> navigatorWindows, GINavigatorController navigatorController) {
        super(root, navigatorWindows, navigatorController);
    }

    protected RootPanels initRootPanels() {
        FlexPanel navWindowsPanel = new FlexPanel(true);
        navWindowsPanel.addStyleName("offcanvas offcanvas-start");

        Element navBarPanelElement = navWindowsPanel.getElement();
        navBarPanelElement.setId(OFFCANVAS_ID);
        navBarPanelElement.getStyle().setOverflowY(Style.Overflow.AUTO);
        navBarPanelElement.getStyle().setOverflowX(Style.Overflow.HIDDEN);

        Predicate<GNavigatorWindow>[] windows = new Predicate[3];
        ComplexPanel[] windowPanels = new ComplexPanel[3];

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

            Pair<ResizableComplexPanel, ResizableComplexPanel> toolbarPanel = GToolbarNavigatorView.createToolbarPanel(true);
            ResizableComplexPanel main = toolbarPanel.first;
            ResizableComplexPanel panel = toolbarPanel.second;

            GToolbarNavigatorView.setAlignment(true, main, panel, (GToolbarNavigatorWindow) toolbar);

            if(flex)
                navWindowsPanel.addFill(main);
            else
                navWindowsPanel.addStretched(main);

            String elementClass = cssWindow.elementClass;
            if(elementClass != null)
                main.addStyleName(elementClass);

            windows[i] = filterWindow;
            windowPanels[i] = panel;
        }

        return new RootPanels(navWindowsPanel, windows, windowPanels);
    }

    protected ComplexPanel initFolderPanel(NavigatorImageButton button) {
        FlexPanel subMenuPanel = new FlexPanel(true);

        subMenuPanel.setVisible(false);
        button.addStyleName("nav-folder collapsed");
        button.addClickHandler(event -> {
            boolean wasVisible = subMenuPanel.isVisible();
            subMenuPanel.setVisible(!wasVisible);
            button.setStyleName("collapsed", wasVisible);
        });

        return subMenuPanel;
    }

    protected ComplexPanel initMenuItem(ComplexPanel panel, int level, ImageButton button) {
        button.addStyleName("nav-item nav-link navbar-text nav-link-horz");
        button.addStyleName("nav-link-horz-" + level);
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