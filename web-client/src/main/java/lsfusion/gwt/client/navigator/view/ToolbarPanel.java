package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.navigator.window.GToolbarNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

public class ToolbarPanel extends ResizableComplexPanel {

    public final ResizableComplexPanel panel;

    public ToolbarPanel(boolean vertical, GToolbarNavigatorWindow window) {
        addStyleName("navbar navbar-expand p-0"); // navbar-expand to set horizontal paddings (vertical are set in navbar-text)

        addStyleName("navbar-" + (vertical ? "vert" : "horz"));

        panel = new ResizableComplexPanel();
        panel.addStyleName("navbar-nav");
        panel.addStyleName(vertical ? "navbar-nav-vert" : "navbar-nav-horz");

        setAlignment(true, panel, window);

        add(panel);
    }

    private static void setAlignment(boolean vertical, ResizableComplexPanel panel, GToolbarNavigatorWindow toolbarWindow) {
        if (vertical) {
            panel.addStyleName(toolbarWindow.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            panel.addStyleName(toolbarWindow.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center" :
                    (toolbarWindow.alignmentY == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        } else {
            panel.addStyleName(toolbarWindow.alignmentY == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "align-items-center" :
                    (toolbarWindow.alignmentY == GToolbarNavigatorWindow.BOTTOM_ALIGNMENT ? "align-items-end" :
                            "align-items-start"));

            panel.addStyleName(toolbarWindow.alignmentX == GToolbarNavigatorWindow.CENTER_ALIGNMENT ? "justify-content-center" :
                    (toolbarWindow.alignmentX == GToolbarNavigatorWindow.RIGHT_ALIGNMENT ? "justify-content-end" :
                            "justify-content-start"));
        }
    }

    public static boolean hasBorder(Widget widget) {
        return MainFrame.useBootstrap ? GwtClientUtils.hasClassNamePrefix(widget.getElement().getClassName(), WindowsController.BACKGROUND_PREFIX) : false;
    }
}
