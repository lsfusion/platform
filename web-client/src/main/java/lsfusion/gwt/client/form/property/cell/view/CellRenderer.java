package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.ColorUtils;
import lsfusion.gwt.client.base.view.SizedFlexPanel;
import lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.form.property.cell.classes.view.InputBasedCellRenderer;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.GwtClientUtils.nvl;

public abstract class CellRenderer {

    protected final GPropertyDraw property;

    TooltipManager.TooltipHelper valueTooltipHelper;

    public CellRenderer(GPropertyDraw property) {
        this.property = property;

        if(property.valueTooltip != null) {
            valueTooltipHelper = new TooltipManager.TooltipHelper() {
                @Override
                public String getTooltip(String dynamicTooltip) {
                    return nvl(dynamicTooltip, property.valueTooltip);
                }
            };
        }

    }

    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererEmpty();
    protected final String NOT_DEFINED_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    private final static String focusElementProp = "focusElement";
    public static Object getFocusElement(Element parent) {
        return parent.getPropertyObject(focusElementProp);
    }
    public static void setFocusElement(Element parent, Element element) {
        parent.setPropertyObject(focusElementProp, element == null ? NULL : element);
    }
    public static void clearFocusElement(Element parent) {
        parent.setPropertyObject(focusElementProp, null);
    }
    private final static String readonlyFncProp = "readonlyFnc";
    public final static String NULL = "NULL"; // none / manual
    public static Object getReadonlyFnc(Element parent) {
        return parent.getPropertyObject(readonlyFncProp);
    }
    // null means that readonly will be set "manually"
    public static void setReadonlyFnc(Element parent, JavaScriptObject fnc) {
        parent.setPropertyObject(readonlyFncProp, fnc == null ? NULL : fnc);
    }
    public static void clearReadonlyFnc(Element parent) {
        parent.setPropertyObject(readonlyFncProp, null);
    }

    protected boolean isTagInput() {
        return property.isTagInput();
    }

    protected String getTag() {
        return property.tag;
    }

    public Element createRenderElement(RendererType rendererType) {
        assert !isTagInput();

        Element renderElement;

        String tag = getTag();
        if(tag != null)
            renderElement = GwtClientUtils.createFocusElementType(tag);
        else
            renderElement = Document.get().createDivElement();

        return renderElement;
    }
    public boolean canBeRenderedInTD() {
        return false;
    }

    // should be consistent with getWidthPadding and getHeightPadding
    // and with TextBasedCellEditor.renderStaticContent
    public void render(Element element, RenderContext renderContext) {
        boolean renderedAlignment = renderContent(element, renderContext);

//        BaseImage.setClasses(element, getValueElementClass());

//        SimpleTextBasedCellRenderer.getSizeElement(element).addClassName("prop-value-shrink");

        if(!renderedAlignment) {
            assert !GwtClientUtils.isTDorTH(element) && !InputBasedCellRenderer.isToolbarContainer(element);
            renderFlexAlignment(property, element, renderContext.getRendererType());
        }
    }

    public void renderPanelLabel(Widget label) {
    }

    public void renderPanelContainer(SizedFlexPanel panel) {
//        was removed in bootstrap 5
//        panel.addStyleName("form-group");
    }

    public static void setBasedTextFonts(GPropertyDraw property, Element element, RenderContext renderContext) {
        GFont font = property.font != null ? property.font : renderContext.getFont();

        if (font != null) {
            font.apply(element.getStyle());
        }
    }

    public static void clearBasedTextFonts(GPropertyDraw property, Element element, RenderContext renderContext) {
        GFont font = property.font != null ? property.font : renderContext.getFont();

        if (font != null) {
            font.clear(element.getStyle());
        }
    }

    public static void renderEditSelected(Element element, GPropertyDraw property) {
        if(property.hasEditObjectAction)
            element.addClassName("selectedCellHasEdit");
    }
    public static void clearEditSelected(Element element, GPropertyDraw property) {
        if(property.hasEditObjectAction)
            element.removeClassName("selectedCellHasEdit");
    }

    private static void renderFlexAlignment(GPropertyDraw property, Element element, RendererType rendererType) {
        element.addClassName("prop-display-flex");

        Style.TextAlign horzTextAlignment = property.getHorzTextAlignment(rendererType);
        switch(horzTextAlignment) {
            case LEFT:
                element.addClassName("prop-flex-horz-start");
                break;
            case CENTER:
                element.addClassName("prop-flex-horz-center");
                break;
            case RIGHT:
                element.addClassName("prop-flex-horz-end");
                break;
        }

        String vertAlignment = property.getVertTextAlignment(false, rendererType); // here we don't care about baseline / center
        switch (vertAlignment) {
            case "top":
                element.addClassName("prop-flex-vert-start");
                break;
            case "baseline":
            case "center":
                element.addClassName("prop-flex-vert-center");
                break;
            case "stretch":
                element.addClassName("prop-flex-vert-stretch");
                break;
            case "bottom":
                element.addClassName("prop-flex-vert-end");
                break;
        }
    }

