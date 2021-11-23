package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.HasMaxPreferredSize;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.*;
import lsfusion.gwt.client.form.object.table.grid.GGrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GFormLayout extends ResizableComplexPanel {

    private GFormController form;

    private GContainer mainContainer;

    private Map<GContainer, GAbstractContainerView> containerViews = new HashMap<>();
    private Map<GComponent, Widget> baseComponentViews = new HashMap<>();

    private ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public final ResizableComplexPanel recordViews;

    public GFormLayout(GFormController iform, GContainer mainContainer) {
        this.form = iform;

        this.mainContainer = mainContainer;

        addContainers(mainContainer);

        Widget view = getMainView();
        setFillMain(view);
        view.getElement().getStyle().setOverflow(Style.Overflow.AUTO);

        recordViews = new ResizableComplexPanel();
        recordViews.setVisible(false);
        add(recordViews);

        DataGrid.initSinkMouseEvents(this);
    }

    public FormsController getFormsController() {
        return form.getFormsController();
    }

    public Widget getMainView() {
        return getContainerView(mainContainer).getView();
    }

    private static GAbstractContainerView createContainerView(GFormController form, GContainer container) {
        if (container.tabbed) {
            return new TabbedContainerView(form, container);
        } else {
            return new LinearContainerView(container);
        }
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
    private GAbstractContainerView addContainers(GContainer container) {
        GAbstractContainerView containerView = createContainerView(form, container);

        containerViews.put(container, containerView);
        Widget viewWidget = containerView.getView();
        add(container, viewWidget, null);

        // debug info
        viewWidget.getElement().setAttribute("lsfusion-container-type", container.getContainerType());

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
        // we wish that all base components margins, paddings and borders should be zero (since we're setting them with topBorder and others)
        // but it seems for now it's not always like that, however later it can be refactored
//        assert GwtClientUtils.getAllMargins(view.getElement()) == 0;
        assert !(component instanceof GContainer);
        baseComponentViews.put(component, view);
        add(component, view, focusReceiver);
    }

    public void add(GComponent key, Widget view, DefaultFocusReceiver focusReceiver) {
        // debug info
        if (key.sID != null)
            view.getElement().setAttribute("lsfusion-container", key.sID);

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

    public void hideEmptyContainerViews(int requestIndex) {
        autoShowHideContainers(mainContainer, requestIndex);

        FlexPanel.autoStretchAndDrawBorders(getMainView());
    }

    private boolean autoShowHideContainers(GContainer container, long requestIndex) {
        GAbstractContainerView containerView = getContainerView(container);
        boolean hasVisible = false;
        int size = containerView.getChildrenCount();
        boolean[] childrenVisible = new boolean[size];
        for (int i = 0; i < size; ++i) {
            GComponent child = containerView.getChild(i);

            boolean childVisible;
            if (child instanceof GContainer)
                childVisible = autoShowHideContainers((GContainer) child, requestIndex);
            else {
                Widget childView = baseComponentViews.get(child); // we have to use baseComponentView (and not a wrapper in getChildView), since it has relevant visible state
                childVisible = childView != null && childView.isVisible();

                if (child instanceof GGrid) {
                    GContainer record = ((GGrid) child).record;
                    if(record != null)
                        autoShowHideContainers(record, requestIndex);
                }
            }

            childrenVisible[i] = childVisible;
            hasVisible = hasVisible || childVisible;
        }
        containerView.updateLayout(requestIndex, childrenVisible);
        return hasVisible;
    }

    public Dimension getPreferredSize(int maxWidth, int maxHeight, int extraHorzOffset, int extraVertOffset) {
        Integer width = mainContainer.getSize(false);
        Integer height = mainContainer.getSize(true);

        setPreferredSize(true, width, height, maxWidth - extraHorzOffset, maxHeight - extraVertOffset);
        try {
            DataGrid.flushUpdateDOM(); // there can be some pending grid changes, and we need actual sizes

            int offsetWidth = getOffsetWidth();
            int offsetHeight = getOffsetHeight();
            return new Dimension(offsetWidth + (width != null ? 0 : extraHorzOffset), offsetHeight + (height != null ? 0 : extraVertOffset));
        } finally {
            setPreferredSize(false, null, null, 0, 0);
        }
    }

    public void setPreferredSize(boolean set, Integer width, Integer height, int maxWidth, int maxHeight) {
        GwtClientUtils.changePercentFillWidget(main, set);

        Element element = main.getElement();
        FlexPanel.setSize(element, true, height);
        FlexPanel.setSize(element, false, width);

        if(set) {
            DataGrid.setMaxWidth(this, maxWidth, Style.Unit.PX);
            DataGrid.setMaxHeight(this, maxHeight, Style.Unit.PX);
        } else {
            DataGrid.clearMaxWidth(this);
            DataGrid.clearMaxHeight(this);
        }

        if(main instanceof HasMaxPreferredSize)
            ((HasMaxPreferredSize) main).setPreferredSize(set);
    }
}