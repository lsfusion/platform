package lsfusion.gwt.client.navigator.view;

import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.view.ImageButton;
import lsfusion.gwt.client.base.view.NavigatorImageButton;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;

public abstract class MobileNavigatorView {

    protected final GINavigatorController navigatorController;

    protected MobileNavigatorView(GNavigatorElement root, GINavigatorController navigatorController) {
        this.navigatorController = navigatorController;

        ComplexPanel navBarPanel = initRootPanel();

        ComplexPanel navPanel = createChildrenMenuItems(navBarPanel, root, -1);

        initSubRootPanel(navPanel);

        RootLayoutPanel.get().add(navBarPanel);

        enable(navBarPanel);
    }

    private final NativeSIDMap<GNavigatorElement, NavigatorImageButton> navigatorItems = new NativeSIDMap<>();

    protected void createNavigatorItem(ComplexPanel panel, GNavigatorElement navigatorElement, int level) {
        NavigatorImageButton button = new NavigatorImageButton(navigatorElement, false, navigatorElement.children.size() > 0); // somewhy folder should be span (otherwise there are some odd borders to the right)
        navigatorItems.put(navigatorElement, button);
        initMenuItem(level, button);

        panel = wrapNavigatorItem(panel);
        panel.add(button);

        boolean isFolder = navigatorElement.children.size() > 0;
        if (isFolder) {
            ComplexPanel subMenuPanel = createChildrenMenuItems(panel, navigatorElement, level);

            initSubMenuItem(button, subMenuPanel);
        } else {
            button.addClickHandler(event -> {
                navigatorController.openElement(navigatorElement, event.getNativeEvent());
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

    protected abstract void enable(ComplexPanel navBarPanel);

    protected abstract void initSubMenuItem(ImageButton button, ComplexPanel subMenuPanel);

    protected abstract ComplexPanel wrapNavigatorItem(ComplexPanel panel);

    protected abstract void initMenuItem(int level, ImageButton button);

    protected ComplexPanel createChildrenMenuItems(ComplexPanel panel, GNavigatorElement navigatorElement, int level) {
        ComplexPanel subMenuPanel = initSubMenuPanel();

        for (GNavigatorElement child : navigatorElement.children) {
            createNavigatorItem(subMenuPanel, child, level + 1);
        }

        panel.add(subMenuPanel);
        return subMenuPanel;
    }

    protected abstract ComplexPanel initRootPanel();

    protected abstract void initSubRootPanel(ComplexPanel rootPanel);

    protected abstract ComplexPanel initSubMenuPanel();

    public abstract void openNavigatorMenu();

    public abstract void closeNavigatorMenu();
}