    public static void renderTextAlignment(GPropertyDraw property, Element element, boolean isInput, RendererType rendererType) {
//        assert GwtClientUtils.isTDorTH(element) || GwtClientUtils.isInput(element);
        assert isInput == GwtClientUtils.isInput(element);

        Style.TextAlign horzTextAlignment = property.getHorzTextAlignment(rendererType);
        switch(horzTextAlignment) {
            case LEFT:
                element.addClassName("prop-text-horz-start");
                break;
            case CENTER:
                element.addClassName("prop-text-horz-center");
                break;
            case RIGHT:
                element.addClassName("prop-text-horz-end");
                break;
        }

        String vertAlignment = property.getVertTextAlignment(isInput, rendererType);
        switch (vertAlignment) {
            case "top":
                element.addClassName("prop-text-vert-start");
                break;
            case "center":
            case "stretch":
                element.addClassName("prop-text-vert-center");
                break;
            case "baseline":
                element.addClassName("prop-text-vert-baseline");
                break;
            case "bottom":
                element.addClassName("prop-text-vert-end");
                break;
        }
    }

    public void clearRender(Element element, RenderContext renderContext) {
        GwtClientUtils.removeAllChildren(element);

        boolean renderedAlignment = clearRenderContent(element, renderContext);

//        SimpleTextBasedCellRenderer.getSizeElement(element).removeClassName("prop-value-shrink");

        if (!renderedAlignment)
            clearRenderFlexAlignment(property, element, renderContext.getRendererType());

        // update
        AbstractDataGridBuilder.clearColors(element);

        clearEditSelected(element, property);

        if(needToRenderToolbarContent()) {
            RenderedState renderedState = (RenderedState) element.getPropertyObject(RENDERED);
            if(renderedState.toolbar != null) {
                clearRenderToolbarContent(element);
            }
        }
        element.setPropertyObject(RENDERED, null);
    }
    public static void clearRenderTextAlignment(GPropertyDraw property, Element element, boolean isInput, RendererType rendererType) {
        Style.TextAlign horzTextAlignment = property.getHorzTextAlignment(rendererType);
        switch(horzTextAlignment) {
            case LEFT:
                element.removeClassName("prop-text-horz-start");
                break;
            case CENTER:
                element.removeClassName("prop-text-horz-center");
                break;
            case RIGHT:
                element.removeClassName("prop-text-horz-end");
                break;
        }

        String vertAlignment = property.getVertTextAlignment(isInput, rendererType);
        switch (vertAlignment) {
            case "top":
                element.removeClassName("prop-text-vert-start");
                break;
            case "center":
            case "stretch":
                element.removeClassName("prop-text-vert-center");
                break;
            case "baseline":
                element.removeClassName("prop-text-vert-baseline");
                break;
            case "bottom":
                element.removeClassName("prop-text-vert-end");
                break;
        }
    }
    private static void clearRenderFlexAlignment(GPropertyDraw property, Element element, RendererType rendererType) {
        element.removeClassName("prop-display-flex");

        Style.TextAlign horzTextAlignment = property.getHorzTextAlignment(rendererType);
        switch(horzTextAlignment) {
            case LEFT:
                element.removeClassName("prop-flex-horz-start");
                break;
            case CENTER:
                element.removeClassName("prop-flex-horz-center");
                break;
            case RIGHT:
                element.removeClassName("prop-flex-horz-end");
                break;
        }

        String vertAlignment = property.getVertTextAlignment(false, rendererType); // here we don't care about baseline / center
        switch (vertAlignment) {
            case "top":
                element.removeClassName("prop-flex-vert-start");
                break;
            case "baseline":
            case "center":
                element.removeClassName("prop-flex-vert-center");
                break;
            case "stretch":
                element.removeClassName("prop-flex-vert-stretch");
                break;
            case "bottom":
                element.removeClassName("prop-flex-vert-end");
                break;
        }
    }

    protected boolean renderedLoadingContent(UpdateContext updateContext) {
        return false;
    }
    protected Object getExtraValue(UpdateContext updateContext) {
        return null;
    }

