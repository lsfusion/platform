package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public class ToolbarPanel extends NavigatorPanel {

    public ToolbarPanel(boolean vertical, GToolbarNavigatorWindow window) {
        super(vertical);

        addStyleName("navbar-expand"); // navbar-expand to set horizontal paddings (vertical are set in navbar-text)

        setAlignment(vertical, panel, window);
    }

    private static void setAlignment(boolean vertical, ResizableComplexPanel panel, GToolbarNavigatorWindow toolbarWindow) {
        if (vertical) {
            panel.addStyleName(toolbarWindow.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            // adding margin-auto to make all buttons visible when scroll appears
            panel.addStyleName(toolbarWindow.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center my-auto" :
                    (toolbarWindow.alignmentY == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        } else {
            panel.addStyleName(toolbarWindow.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentY == GToolbarNavigatorWindow.BOTTOM_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            // adding margin-auto to make all buttons visible when scroll appears
            panel.addStyleName(toolbarWindow.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center mx-auto" :
                    (toolbarWindow.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        }
    }

    public static boolean hasBorder(Widget widget) {
        return MainFrame.useBootstrap ? GwtClientUtils.hasClassNamePrefix(widget.getElement().getClassName(), WindowsController.BACKGROUND_PREFIX) : false;
    }

    public static boolean isPopupOver(Widget widget) {
        return widget.getElement().hasClassName(WindowsController.NAVBAR_POPUP_OVER_SELECTED_HOVER);
    }
}
