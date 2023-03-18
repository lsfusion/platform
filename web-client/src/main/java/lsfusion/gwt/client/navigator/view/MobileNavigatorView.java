package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;

import java.util.ArrayList;
import java.util.function.Predicate;

public abstract class MobileNavigatorView {

    protected final GINavigatorController navigatorController;
    protected final WindowsController windowsController;

    protected final static Predicate<GNavigatorWindow> ANY = navigatorWindow -> true;

    protected GNavigatorWindow logo;
    protected GNavigatorWindow system;
    protected GNavigatorWindow toolbar;


    protected static class RootPanels {

        ComplexPanel mainPanel;

        Predicate<GNavigatorWindow>[] windows;
        ComplexPanel[] windowPanels;

        public RootPanels(ComplexPanel mainPanel, Predicate<GNavigatorWindow>[] windows, ComplexPanel[] windowPanels) {
            this.mainPanel = mainPanel;
            this.windows = windows;
            this.windowPanels = windowPanels;
        }
    }

    protected MobileNavigatorView(ArrayList<GNavigatorWindow> navigatorWindows, WindowsController windowsController, GINavigatorController navigatorController) {
        this.navigatorController = navigatorController;
        this.windowsController = windowsController;

        for(GNavigatorWindow navigatorWindow : navigatorWindows) {
            if(navigatorWindow.isLogo())
                logo = navigatorWindow;
            if(navigatorWindow.isSystem())
                system = navigatorWindow;
            if(navigatorWindow.isToolbar())
                toolbar = navigatorWindow;
        }

        RootPanels rootPanels = initRootPanels();

        for(int i=0;i<rootPanels.windows.length;i++) {
            createChildrenMenuItems(rootPanels.windowPanels[i], rootPanels.windows[i], navigatorController.getRoot(), -1);
        }


        ComplexPanel mainPanel = rootPanels.mainPanel;

        windowsController.initNavigatorRootView(mainPanel);
        RootLayoutPanel.get().add(mainPanel);
        enable(mainPanel);
    }

    private final NativeSIDMap<GNavigatorElement, NavigatorImageButton> navigatorItems = new NativeSIDMap<>();

    protected void createNavigatorItem(ComplexPanel panel, GNavigatorElement navigatorElement, int level) {
        boolean isFolder = navigatorElement instanceof GNavigatorFolder;
        NavigatorImageButton button = new NavigatorImageButton(navigatorElement, false, isFolder, level); // somewhy folder should be span (otherwise there are some odd borders to the right)
        navigatorItems.put(navigatorElement, button);

        panel = wrapMenuItem(panel, level, button);
        panel.add(button);

        if (isFolder) {
            ComplexPanel subMenuPanel = initFolderPanel(button);

            createChildrenMenuItems(subMenuPanel, ANY, navigatorElement, level);

            panel.add(subMenuPanel);
        } else {
            button.addClickHandler(event -> {
                navigatorController.openElement((GNavigatorAction) navigatorElement, event.getNativeEvent());
                closeNavigatorMenu();
            });
        }
    }

    public void updateImage(GNavigatorElement navigatorElement) {
        navigatorItems.get(navigatorElement).updateImage();
    }

    public void updateText(GNavigatorElement navigatorElement) {
        navigatorItems.get(navigatorElement).updateText();
    }

    public void updateElementClass(GNavigatorElement navigatorElement) {
        navigatorItems.get(navigatorElement).updateElementClass();
    }

    protected void createChildrenMenuItems(ComplexPanel subMenuPanel, Predicate<GNavigatorWindow> window, GNavigatorElement navigatorElement, int level) {
        for (GNavigatorElement child : navigatorElement.children) {
            if(window.test(child.window))
                createNavigatorItem(subMenuPanel, child, level + 1);
        }
    }

    protected abstract RootPanels initRootPanels();

    protected abstract ComplexPanel initFolderPanel(NavigatorImageButton button);

    protected abstract ComplexPanel wrapMenuItem(ComplexPanel panel, int level, ImageButton button);

    protected abstract void enable(ComplexPanel navBarPanel);

    public abstract void openNavigatorMenu();

    public abstract void closeNavigatorMenu();
}