    protected boolean needToRenderToolbarContent() {
        return property.toolbar;
    }

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class RenderedState {
        public PValue value;
        public Object extraValue;
        public GColorTheme colorTheme; // for action and color cell renderer

        public String foreground;
        public String background;

        public Boolean readonly;

        public String valueElementClass;

        public String valueTooltip;

        public boolean rerender;

        public ToolbarState toolbar;
    }
    private static boolean equalsDynamicState(RenderedState state, PValue value, Object extraValue, GColorTheme colorTheme) {
        return GwtClientUtils.nullEquals(state.value, value) && GwtClientUtils.nullEquals(state.extraValue, extraValue) && state.colorTheme == colorTheme && !state.rerender;
    }
    private static boolean equalsColorState(RenderedState state, String background, String foreground) {
        return GwtClientUtils.nullEquals(state.background, background) && GwtClientUtils.nullEquals(state.foreground, foreground);
    }
    private static boolean equalsReadonlyState(RenderedState state, Boolean readonly) {
        return GwtClientUtils.nullEquals(state.readonly, readonly);
    }
    private static boolean equalsValueElementClassState(RenderedState state, String elementClass) {
        return GwtClientUtils.nullEquals(state.valueElementClass, elementClass);
    }
    private static boolean equalsValueTooltipState(RenderedState state, String valueTooltip) {
        return GwtClientUtils.nullEquals(state.valueTooltip, valueTooltip);
    }

    private static final String RENDERED = "rendered";

    protected String getBackground(UpdateContext updateContext) {
        return ColorUtils.getThemedColor(updateContext.getBackground());
    }

    protected static void rerenderState(Element element, boolean set) {
        RenderedState renderedState = (RenderedState) element.getPropertyObject(RENDERED);
        if(renderedState != null) // since element can be already dead
            renderedState.rerender = set;
    }
    
    public void update(Element element, UpdateContext updateContext) {
        boolean selected = updateContext.isSelectedLink();
        if(selected)
            renderEditSelected(element, property);
        else
            clearEditSelected(element, property);

        PValue value = updateContext.getValue();
        Object extraValue = getExtraValue(updateContext); // in action we also use isLoading and getImage

        RenderedState renderedState = (RenderedState) element.getPropertyObject(RENDERED);
        boolean isNew = false;
        if(renderedState == null) {
            renderedState = new RenderedState();
            element.setPropertyObject(RENDERED, renderedState);

            isNew = true;
        }

        Boolean readonly = updateContext.isPropertyReadOnly();
        if(isNew || !equalsReadonlyState(renderedState, readonly)) {
            renderedState.readonly = readonly;

            updateReadonly(element, readonly);
        }

        String valueElementClass = updateContext.getValueElementClass();
        if(isNew || !equalsValueElementClassState(renderedState, valueElementClass)) {
            renderedState.valueElementClass = valueElementClass;

            BaseImage.updateClasses(element, valueElementClass);
        }

        if(valueTooltipHelper != null) {
            String valueTooltip = updateContext.getValueTooltip();
            if (isNew || !equalsValueTooltipState(renderedState, valueTooltip)) {
                renderedState.valueTooltip = valueTooltip;
                JavaScriptObject valueTippy;
                if (isNew) {
                    valueTippy = TooltipManager.initTooltip(element, valueTooltipHelper);
                    element.setPropertyObject("valueTippy", valueTippy);
                } else {
                    valueTippy = (JavaScriptObject) element.getPropertyObject("valueTippy");
                }
                TooltipManager.updateContent(valueTippy, element, valueTooltipHelper, valueTooltip);
            }
        }

        // already themed colors expected
        String background = getBackground(updateContext);
        String foreground = ColorUtils.getThemedColor(updateContext.getForeground());
        if(isNew || !equalsColorState(renderedState, background, foreground)) {
            renderedState.background = background;
            renderedState.foreground = foreground;

            AbstractDataGridBuilder.updateColors(element, background, foreground);
        }

        boolean cleared = false;
        if(isNew || !equalsDynamicState(renderedState, value, extraValue, MainFrame.colorTheme)) {
            // there might be stack overflow, if this is done after renderDynamicContent, and this is a custom cell render, which calls changeProperty in its update method
            // setting value earlier breaks the recursion
            renderedState.value = value;
            renderedState.extraValue = extraValue;
            renderedState.colorTheme = MainFrame.colorTheme;
            renderedState.rerender = false;

            cleared = updateContent(element, value, extraValue, updateContext);
        }

        if(needToRenderToolbarContent())
            renderToolbarContent(element, updateContext, renderedState, cleared);
    }

