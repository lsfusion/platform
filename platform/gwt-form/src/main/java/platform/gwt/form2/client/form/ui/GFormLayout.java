package platform.gwt.form2.client.form.ui;

import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.client.form.ui.container.GAbstractFormContainer;
import platform.gwt.form2.client.form.ui.container.GFormContainer;
import platform.gwt.form2.client.form.ui.container.GFormSplitPane;
import platform.gwt.form2.client.form.ui.container.GFormTabbedPane;
import platform.gwt.form2.shared.view.GComponent;
import platform.gwt.form2.shared.view.GContainer;
import platform.gwt.form2.shared.view.GGroupObject;
import platform.gwt.form2.shared.view.GShowType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GFormLayout extends FlowPanel {
    private Panel mainContainer;
    private GContainer mainKey;
    private Map<GContainer, GAbstractFormContainer> containerViews = new HashMap<GContainer, GAbstractFormContainer>();
    private List<GFormSplitPane> splitPanels = new ArrayList<GFormSplitPane>();

    public GFormLayout(GFormController formController, GContainer mainContainer) {

        addStyleName("formLayout");

        createContainerViews(formController, mainContainer);
        adjustFills(mainContainer);
        adjustContainerSizes(mainContainer);

        setSize("100%", "100%");

        add(this.mainContainer);
    }

    private void createContainerViews(GFormController formController, GContainer container) {
        GAbstractFormContainer formContainer;
        if (container.type.isSplit()) {
            formContainer = new GFormSplitPane(container);
            splitPanels.add((GFormSplitPane) formContainer);
        } else if (container.type.isTabbed()) {
            formContainer = new GFormTabbedPane(formController, container);
        } else {
            formContainer = new GFormContainer(container);
        }

        if (container.container == null) {
            mainContainer = (Panel) formContainer.getContainerView();
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

    public Widget getFormContainerView(GContainer component) {
        return getFormContainer(component).getContainerView();
    }

    public GAbstractFormContainer getComponentParentFormContainer(GComponent component) {
        return component == null ? null : getFormContainer(component.container);
    }

    public void hideEmptyContainerViews() {
        hideEmptyContainerViews(mainKey);
    }

    private void hideEmptyContainerViews(GContainer container) {
        Widget containerView = getFormContainerView(container);

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
            if (getFormContainer(container).isInSplitPane()) {
                ((GFormSplitPane) getFormContainer(container.container)).update();
            }
        }
    }

    public boolean hasVisibleChildren(GContainer container) {
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

    private void adjustFills(GContainer container) {
        for (GComponent child : container.children) {
            if (child instanceof GContainer) {
                adjustFills((GContainer) child);
            }
        }

        if (container.absoluteWidth == -1) {
            if (container.fillHorizontal < 0) {
                double childFill = getChildFill(container, false);
                if (childFill > 0) {
                    container.fillHorizontal = childFill;
                }
            }
        }

        if (container.absoluteHeight == -1) {
            if (container.fillVertical < 0) {
                double childFill = getChildFill(container, true);
                if (childFill > 0) {
                    container.fillVertical = childFill;
                }
            }
        }
    }

    private double getChildFill(GContainer container, boolean vertical) {
        double fill = 0;
        for (GComponent child : container.children) {
            if ((vertical && child.fillVertical > 0) || (!vertical && child.fillHorizontal > 0)) {
                fill += vertical ? child.fillVertical : child.fillHorizontal;
            }
        }
        return fill;
    }

    private void adjustContainerSizes(GContainer container) {
        String width;
        String height;
        Widget view = getFormContainerView(container);

        if (!container.resizable) {
            view.setSize("auto", "auto");
            return;
        } else {
            for (GComponent child : container.children) {
                if (child instanceof GContainer) {
                    adjustContainerSizes((GContainer) child);
                }
            }

            if (container.container != null && container.container.type.isTabbed()) {
                width = "100%";
                height = "100%";
            } else {
                width = calculateSize(container, true);
                height = calculateSize(container, false);
            }
        }

        GAbstractFormContainer parentView = getComponentParentFormContainer(container);
        if (parentView != null && parentView.isSplit()) {
            if (width != null) {
                ((GFormSplitPane) parentView).setWidgetSize(view, width, true);
            }
            if (height != null) {
                ((GFormSplitPane) parentView).setWidgetSize(view, height, false);
            }
        } else {
            if (parentView != null && parentView.getContainerView() instanceof CellPanel) {
                if (width != null) {
                    ((CellPanel) parentView.getContainerView()).setCellWidth(view, width);
                }
                if (height != null) {
                    ((CellPanel) parentView.getContainerView()).setCellHeight(view, height);
                }
            } else {
                if (width != null) {
                    view.setWidth(width);
                }
                if (height != null) {
                    view.setHeight(height);
                }
            }
        }
    }

    private String calculateSize(GContainer container, boolean width) {
        if (container.container == null) {
            return "100%";
        }

        if ((width && container.absoluteWidth != -1) || (!width && container.absoluteHeight != -1)) {
            return (width ? container.absoluteWidth : container.absoluteHeight) + "px";
        }

        if ((width && container.fillHorizontal <= 0) || (!width && container.fillVertical <= 0)) {
            return null;
        }

        double sum = 0;
        for (GComponent child : container.container.children) {
            if (width) {
                if (!container.container.isVertical) {
                    if (child.fillHorizontal > 0) {
                        sum += child.fillHorizontal;
                    }
                } else {
                    return "100%";
                }
            } else {
                if (container.container.isVertical) {
                    if (child.fillVertical > 0) {
                        sum += child.fillVertical;
                    }
                } else {
                    return "100%";
                }
            }
        }
        return (width ? container.fillHorizontal : container.fillVertical) / sum * 100 + "%";
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
}
