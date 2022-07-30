package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LinearContainerView;
import lsfusion.gwt.client.form.object.table.grid.GGrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GFormLayout extends ResizableComplexPanel {

    private final GFormController form;

    private final GContainer mainContainer;

    private final Map<GContainer, GAbstractContainerView> containerViews = new HashMap<>();
    private final Map<GComponent, Widget> baseComponentViews = new HashMap<>();

    private final ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private final ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public final ResizableComplexPanel attachContainer;

    private final boolean autoSize;
    public GFormLayout(GFormController iform, GContainer mainContainer, boolean autoSize) {
        this.form = iform;
        this.mainContainer = mainContainer;
        this.autoSize = autoSize;

        attachContainer = new ResizableComplexPanel();
        attachContainer.setVisible(false);
        addContainers(mainContainer);

        Widget view = getMainView();
        setSizedMain(view, autoSize);
        view.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        add(attachContainer);

        DataGrid.initSinkMouseEvents(this);
    }

    public FormsController getFormsController() {
        return form.getFormsController();
    }

    public Widget getMainView() {
        return getContainerView(mainContainer).getView();
    }

    private static GAbstractContainerView createContainerView(GFormController form, GContainer container) {
        if (container.tabbed)
            return new TabbedContainerView(form, container);
        else if (container.isCustomDesign())
            return new CustomContainerView(form, container);
        else
            return new LinearContainerView(form, container);
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element target = DataGrid.getTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!form.previewEvent(target, event))
            return;

        super.onBrowserEvent(event);

        form.checkGlobalMouseEvent(event);
    }

    @Override
    public void onResize() {
        if (form.isVisible()) {
            super.onResize();
        }
    }

    // creating containers (all other components are created when creating controllers)
    private void addContainers(GContainer container) {
        GAbstractContainerView containerView = createContainerView(form, container);

        containerViews.put(container, containerView);
        Widget viewWidget = containerView.getView();
        add(container, new ComponentWidget(viewWidget), null);

        // debug info
        viewWidget.getElement().setAttribute("lsfusion-container-type", container.getContainerType());

        for (GComponent child : container.children) {
            if(child instanceof GGrid)
                child = ((GGrid)child).record;
            if (child instanceof GContainer) {
                addContainers((GContainer) child);
            }
        }
    }
    public void addBaseComponent(GComponent component, Widget view, DefaultFocusReceiver focusReceiver) {
        addBaseComponent(component, new ComponentWidget(view), focusReceiver);
    }
    public void addBaseComponent(GComponent component, ComponentWidget view, DefaultFocusReceiver focusReceiver) {
        assert !(component instanceof GContainer);
        baseComponentViews.put(component, view.getWidget());
        add(component, view, focusReceiver);
    }

    public void setShowIfVisible(GComponent component, boolean visible) {
        Widget widget = baseComponentViews.get(component);
        if(widget != null) {
            GwtClientUtils.setShowIfVisible(widget, visible);
        }
    }

    public void add(GComponent key, ComponentWidget view, DefaultFocusReceiver focusReceiver) {
        // debug info
        if (key.sID != null)
            view.getWidget().getElement().setAttribute("lsfusion-container", key.sID);

        GAbstractContainerView containerView;
        if(key.container != null && (containerView = containerViews.get(key.container)) != null) { // container can be null when component should be layouted manually, containerView can be null when it is removed 
            containerView.add(key, view);

            maybeAddDefaultFocusReceiver(key, focusReceiver);
        }
    }

    public void remove(GComponent key) {
        assert !(key instanceof GContainer);
        GAbstractContainerView containerView;
        if (key.container != null && (containerView = containerViews.get(key.container)) != null) { // see add method
            containerView.remove(key);

            maybeRemoveDefaultFocusReceiver(key);
        }
    }

    public void removeBaseComponent(GComponent key) {
        assert !(key instanceof GContainer);
        baseComponentViews.remove(key);
        remove(key);
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

    public boolean focusDefaultWidget(FocusUtils.Reason reason) {
        for (DefaultFocusReceiver dc : defaultFocusReceivers) {
            if (dc.focus(reason)) {
                return true;
            }
        }
        return false;
    }

    public GAbstractContainerView getContainerView(GContainer container) {
        return containerViews.get(container);
    }

    public void update(int requestIndex) {
        updateContainersVisibility(mainContainer, requestIndex);

        updatePanels();
    }

    public void updatePanels() {
        FlexPanel.updatePanels(getMainView());

        onResize();
    }

    private boolean updateContainersVisibility(GContainer container, long requestIndex) {
        GAbstractContainerView containerView = getContainerView(container);
        boolean hasVisible = false;
        int size = containerView.getChildrenCount();
        boolean[] childrenVisible = new boolean[size];
        for (int i = 0; i < size; ++i) {
            GComponent child = containerView.getChild(i);

            boolean childVisible;
            if (child instanceof GContainer)
                childVisible = updateContainersVisibility((GContainer) child, requestIndex);
            else {
                Widget childView = baseComponentViews.get(child); // we have to use baseComponentView (and not a wrapper in getChildView), since it has relevant visible state
                childVisible = childView != null && childView.isVisible();

                if (child instanceof GGrid) {
                    GContainer record = ((GGrid) child).record;
                    if(record != null)
                        updateContainersVisibility(record, requestIndex);
                }
            }

            childrenVisible[i] = childVisible;
            hasVisible = hasVisible || childVisible;
        }
        containerView.updateLayout(requestIndex, childrenVisible);
        return hasVisible;
    }

    public Dimension getPreferredSize(GSize maxWidth, GSize maxHeight, Element element) {
        GSize width = mainContainer.getWidth();
        GSize height = mainContainer.getHeight();

        Pair<Integer, Integer> extraOffset = setPreferredSize(true, width, height, maxWidth, maxHeight);
        try {
            DataGrid.flushUpdateDOM(); // there can be some pending grid changes, and we need actual sizes

            GSize offsetWidth = GwtClientUtils.getOffsetWidth(element);
            GSize offsetHeight = GwtClientUtils.getOffsetHeight(element);
            if(width == null)
                offsetWidth = offsetWidth.add(extraOffset.first);
            if(height == null)
                offsetHeight = offsetHeight.add(extraOffset.second);
            return new Dimension(offsetWidth, offsetHeight);
        } finally {
            setPreferredSize(false, null, null, GSize.ZERO, GSize.ZERO);
        }
    }

    public Pair<Integer, Integer> setPreferredSize(boolean set, GSize width, GSize height, GSize maxWidth, GSize maxHeight) {
        assert autoSize;
//        GwtClientUtils.changePercentFillWidget(main, set);

        Element element = main.getElement();
        FlexPanel.setPrefHeight(element, height);
        FlexPanel.setPrefWidth(element, width);

        Result<Integer> grids = new Result<>(0);
        if(main instanceof HasMaxPreferredSize)
            ((HasMaxPreferredSize) main).setPreferredSize(set, grids);

        element = getElement();
        if(set) {
            // there are 2 problems : rounding (we need to round up), however it coukd be fixed differently
            // since we are changing for example grid basises (by changing fill to percent), we can get extra scrollbars in grids (which is not what we want), so we just add some extraOffset
            int extraHorzOffset = DataGrid.nativeScrollbarWidth * grids.result + 1; // 1 is for rounding
            int extraVertOffset = DataGrid.nativeScrollbarHeight * grids.result + 1; // 1 is for rounding

            FlexPanel.setMaxPrefWidth(element, maxWidth);
            FlexPanel.setMaxPrefHeight(element, maxHeight);

            return new Pair<>(extraHorzOffset, extraVertOffset);
        } else {
            FlexPanel.setMaxPrefWidth(element, (GSize) null);
            FlexPanel.setMaxPrefHeight(element, (GSize) null);
            return null;
        }
    }
}