    private void updateReadonly(Element element, Boolean readonly) {
        Object readonlyFnc = getReadonlyFnc(element);
        if(readonlyFnc != null) {
            if(readonlyFnc != CellRenderer.NULL)
                GwtClientUtils.call((JavaScriptObject) readonlyFnc, GSimpleStateTableView.fromObject(readonly));
        } else
            setDefaultReadonlyFnc(element, readonly);
    }

    private static native JavaScriptObject setDefaultReadonlyFnc(Element element, Boolean readonly)/*-{
        $wnd.setDisabledType(element, readonly != null && readonly);
        $wnd.setReadonlyType(element, readonly != null && !readonly);
    }-*/;

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class ToolbarState {
        public final boolean loading;
        public final ToolbarAction[] toolbarActions;

        public Element element;

        public ToolbarState(boolean loading, ToolbarAction[] toolbarActions) {
            this.loading = loading;
            this.toolbarActions = toolbarActions;
        }
    }

    private boolean equalsState(ToolbarState stateA, ToolbarState stateB) {
        if(stateA == null)
            return stateB == null;
        if(stateB == null)
            return false;

        if(!(stateA.loading == stateB.loading))
            return false;

        if(stateA.toolbarActions != stateB.toolbarActions) {
            if (stateA.toolbarActions.length != stateB.toolbarActions.length)
                return false;
            for(int i = 0; i<stateA.toolbarActions.length; i++)
                if(!stateA.toolbarActions[i].matches(stateB.toolbarActions[i]))
                    return false;
        }

        return true;
    }

    public interface ToolbarAction {

        boolean isHover();
        GKeyStroke getKeyStroke();
        BaseStaticImage getImage();

        boolean matches(ToolbarAction action);

        // there are to ways of working with toolbar actions
        // 1 is setting some mark for the target element (s) and then checking it in regular event handler (this  is more flexible, for example in editOnSingleClick scenario, however needs some assertions)
        // 2 setting onMouseDown and stopping propagation (this way the row change won't be handled, when using ALL, and maybe some mor things)
        default void setToolbarAction(Element actionImgElement, Object value) {
            // we're setting TOOLBAR_ACTION for all containers to avoid recursive run in getToolbarAction (optimization)
            actionImgElement.setPropertyObject(GEditBindingMap.TOOLBAR_ACTION, value);
            Element parentElement = actionImgElement.getParentElement();
            parentElement.setPropertyObject(GEditBindingMap.TOOLBAR_ACTION, value);
        }
        default void setToolbarAction(ImageElement actionImgElement, Runnable run) {
            // it has to be mouse down, since all other handlers use mousedown
            GwtClientUtils.setOnMouseDown(actionImgElement.getParentElement(), nativeEvent -> {
                nativeEvent.stopPropagation(); // need to stop, otherwise editing will be started
                nativeEvent.preventDefault(); // preventing default stops button from focusing (this way we make this button unfocusable, which is important since onClick will lead to rerender, removing component and in this case focus will go south)
                run.run();
            });
        }

        void setOnPressed(Element actionImgElement, UpdateContext updateContext);
    }

