package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form2.client.form.ui.container.GAbstractFormContainer;
import platform.gwt.form2.client.form.ui.container.GFormContainer;
import platform.gwt.form2.client.form.ui.container.GFormTabbedPane;
import platform.gwt.view2.GComponent;
import platform.gwt.view2.GContainer;
import platform.gwt.view2.GGroupObject;
import platform.gwt.view2.GShowType;

import java.util.HashMap;
import java.util.Map;

public abstract class GFormLayout extends FlowPanel {
    private Panel mainContainer;
    private GContainer mainKey;
    private Map<GContainer, GAbstractFormContainer> containerViews = new HashMap<GContainer, GAbstractFormContainer>();

    public GFormLayout(GFormController formController, GContainer mainContainer) {

        addStyleName("formLayout");

        createContainerViews(formController, mainContainer);
        add(this.mainContainer);
    }

    private void createContainerViews(GFormController formController, GContainer container) {
        GAbstractFormContainer formContainer;
        if (container.type.isSplit()) {
            //todo:
//            formContainer = new GFormSplitPane(container);
            formContainer = new GFormContainer(container);
        } else if (container.type.isSplit()) {
            formContainer = new GFormTabbedPane(formController, container);
        } else {
            formContainer = new GFormContainer(container);
        }

        if (container.container == null) {
            mainContainer = formContainer.getContainerView();
            mainKey = formContainer.getKey();
        } else {
            add(container, formContainer.getContainerView());
        }

        containerViews.put(container, formContainer);

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                createContainerViews(formController, (GContainer) child);
            }
        }
    }

    public boolean add(GComponent key, Widget view, int position) {
        GAbstractFormContainer keyContView = getComponentParentFormContainer(key);
        if (keyContView == null) {
            return false;
        }

        keyContView.add(key, view, position);
        return true;
    }

    public void add(GComponent key, Widget view) {
        add(key, view, -1);
    }

    public boolean remove(GComponent key) {
        GAbstractFormContainer keyContView = getComponentParentFormContainer(key);
        if (keyContView == null) {
            return false;
        }

        keyContView.remove(key);
        return true;
    }

    public GAbstractFormContainer getFormContainer(GContainer container) {
        return containerViews.get(container);
    }

    public Panel getFormContainerView(GContainer component) {
        return getFormContainer(component).getContainerView();
    }

    public GAbstractFormContainer getComponentParentFormContainer(GComponent component) {
        return component == null ? null : getFormContainer(component.container);
    }

    public void hideEmptyContainerViews() {
        hideEmptyContainerViews(mainKey);
    }

    private void hideEmptyContainerViews(GContainer container) {
        Panel containerView = getFormContainerView(container);

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                hideEmptyContainerViews((GContainer) child);
            }
        }

        //предоставляем TabbedPane'у самому управлять видимостью своих компонентов
        if (!getFormContainer(container).isInTabbedPane()) {
            containerView.setVisible(
                    hasVisibleChildren(container)
            );
        }
    }

    private boolean hasVisibleChildren(GContainer container) {
        for (GComponent child : container.children) {
//            if (child instanceof GShowType) {
            if (child instanceof GShowType && isShowTypeViewInPanel(((GShowType) child).groupObject)) {
                return true;
            } else if (child instanceof GContainer) {
                if (hasVisibleChildren((GContainer) child)) {
                    return true;
                }
             } else if (getFormContainer(container).isChildVisible(child)) {
                return true;
            }
        }
        return false;
    }

    public abstract boolean isShowTypeViewInPanel(GGroupObject groupObject);

    public GContainer getMainKey() {
        return mainKey;
    }
}
