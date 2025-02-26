package lsfusion.gwt.client.form.design.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.focus.DefaultFocusReceiver;
import lsfusion.gwt.client.base.jsni.NativeSIDMap;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LinearContainerView;
import lsfusion.gwt.client.form.object.table.TableContainer;
import lsfusion.gwt.client.form.object.table.grid.GGrid;
import lsfusion.gwt.client.form.object.table.grid.GGridProperty;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static lsfusion.gwt.client.base.GwtClientUtils.IGNORE_DESTROY;
import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public class GFormLayout extends SizedFlexPanel {

    private final GFormController form;

    private final GContainer mainContainer;

    private final NativeSIDMap<GContainer, GAbstractContainerView> containerViews = new NativeSIDMap<>();
    private final NativeSIDMap<GContainer, Widget> containerCaptions = new NativeSIDMap<>();
    private final Map<GComponent, ComponentViewWidget> baseComponentViews = new HashMap<>();

    private final ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private final ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public final ResizableComplexPanel attachContainer;

    public GFormLayout(GFormController iform, GContainer mainContainer) {
        super(true);

        this.form = iform;
        this.mainContainer = mainContainer;

        attachContainer = new ResizableComplexPanel();
        attachContainer.setVisible(false);
        addContainers(mainContainer);

        Widget view = getMainView();
        addMainView(view, null, null);
        GAbstractContainerView.setupOverflow(mainContainer, view, isVertical(), false, mainContainer.width == -3 ? GSize.CONST(1) : null, mainContainer.height == -3 ? GSize.CONST(1) : null);

        // this is shrinked container and needs scrolling / padding
        FlexPanel.registerContentScrolledEvent(this);

        add(attachContainer);

        GwtClientUtils.addClassName(this, "form");

        DataGrid.initSinkMouseEvents(this);
    }

    private void addMainView(Widget view, GSize width, GSize height) {
        GContainer mainContainer = this.mainContainer;
        new SizedWidget(view, width != null ? width : mainContainer.getWidth(), height != null ? height : mainContainer.getHeight()).add(this, 0, mainContainer.getFlex(RendererType.PANEL), mainContainer.isShrink(), mainContainer.getAlignment(), mainContainer.isAlignShrink());
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
        if(form.getTargetAndPreview(getElement(), event) == null)
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
        add(container, new ComponentWidget(viewWidget, captionWidget != null ? new CaptionWidget(captionWidget, container.captionAlignmentHorz, container.captionAlignmentVert) : null), null);

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
        baseComponentViews.put(component, view.widget);
        add(component, view, focusReceiver);
    }

    public void setShowIfVisible(GComponent component, boolean visible) {
        ComponentViewWidget widget = baseComponentViews.get(component);
        if(widget != null) {
            widget.setShowIfVisible(visible);
        }
    }

    public void setElementClass(GComponent component, String elementClass) {
        component.elementClass = elementClass;

        Widget widget = containerViews.get(component.container).getChildWidget(component);
        if(widget != null) // if the component is a base component it can be hidden, but the class can be changed anyway (however elementClass will be changed and it will be used when adding view)
            updateComponentClass(elementClass, widget, BaseImage.emptyPostfix);
        else
            assert !(component instanceof GContainer);
    }

    public void setCaptionClass(GContainer component, String elementClass) {
        component.captionClass = elementClass;

        updateComponentClass(elementClass, containerViews.get(component.container).getCaptionView(component), "caption");
    }

    public void setValueClass(GContainer component, String valueClass) {
        component.valueClass = valueClass;

//        Widget widget = component instanceof GContainer ? containerViews.get((GContainer) component).getView() : baseComponentViews.get(component);
        updateComponentClass(valueClass, containerViews.get(component).getView(), "value");
    }

    public void setValueClass(GGridProperty component, String valueClass) {
        component.valueClass = valueClass;

        TableContainer tableContainer = (TableContainer)baseComponentViews.get(component).getSingleWidget().widget;
        tableContainer.updateElementClass(component);
    }

    public static void updateComponentClass(String elementClass, Widget widget, String postfix) {
        BaseImage.updateClasses(widget.getElement(), elementClass, postfix);
    }

    public static void setDebugInfo(Widget widget, String debugInfo) {
        widget.getElement().setAttribute("lsfusion-container", debugInfo);
    }

    public void add(GComponent key, ComponentWidget view, DefaultFocusReceiver focusReceiver) {
        // debug info
        if (key.sID != null)
            view.widget.setDebugInfo(key.sID);

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
                ComponentViewWidget childView = baseComponentViews.get(child); // we have to use baseComponentView (and not a wrapper in getChildView), since it has relevant visible state
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

    public void initPreferredSize(Widget maxWindow, GSize maxWidth, GSize maxHeight) {
        Widget main = getMainView();
        Element element = main.getElement();

        boolean fixWidthOnInit = mainContainer.width == -3;
        boolean fixHeightOnInit = mainContainer.height == -3;
        if(!fixWidthOnInit && !fixHeightOnInit) //optimisation
            return;

        Result<Integer> grids = new Result<>(0);
        if(main instanceof HasMaxPreferredSize)
            ((HasMaxPreferredSize) main).setPreferredSize(true, grids);

        // there are 2 problems : rounding (we need to round up), however it coukd be fixed differently
        // since we are changing for example grid basises (by changing fill to percent), we can get extra scrollbars in grids (which is not what we want), so we just add some extraOffset
        // 1 is for rounding
        GSize extraWidth = GSize.CONST(DataGrid.nativeScrollbarWidth * grids.result + 1);
        GSize extraHeight = GSize.CONST(DataGrid.nativeScrollbarHeight * grids.result + 1);

        Element maxWindowElement = maxWindow.getElement();
        FlexPanel.setMaxPrefWidth(maxWindowElement, maxWidth.subtract(extraWidth));
        FlexPanel.setMaxPrefHeight(maxWindowElement, maxHeight.subtract(extraHeight));

        try {
            DataGrid.flushUpdateDOM(); // there can be some pending grid changes, and we need actual sizes

            GSize fixedWidth = fixWidthOnInit ? GwtClientUtils.getOffsetWidth(element).add(extraWidth) : null;
            GSize fixedHeight = fixHeightOnInit ? GwtClientUtils.getOffsetHeight(element).add(extraHeight) : null;

            main.getElement().setPropertyBoolean(IGNORE_DESTROY, true);
            try {
                // in theory fixFlexBasis could be used, but it's not clear what to do with the opposite direction, since it requires DOM change (to make resize of the modal form work in that direction)
                removeSized(main);
                addMainView(main, fixedWidth, fixedHeight);
            } finally {
                main.getElement().setPropertyBoolean(IGNORE_DESTROY, false);
            }
        } finally {
            FlexPanel.setMaxPrefWidth(maxWindowElement, (GSize) null);
            FlexPanel.setMaxPrefHeight(maxWindowElement, (GSize) null);

            if(main instanceof HasMaxPreferredSize)
                ((HasMaxPreferredSize) main).setPreferredSize(false, grids);
        }
    }

    public Map<GComponent, ComponentViewWidget> getBaseComponentViews() {
        return baseComponentViews;
    }
}