package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GFixedSize;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LinearContainerView;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public class GFormLayout extends ResizableComplexPanel {

    private final GFormController form;

    private final GContainer mainContainer;

    private final NativeSIDMap<GContainer, GAbstractContainerView> containerViews = new NativeSIDMap<>();
    private final NativeSIDMap<GContainer, Widget> containerCaptions = new NativeSIDMap<>();
    private final Map<GComponent, Widget> baseComponentViews = new HashMap<>();

    private final ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private final ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public final ResizableComplexPanel attachContainer;

    public GFormLayout(GFormController iform, GContainer mainContainer) {
        this.form = iform;
        this.mainContainer = mainContainer;

        attachContainer = new ResizableComplexPanel();
        attachContainer.setVisible(false);
        addContainers(mainContainer);

        Widget view = getMainView();
        setPercentMain(view);

        // this is shrinked container and needs scrolling / padding
        FlexPanel.registerContentScrolledEvent(view);

        add(attachContainer);

        addStyleName("form");

        DataGrid.initSinkMouseEvents(this);
    }

    public void addTooltip(Widget header, GContainer container) {
        boolean isMain = container.main;
        TooltipManager.initTooltip(header, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip(String dynamicTooltip) {
                return nvl(dynamicTooltip, isMain ? form.form.getTooltip() : container.getTooltip());
            }

            @Override
            public String getPath() {
                return isMain ? form.form.getPath() : container.getPath();
            }

            @Override
            public String getCreationPath() {
                return isMain ? form.form.getCreationPath() : container.getCreationPath();
            }
        });
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
        Element target = DataGrid.getBrowserTargetAndCheck(getElement(), event);
        if(target == null)
            return;
        if(!form.previewEvent(target, event))
            return;

        super.onBrowserEvent(event);

        form.checkGlobalMouseEvent(event);
    }

    @Override
    public void onResize() {
        if (form.isActive()) {
            super.onResize();
        }
    }


    public static Widget createContainerCaptionWidget(GFormController form, GContainer parentContainer, boolean isPopup, boolean hasBorder) {
        if (parentContainer != null && parentContainer.tabbed) {
            return createTabCaptionWidget();
        } else {
            if (isPopup) {
                return new PopupButton(form);
            } else if (hasBorder) {
                return MainFrame.useBootstrap ? new SimpleWidget("h6") : new LabelWidget();
            }
        }
        return null;
    }

    public static Widget createTabCaptionWidget() {
        return createLabelCaptionWidget();
    }

    public static Widget createLabelCaptionWidget() {
        return new LabelWidget();
    }

    public static Widget createModalWindowCaptionWidget() {
        return new SimpleWidget("h5");
    }

    // creating containers (all other components are created when creating controllers)
    private void addContainers(GContainer container) {
        GAbstractContainerView containerView = createContainerView(form, container);

        containerViews.put(container, containerView);

        Widget captionWidget;
        boolean alreadyInitialized = false;
        if(container.main) {
            Pair<Widget, Boolean> formCaptionWidgetAsync = form.getCaptionWidget();

            captionWidget = formCaptionWidgetAsync.first;
            alreadyInitialized = formCaptionWidgetAsync.second;
        } else
            captionWidget = createContainerCaptionWidget(form, container.container,
                    container.popup, container.caption != null || container.collapsible);

        if (captionWidget != null) {
            updateComponentClass(container.captionClass, captionWidget, "caption");

            addTooltip(captionWidget, container);

            String caption = container.caption;
            BaseImage image = container.image;
            if(alreadyInitialized) {
                BaseImage.updateText(captionWidget, caption);
                BaseImage.updateImage(image, captionWidget);
            } else
                BaseImage.initImageText(captionWidget, caption, image, ImageHtmlOrTextType.CONTAINER);

            containerCaptions.put(container, captionWidget);
        }

        Widget viewWidget = containerView.getView();
        add(container, new ComponentWidget(viewWidget, captionWidget), null);

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

    public void setElementClass(GComponent component, String elementClass) {
        component.elementClass = elementClass;

        Widget widget = containerViews.get(component.container).getChildView(component);
//        if(widget != null) {
            updateComponentClass(elementClass, widget, BaseImage.emptyPostfix);
//        }
    }

    public void setCaptionClass(GContainer component, String elementClass) {
        component.captionClass = elementClass;

        Widget widget = containerViews.get(component.container).getCaptionView(component);
//        if(widget != null) {
            updateComponentClass(elementClass, widget, "caption");
//        }
    }

    public void setValueClass(GContainer component, String valueClass) {
        component.valueClass = valueClass;

//        Widget widget = component instanceof GContainer ? containerViews.get((GContainer) component).getView() : baseComponentViews.get(component);
        Widget widget = containerViews.get(component).getView();
//        if(widget != null) {
            updateComponentClass(valueClass, widget, "value");
//        }
    }

    public static void updateComponentClass(String elementClass, Widget widget, String postfix) {
        BaseImage.updateClasses(widget.getElement(), elementClass, postfix);
    }

    public void add(GComponent key, ComponentWidget view, DefaultFocusReceiver focusReceiver) {
        // debug info
        if (key.sID != null)
            view.getWidget().getElement().setAttribute("lsfusion-container", key.sID);

        GAbstractContainerView containerView;
        if(key.container != null && (containerView = containerViews.get(key.container)) != null) { // container can be null when component should be layouted manually, containerView can be null when it is removed 
            containerView.add(key, view, attachContainer);

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

    public Widget getContainerCaption(GContainer container) {
        return containerCaptions.get(container);
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

        GSize customWidth = mainContainer.width >= 0 ? new GFixedSize(mainContainer.width, GFixedSize.Type.PX) : null;
        GSize customHeight = mainContainer.height >= 0 ? new GFixedSize(mainContainer.height, GFixedSize.Type.PX) : null;

        boolean fixWidthOnInit = mainContainer.width == -3;
        boolean fixHeightOnInit = mainContainer.height == -3;
        if(!fixWidthOnInit && !fixHeightOnInit) {
            return new Dimension(customWidth, customHeight); //optimisation
        }
        Pair<Integer, Integer> extraOffset = setPreferredSize(true, width, height, maxWidth, maxHeight);
        try {
            DataGrid.flushUpdateDOM(); // there can be some pending grid changes, and we need actual sizes

            GSize offsetWidth = null;
            if (fixWidthOnInit) {
                offsetWidth = GwtClientUtils.getOffsetWidth(element);
                if (width == null)
                    offsetWidth = offsetWidth.add(extraOffset.first);
            } else {
                offsetWidth = customWidth;
            }

            GSize offsetHeight = null;
            if (fixHeightOnInit) {
                offsetHeight = GwtClientUtils.getOffsetHeight(element);
                if (height == null)
                    offsetHeight = offsetHeight.add(extraOffset.second);
            } else {
                offsetHeight = customHeight;
            }

            return new Dimension(offsetWidth, offsetHeight);
        } finally {
            setPreferredSize(false, null, null, GSize.ZERO, GSize.ZERO);
        }
    }

    public Pair<Integer, Integer> setPreferredSize(boolean set, GSize width, GSize height, GSize maxWidth, GSize maxHeight) {
        Widget main = getMainView();
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