package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.*;
import lsfusion.gwt.client.form.object.table.grid.GGrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GFormLayout extends ResizableSimplePanel {

    private GFormController form;

    private GContainer mainContainer;

    private Map<GContainer, GAbstractContainerView> containerViews = new HashMap<>();
    private Map<GComponent, Widget> baseComponentViews = new HashMap<>();

    private ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public GFormLayout(GFormController iform, GContainer mainContainer) {
        this.form = iform;

        this.mainContainer = mainContainer;

        GAbstractContainerView mainContainerView = addContainers(mainContainer);

        setFillWidget(mainContainerView.getView());
        
        getWidget().getElement().getStyle().setOverflow(Style.Overflow.AUTO);
    }

    private static GAbstractContainerView createContainerView(GFormController form, GContainer container) {
        if (container.isLinear()) {
            return new LinearContainerView(container);
        } else if (container.isSplit()) {
            return new SplitContainerView(container);
        } else if (container.isTabbed()) {
            return new TabbedContainerView(form, container);
        } else if (container.isColumns()) {
            return new ColumnsContainerView(container);
        } else if(container.isScroll()) {
            return new ScrollContainerView(container);
        } else {
            throw new IllegalStateException("Incorrect container type");
        }
    }

    // creating containers (all other components are created when creating controllers)
    private GAbstractContainerView addContainers(GContainer container) {
        GAbstractContainerView containerView = createContainerView(form, container);

        containerViews.put(container, containerView);
        Widget view = containerView.getView();
        if (container.sID != null) {
            view.getElement().setAttribute("lsfusion-container", container.sID);
            view.getElement().setAttribute("lsfusion-container-type", container.type.name());
        }

        add(container, view, null);

        for (GComponent child : container.children) {
            if(child instanceof GGrid)
                child = ((GGrid)child).record;
            if (child instanceof GContainer) {
                addContainers((GContainer) child);
            }
        }
        return containerView;
    }
    public void addBaseComponent(GComponent component, Widget view, DefaultFocusReceiver focusReceiver) {
        assert !(component instanceof GContainer);
        baseComponentViews.put(component, view);
        add(component, view, focusReceiver);
    }

    public void add(GComponent key, Widget view, DefaultFocusReceiver focusReceiver) {
        GAbstractContainerView containerView;
        if(key.container != null && (containerView = containerViews.get(key.container)) != null) { // container can be null when component should be layouted manually, containerView can be null when it is removed 
            containerView.add(key, view);

            maybeAddDefaultFocusReceiver(key, focusReceiver);
        }
    }

    public void remove(GComponent key, Widget view) {
        assert !(key instanceof GContainer);
        GAbstractContainerView containerView;
        if (key.container != null && (containerView = containerViews.get(key.container)) != null) { // see add method
            containerView.remove(key);

            maybeRemoveDefaultFocusReceiver(key);
        }
    }

    public void removeBaseComponent(GComponent key, Widget view) {
        assert !(key instanceof GContainer);
        baseComponentViews.remove(key);
        remove(key, view);
    }

    private void maybeAddDefaultFocusReceiver(GComponent key, DefaultFocusReceiver focusReceiver) {
        if (key.defaultComponent && focusReceiver != null) {
            defaultComponents.add(key);
            defaultFocusReceivers.add(focusReceiver);
        }
    }

    private void maybeRemoveDefaultFocusReceiver(GComponent key) {
        int index = defaultComponents.indexOf(key);
        if (index != -1) {
            defaultComponents.remove(index);
            defaultFocusReceivers.remove(index);
        }
    }

    public boolean focusDefaultWidget() {
        for (DefaultFocusReceiver dc : defaultFocusReceivers) {
            if (dc.focus()) {
                return true;
            }
        }
        return false;
    }

    public GAbstractContainerView getContainerView(GContainer container) {
        return containerViews.get(container);
    }
    public Widget getComponentView(GComponent component) {
        if(component instanceof GContainer)
            return getContainerView((GContainer) component).getView();
        return baseComponentViews.get(component);
    }

    public void hideEmptyContainerViews() {
        autoShowHideContainers(mainContainer);
    }

    private void autoShowHideContainers(GContainer container) {
        GAbstractContainerView containerView = getContainerView(container);
        int childCnt = containerView.getChildrenCount();
        boolean hasVisible = false;
        for (int i = 0; i < childCnt; ++i) {
            GComponent child = containerView.getChild(i);
            Widget childView = containerView.getChildView(i);
            if (child instanceof GContainer) {
                autoShowHideContainers((GContainer) child);
            }

            if (childView.isVisible()) {
                hasVisible = true;
            }
        }
        containerView.getView().setVisible(hasVisible);
        containerView.updateLayout();
    }

    @Override
    public Dimension getMaxPreferredSize() {
        Dimension result = GAbstractContainerView.getMaxPreferredSize(mainContainer, containerViews, false); // в BOX container'е берем явный size (предполагая что он используется не как базовый размер с flex > 0, а конечный)
        setDebugDimensionsAttributes(containerViews.get(mainContainer).getView(), result);
        return result;
    }

    public static void setDebugDimensionsAttributes(Widget w, Dimension result) {
        w.getElement().setAttribute("lsfusion-size", "(" + result.width + ", " + result.height + ")");
    }
}