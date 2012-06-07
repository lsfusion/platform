package platform.gwt.form.client.ui;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.*;
import platform.gwt.base.shared.GContainerType;
import platform.gwt.view.*;

import java.util.HashMap;
import java.util.Map;

public abstract class GFormLayout extends VLayout {
    private Layout mainContainer;
    private GContainer mainKey;
    private Map<GContainer, GAbstractFormContainer> contViews = new HashMap<GContainer, GAbstractFormContainer>();

    public GFormLayout(GContainer mainContainer) {
        createContainerViews(mainContainer);
        addMember(this.mainContainer);
    }

    private void createContainerViews(GContainer container) {
        GAbstractFormContainer formContainer;
        if (GContainerType.isSplitPane(container.type)) {
            formContainer = new GFormSplitPane(container);
        } else if (GContainerType.isTabbedPane(container.type)) {
            formContainer = new GFormTabbedPane(container);
        } else {
            formContainer = new GFormContainer(container);
        }

        if (container.container == null) {
            mainContainer = formContainer.getComponent();
            mainKey = formContainer.getKey();
        } else {
            GAbstractFormContainer parent = contViews.get(container.container);
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

        GAbstractFormContainer keyContView = contViews.get(key.container);
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

        GAbstractFormContainer keyContView = contViews.get(key.container);
        if (keyContView == null) {
            return false;
        }

        keyContView.remove(key);
        return true;
    }

    public void resizeAll() {
        adjustContainerSize(mainKey);
    }

    private void adjustContainerSize(GContainer container) {
        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                adjustContainerSize((GContainer) child);
            }
        }

        Layout view = contViews.get(container).getComponent();
        if (!contViews.get(container).isInSplitPane())
            if (hasCollapsibleMembers(container)) {
                view.setHeight100();
                view.setWidth100();
            } else if (!contViews.get(container).isTabbed()) {
                view.setAutoHeight();
                view.setAutoWidth();
            }
    }

    // похоже, не только ListGrid, но и TabSet ведёт себя пассивно при нахождении в Layout'е с маленикими размерами,
    // т.е. сам уменьшается в размерах вместо того, чтобы расширить свой parent Layout
    private boolean hasCollapsibleMembers(GContainer container) {
        for (GComponent child : container.children) {
            if (child instanceof GGrid || child instanceof GTreeGroup) {
                boolean result = contViews.get(container).drawsChild(child);
                if (result)
                    return true;
            } else if (child instanceof GContainer) {
                if (GContainerType.isTabbedPane(((GContainer) child).type))
                    return true;
                boolean has = hasCollapsibleMembers((GContainer) child);
                if (has)
                    return true;
            }
        }
        return false;
    }

    public void hideEmpty() {
        hide(mainKey);
    }

    private void hide(GContainer container) {
        Layout view = contViews.get(container).getComponent();

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                hide((GContainer) child);
            }
        }

        if (!contViews.get(container).isInTabbedPane()) //предоставляем TabbedPane'у самому управлять видимостью своих компонентов
            if (!hasVisibleChildren(container)) {
                view.setVisible(false);
            } else {
                view.setVisible(true);
            }
    }

    private boolean hasVisibleChildren(GContainer container) {
        for (GComponent child : container.children) {
            // поскольку при отрисовке groupObject'а в панели ShowTypeView рисуем в тулбаре, а не в соответствующем ему
            // по иерархии контейнере, то поиск его в этом контейнере не даст положительного результата
            if (child instanceof GShowType && isShowTypeInItsPlace(((GShowType) child).groupObject)) {
                return true;
            } else if (child instanceof GContainer) {
                if (hasVisibleChildren((GContainer) child))
                    return true;
            } else if (contViews.get(container).drawsChild(child))
                return true;
        }
        return false;
    }

    public abstract boolean isShowTypeInItsPlace(GGroupObject groupObject);
}
