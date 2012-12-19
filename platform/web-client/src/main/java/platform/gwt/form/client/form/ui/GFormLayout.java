package platform.gwt.form.client.form.ui;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.form.client.form.ui.container.*;
import platform.gwt.form.shared.view.GComponent;
import platform.gwt.form.shared.view.GContainer;
import platform.gwt.form.shared.view.GGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GFormLayout extends FlowPanel {
    private Panel mainContainer;
    private GContainer mainKey;
    private Map<GContainer, GAbstractFormContainer> containerViews = new HashMap<GContainer, GAbstractFormContainer>();

    public GFormLayout(GFormController formController, GContainer mainContainer) {

        addStyleName("formLayout");

        mainKey = mainContainer;
        mainKey.calculateFills();
        createContainerViews(formController, mainContainer);

        setSize("100%", "100%");

        add(this.mainContainer);
    }

    private void createContainerViews(GFormController formController, GContainer container) {
        GAbstractFormContainer formContainer;
        if (container.type.isSplit()) {
            formContainer = new GFormSplitPane(container, formController.getForm().allowScrollSplits);
        } else if (container.type.isTabbed()) {
            formContainer = new GFormTabbedPane(formController, container);
        } else if (container.toFlow()) {
            formContainer = new GFormFlowPanel(container);
        } else {
            formContainer = new GFormContainer(container);
        }

        if (container.container == null) {
            mainContainer = (Panel) formContainer.getContainerView();
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

    private Widget getFormContainerView(GContainer component) {
        GAbstractFormContainer formContainer = getFormContainer(component);
        return formContainer == null ? null : formContainer.getContainerView();
    }

    private GAbstractFormContainer getComponentParentFormContainer(GComponent component) {
        return component == null ? null : getFormContainer(component.container);
    }

    public void hideEmptyContainerViews() {
        hideEmptyContainerViews(mainKey);
    }

    private void hideEmptyContainerViews(GContainer container) {

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                hideEmptyContainerViews((GContainer) child);
            }
        }

        //предоставляем TabbedPane'у самому управлять видимостью своих компонентов
        GAbstractFormContainer formContainer = getFormContainer(container);
        if (formContainer != null && !formContainer.isInTabbedPane()) {
            formContainer.getContainerView().setVisible(
                    hasVisibleChildren(container)
            );
            if (formContainer.isInSplitPane()) {
                ((GFormSplitPane) getFormContainer(container.container)).update();
            }
        }
    }

    public boolean hasVisibleChildren(GContainer container) {
        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                if (hasVisibleChildren((GContainer) child)) {
                    return true;
                }
            } else {
                GAbstractFormContainer formContainer = getFormContainer(container);
                if (formContainer != null && formContainer.isChildVisible(child)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void totalResize() {
        adjustContainerSizes(mainKey);
    }

    public void adjustContainerSizes(GContainer container) {
        Widget view = getFormContainerView(container);

        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                adjustContainerSizes((GContainer) child);
            } else {
                adjustComponentSize(child);
            }
        }

        String width = calculateSize(container, true);
        String height = calculateSize(container, false);
        GAbstractFormContainer parentView = getComponentParentFormContainer(container);
        if (parentView != null) {
            parentView.setChildSize(container, width, height);
        } else {
            view.setSize(width, height);
        }
    }

    private void adjustComponentSize(GComponent component) {
        GAbstractFormContainer parentView = getComponentParentFormContainer(component);
        if (parentView != null) {
            String width = calculateSize(component, true);
            String height = calculateSize(component, false);
            parentView.setChildSize(component, width, height);
        }
    }

    private String calculateSize(GComponent component, boolean width) {
        GContainer container = component.container;
        if (container == null) {
            return shouldBeCollapsed(component, width) ? "auto" : "100%";
        }

        if ((width && component.getAbsoluteWidth() != -1) || (!width && component.getAbsoluteHeight() != -1)) {
            return (width ? component.getAbsoluteWidth() : component.getAbsoluteHeight()) + "px";
        }

        if (shouldBeCollapsed(component, width)) {
            return "auto";
        }

        double sum = 0;
        for (GComponent child : container.children) {
            if (!shouldBeCollapsed(child, width)) {
                if (width) {
                    sum += child.fillHorizontal;
                } else {
                    sum += child.fillVertical;
                }
            }
        }
        return (width ? component.fillHorizontal : component.fillVertical) / sum * 100 + "%";
    }

    private boolean shouldBeCollapsed(GComponent component, boolean width) {
        if ((width && component.fillHorizontal <= 0) || (!width && component.fillVertical <= 0)) {
            return true;
        }

        GAbstractFormContainer parentContainer = getComponentParentFormContainer(component);
        if (parentContainer != null && !parentContainer.isChildVisible(component)) {
            return true;
        }

        if (component instanceof GContainer) {
            if (((GContainer) component).hasSingleGridInTree()) {
                return true;
            }

            for (GComponent child : ((GContainer) component).children) {
                boolean result = shouldBeCollapsed(child, width);
                if (!result) {
                    return false;
                }
            }

            List<GGrid> grids = ((GContainer) component).getAllGrids();
            for (GGrid grid : grids) {
                GAbstractFormContainer gridContainer = getFormContainer(grid.container);
                if (gridContainer != null && gridContainer.isChildVisible(grid)) {
                    return false;
                }
            }

            if (!((GContainer) component).containsTreeGroup()){
                return true;
            }
        }
        return false;
    }

    public GContainer getMainKey() {
        return mainKey;
    }

    public void setTableCellSize(GContainer container, GContainer childContainer, String size, boolean width) {
        Widget childContainerView = getFormContainerView(childContainer);
        if (childContainerView != null) {
            setTableCellSize(container, childContainerView, size, width);
        }
    }

    public void setTableCellSize(GContainer container, Widget child, String size, boolean width) {
        GAbstractFormContainer formContainer = getFormContainer(container);
        if (formContainer != null) {
            formContainer.setTableCellSize(child, size, width);
        }
    }

    public int getMainContainerWidth() {
        if (mainContainer != null) {
            return mainContainer.getOffsetWidth();
        }
        return -1;
    }

    public int getMainContainerHeight() {
        if (mainContainer != null) {
            return mainContainer.getOffsetHeight();
        }
        return -1;
    }
}
