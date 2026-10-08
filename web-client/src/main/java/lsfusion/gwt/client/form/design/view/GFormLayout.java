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
import lsfusion.gwt.client.form.object.table.tree.GTreeGroup;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public class GFormLayout extends SizedFlexPanel {

    private final GFormController form;

    private final GContainer mainContainer;

    private final NativeSIDMap<GContainer, GAbstractContainerView> containerViews = new NativeSIDMap<>();
    private final NativeSIDMap<GContainer, Widget> containerCaptions = new NativeSIDMap<>();
    // the containers whose SHOWIF hides them now: a container is hidden through what it holds, except a CUSTOM REACT
    // one, whose content GWT does not see - it is told its own SHOWIF instead (FormView.isReactShowIfContainer)
    private final java.util.Set<GContainer> showIfHidden = new java.util.HashSet<>();
    private final Map<GComponent, ComponentViewWidget> baseComponentViews = new HashMap<>();

    private final ArrayList<GComponent> defaultComponents = new ArrayList<>();
    private final ArrayList<DefaultFocusReceiver> defaultFocusReceivers = new ArrayList<>();

    public final ResizableComplexPanel attachContainer;

    public GFormLayout(GFormController iform, GContainer mainContainer) {
        super(true);

        this.form = iform;
        this.form.formLayout = this; //Because formController.getFormLayout() is used inside TemplateContainerView.addImpl before formController is fully initialized, formLayout may be null.
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
        if (container.isReact()) // CUSTOM REACT 'fn': React owns the subtree (children not laid out by GWT)
            return new ReactContainerView(form, container);
        else if (container.tabbed)
            return new TabbedContainerView(form, container);
        else if (container.isCustom())
            return new TemplateContainerView(form, container);
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

    // a form that is shown gets its size, whether or not it holds the keyboard: with more than one forms window a form
    // is drawn beside the one the user works in, and its grids need the room they have. A form no one sees - a tab
    // behind another - is left alone, as before
    @Override
    public void onResize() {
        if (form.isActive() || GwtClientUtils.isShowing(this)) {
            super.onResize();
        }
    }


    public static Widget createContainerCaptionWidget(GFormController form, GContainer parentContainer, boolean isPopup, boolean hasBorder) {
        // when the parent view draws its children's captions itself, the child gets no caption widget of its own: a
        // tabbed parent draws it in the tab strip, a react parent's component draws it from the child's descriptor in
        // `data` (only an lsf child reaches here under a react parent — see addContainers). This is also why an lsf
        // child never gets a CollapsiblePanel: that is built for a LayoutContainerView's flex children, and neither the
        // tab strip nor the parked react subtree is one.
        if (parentContainer != null && parentContainer.tabbed) {
            return createTabCaptionWidget();
        } else if (parentContainer != null && parentContainer.isReact()) {
            return null;
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
            // none for an lsf container a React view places, however deep: its caption is drawn by that component from
            // its entry in data, exactly as a tabbed parent draws a child's caption in the strip
            captionWidget = container.isLsfView() && container.getReactPlace() != null ? null : createContainerCaptionWidget(form, container.container,
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

            // debug info, the tab header (nav-link) is the caption widget itself
            if(container.container != null && container.container.tabbed && container.sID != null)
                captionWidget.getElement().setAttribute("lsfusion-tab", container.sID);

            containerCaptions.put(container, captionWidget);
        }

        Widget viewWidget = containerView.getView();
        add(container, new ComponentWidget(viewWidget, captionWidget != null ? new CaptionWidget(captionWidget, container.captionAlignmentHorz, container.captionAlignmentVert) : null), null);

        // debug info
        viewWidget.getElement().setAttribute("lsfusion-container-type", container.getContainerType());

        for (GComponent child : container.children) {
            if (child.isReactDrawn()) { // React draws this child (from data): no GWT view of it - only of the lsf
                addPlacedContainers(child);  // containers inside it, which this react container places
                continue;
            }
            if(child instanceof GGrid)
                child = ((GGrid)child).record;
            if (child instanceof GContainer) {
                addContainers((GContainer) child);
            }
        }
    }
    // inside a component React draws itself - a container's children, a grid's record (it is where its grid is): an lsf
    // container gets its view - its parent has none, so add hands the view to the react container that places it - and
    // is built on as any other, as is an lsf grid's record; anything else is walked for more of them. An lsf base
    // component's view comes from its controller, and reaches that react container the same way
    private void addPlacedContainers(GComponent drawn) {
        for (GComponent child : drawn.getChildren())
            addPlacedContainer(child);
    }
    private void addPlacedContainer(GComponent component) {
        if (!component.isLsfView())
            addPlacedContainers(component);
        else {
            if (component instanceof GGrid)
                component = ((GGrid) component).record;
            if (component instanceof GContainer)
                addContainers((GContainer) component);
        }
    }

    // the container view that holds a component's view: its container's - or, for a component inside a container React
    // draws itself, which has no view, the view of the react container that places it
    private GAbstractContainerView getHoldingView(GComponent component) {
        GAbstractContainerView view = containerViews.get(component.container);
        if (view == null) {
            GContainer place = component.getReactPlace();
            if (place != null)
                view = containerViews.get(place);
        }
        return view;
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
        // hidden through what it holds, or - a react container - by this, on the next update
        if (component instanceof GContainer) {
            if (visible)
                showIfHidden.remove(component);
            else
                showIfHidden.add((GContainer) component);
        }
        ComponentViewWidget widget = baseComponentViews.get(component);
        if(widget != null) {
            widget.setShowIfVisible(visible);
        }
    }

    public void setElementClass(GComponent component, String elementClass) {
        component.elementClass = elementClass;

        if (component.container == null) // the main container is placed by no container view
            return;
        GAbstractContainerView holding = getHoldingView(component);
        Widget widget = holding != null ? holding.getChildWidget(component) : null;
        if(widget != null) // if the component is a base component it can be hidden, but the class can be changed anyway (however elementClass will be changed and it will be used when adding view)
            updateComponentClass(elementClass, widget, BaseImage.emptyPostfix);
        else
            assert !(component instanceof GContainer);
    }

    public void setCaptionClass(GContainer component, String elementClass) {
        component.captionClass = elementClass;
        // on the caption widget the container was given - none for an lsf container a React view places, whose caption
        // React draws from its entry; the main container's caption is the form's
        Widget caption = component.container != null ? getContainerCaption(component) : null;
        if (caption != null)
            updateComponentClass(elementClass, caption, "caption");
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

    public void setHierarchicalCaption(GTreeGroup component, String hierarchicalCaption) {
        component.hierarchicalCaption = hierarchicalCaption;

        TableContainer tableContainer = (TableContainer)baseComponentViews.get(component).getSingleWidget().widget;
        tableContainer.updateHierarchicalCaption(component);
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
        // container can be null when component should be layouted manually, containerView can be null when it is
        // removed
        if(key.container != null && (containerView = getHoldingView(key)) != null) {
            reportUnplaceable(key, containerView);
            containerView.add(key, view, attachContainer);

            maybeAddDefaultFocusReceiver(key, focusReceiver);
        }
    }

    // the single funnel every built view goes through, and the one place that sees BOTH that a view was built and
    // where it landed. A react container draws its children from `data` and parks everything else until an <Lsf>
    // places it; a child with no `lsf = TRUE` that nevertheless has a view of its own - a group's toolbar moved in,
    // say - is drawn by nobody: React has at most what labels it, and the park is not a place. Marking
    // it `lsf = TRUE` is what the author means, and then the view names it in an <Lsf>. A component the design does not
    // name - a user filter's - is placed with the named container that holds it. A group's filter box moved in is not
    // such a child: the platform builds no panel for it (GAbstractTableController.initFilters), and its conditions are
    // the view's
    private void reportUnplaceable(GComponent key, GAbstractContainerView containerView) {
        if (!(containerView instanceof ReactContainerView) || key.isLsfView() || !unplaceable.add(key))
            return;
        String container = ((ReactContainerView) containerView).getContainer().sID;
        GwtClientUtils.logLsfViewError(key.sID != null
                ? "'" + key.sID + "' has a view of its own inside the react container '" + container + "', which draws"
                  + " its children from `data` and has nothing there to draw it with, so it is shown by nobody; mark it"
                  + " `lsf = TRUE` and place it with <Lsf name=\"" + key.sID + "\"/>"
                : "a component with no name - a user filter's, say - has a view of its own inside the react container '"
                  + container + "' and is shown by nobody; mark the named container that holds it `lsf = TRUE` and"
                  + " place that with <Lsf name/>");
    }
    private final Set<GComponent> unplaceable = new HashSet<>(); // said once per component

    public void remove(GComponent key) {
        assert !(key instanceof GContainer);
        GAbstractContainerView containerView;
        if (key.container != null && (containerView = getHoldingView(key)) != null) { // see add method
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
        // a container that draws its own content - a React component, or an HTML template - has GWT child views only for
        // the children it places, so hasVisible says nothing about what it renders; it must not be collapsed to
        // display:none by its parent (which used to leave a template-only container invisible, and its window collapsed)
        // - unless its SHOWIF hides it, which a CUSTOM REACT container is told as any component is
        return hasVisible || (container.isCustomDrawn() && !showIfHidden.contains(container));
    }

    // the window is measured ONCE when it is shown and fixed to that size (initPreferredSize below); -1 stays dynamic
    // and follows its content, so only this case needs the content to be drawn before the measurement
    public boolean isFixedSizeOnInit() {
        return mainContainer.width == -3 || mainContainer.height == -3;
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

            // in theory fixFlexBasis could be used, but it's not clear what to do with the opposite direction, since it requires DOM change (to make resize of the modal form work in that direction)
            removeSized(main);
            addMainView(main, fixedWidth, fixedHeight);
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