    public final static GPropertyDraw.QuickAccessAction[] noToolbarActions = new GPropertyDraw.QuickAccessAction[0];
    // cleared - cleared with setInnerText / setInnerHTML
    protected void renderToolbarContent(Element element, UpdateContext updateContext, RenderedState renderedState, boolean cleared) {
        boolean loading = updateContext.isLoading() && !renderedLoadingContent(updateContext);
        ToolbarAction[] toolbarActions = updateContext.getToolbarActions();

        boolean needToolbar = loading || toolbarActions.length > 0;

        ToolbarState toolbarState = needToolbar ? new ToolbarState(loading, toolbarActions) : null;
        ToolbarState prevState = renderedState.toolbar;
        if (equalsState(toolbarState, prevState)) { // already rendered
            if(!(cleared && needToolbar)) // if cleared we still need to rerender the toolbar
                return;

            toolbarState = prevState; // to keep toolbar element
        } else {
            if(prevState != null && toolbarState != null)
                toolbarState.element = prevState.element;

            renderedState.toolbar = toolbarState;
        }

        if(needToolbar) {
            Element toolbarElement = cleared ? null : toolbarState.element;
            boolean start = !property.getHorzTextAlignment(updateContext.getRendererType()).equals(Style.TextAlign.LEFT);
            if(toolbarElement == null) {
                toolbarElement = Document.get().createDivElement();
                toolbarElement.addClassName(start ? "property-toolbar-start" : "property-toolbar-end");
                toolbarElement.addClassName("property-toolbar");
                GToolbarView.styleToolbar(toolbarElement);
                // we need background-inherit for hover components because of transition (toolbar gets all the width immediately which leads to some annoying blinking)
//                toolbarElement.addClassName("background-inherit");
                element.appendChild(toolbarElement);

                GwtClientUtils.setupEdgeStretchParent(toolbarElement, true, start);

                toolbarState.element = toolbarElement;
            } else
                GwtClientUtils.removeAllChildren(toolbarElement);

            if(loading) {
                Element loadingImage = StaticImage.LOADING_IMAGE_PATH.createImage();
                loadingImage.addClassName("property-toolbar-loading");

                //We don't need the background-inherit class because when we use it, the loading spinner has a visible square background
//                loadingImage.addClassName("background-inherit");

                addToToolbar(toolbarElement, start, loadingImage);
            }

            if (toolbarActions.length > 0) {
                Element propertyToolbarItemGroup = null;
                Element verticalSeparator = GwtClientUtils.createVerticalStretchSeparator().getElement();
                verticalSeparator.addClassName("background-inherit");
                if(allHover(toolbarActions)) {
                    propertyToolbarItemGroup = wrapPropertyToolbarItemGroup(null, toolbarElement, verticalSeparator, start);
                } else {
                    addToToolbar(toolbarElement, start, verticalSeparator);
                }

                int hoverCount = 0;
                for (ToolbarAction toolbarAction : toolbarActions) {
                    // there is an assertion that the DOM structure will be exactly like that in setOnPressed / for optimization reasons
                    Element actionDivElement = GwtClientUtils.createFocusElement("button");
                    actionDivElement.addClassName("btn");

                    Element actionImgElement = toolbarAction.getImage().createImage();

                    actionDivElement.appendChild(actionImgElement);
                    actionDivElement.addClassName("property-toolbar-item"); // setting paddings
                    GToolbarView.styleToolbarItem(actionDivElement);
                    actionDivElement.addClassName("background-inherit");

                    toolbarAction.setOnPressed(actionImgElement, updateContext);

                    GKeyStroke keyStroke = toolbarAction.getKeyStroke();
                    actionDivElement.setTitle(keyStroke != null ? keyStroke.toString() : "");

                    if (toolbarAction.isHover()) {
                        propertyToolbarItemGroup = wrapPropertyToolbarItemGroup(propertyToolbarItemGroup, toolbarElement, actionDivElement, start);
                        hoverCount++;
                    } else {
                        propertyToolbarItemGroup = null;
                        addToToolbar(toolbarElement, start, actionDivElement);
                    }
                }

                if (hoverCount > 0) {
                    element.addClassName("property-toolbar-on-hover");
                }
            }
        } else {
            if (!cleared)
                element.removeChild(prevState.element);

            clearRenderToolbarContent(element);
        }
    }

    private boolean allHover(ToolbarAction[] toolbarActions) {
        for (ToolbarAction toolbarAction : toolbarActions) {
            if (!toolbarAction.isHover()) {
                return false;
            }
        }
        return true;
    }

    private Element wrapPropertyToolbarItemGroup(Element propertyToolbarItemGroup, Element toolbarElement, Element element, boolean start) {
        if(propertyToolbarItemGroup == null)
            propertyToolbarItemGroup = Document.get().createDivElement();
        propertyToolbarItemGroup.addClassName(start ? "property-toolbar-item-hover-start" : "property-toolbar-item-hover-end");
        propertyToolbarItemGroup.addClassName("property-toolbar-item-hover");

        addToToolbar(toolbarElement, start, propertyToolbarItemGroup);

        addToToolbar(propertyToolbarItemGroup, start, element);

        return propertyToolbarItemGroup;
    }

    private void addToToolbar(Element toolbarElement, boolean start, Element element) {
        if(start)
            toolbarElement.insertFirst(element);
        else
            toolbarElement.appendChild(element);
    }

    protected void clearRenderToolbarContent(Element element) {
        GwtClientUtils.clearFillParentElement(element);
        element.removeClassName("property-toolbar-on-hover");
    }

    public abstract boolean renderContent(Element element, RenderContext renderContext);
    public abstract boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext);
    public abstract boolean clearRenderContent(Element element, RenderContext renderContext);

    public int getWidthPadding() {
        return 0;
    }

    public abstract String format(PValue value, RendererType rendererType);

    public boolean isCustomRenderer() {
        return false;
    }
}
