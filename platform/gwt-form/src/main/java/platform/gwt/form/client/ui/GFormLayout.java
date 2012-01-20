package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.view.GComponent;
import platform.gwt.view.GContainer;

import java.util.HashMap;
import java.util.Map;

public class GFormLayout extends VLayout {
    private Layout mainContainer;
    private GContainer mainKey;
    private Map<GContainer, GFormContainer> contViews = new HashMap<GContainer, GFormContainer>();

    public GFormLayout(GContainer mainContainer) {
        createContainerViews(mainContainer);
        addMember(this.mainContainer);
    }

    private void createContainerViews(GContainer container) {
        GFormContainer formContainer = new GFormContainer(container);

        if (container.container == null) {
            mainContainer = formContainer.getComponent();
            mainKey = formContainer.getKey();
        } else {
            GFormContainer parent = contViews.get(container.container);
            parent.add(container, formContainer.getComponent());
        }

        contViews.put(container, formContainer);

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                createContainerViews((GContainer) child);
            }
        }
    }

    public boolean add(GComponent key, Canvas view, int position) {
        if (key == null) {
            return false;
        }

        GFormContainer keyContView = contViews.get(key.container);
        if (keyContView == null) {
            return false;
        }

        keyContView.add(key, view, position);
        return true;
    }

    public void add(GComponent key, Canvas view) {
        add(key, view, -1);
    }

    public boolean remove(GComponent key) {
        if (key == null) {
            return false;
        }

        GFormContainer keyContView = contViews.get(key.container);
        if (keyContView == null) {
            return false;
        }

        keyContView.remove(key);
        return true;
    }

    public void hideEmpty() {
        hide(mainKey);
    }

    private void hide(GContainer container) {
        Layout view = contViews.get(container).getComponent();
        if (!hasVisibleChildren(container)) {
            view.setVisible(false);
        } else {
            view.setVisible(true);
            for (GComponent child : container.children) {
                if (child instanceof GContainer) {
                    hide((GContainer) child);
                }
            }
        }
    }

    private boolean hasVisibleChildren(GContainer container) {
        if (contViews.get(container).needToBeHidden()) {
            return false;
        }
        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                if (hasVisibleChildren((GContainer) child))
                    return true;
            } else if (contViews.get(container).drawsChild(child))
                return true;
        }
        return false;
    }
}
