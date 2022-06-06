package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.GEditBindingMap;
import lsfusion.gwt.client.view.GColorTheme;
import lsfusion.gwt.client.view.MainFrame;

public abstract class CellRenderer<T> {

    public static final String ICON_LOADING = "loading.gif";

    protected final GPropertyDraw property;

    public CellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererEmpty();
    protected final String NOT_DEFINED_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    public boolean isSimpleText(RenderContext renderContext) {
        return false;
    }
    public boolean isSimpleText(UpdateContext updateContext) {
        return false;
    }

    protected Style.TextAlign getDefaultHorzTextAlignment() {
        return Style.TextAlign.LEFT;
    }
    protected String getDefaultVertAlignment() {
        return "center";
    }

    private static String getFlexAlign(Style.TextAlign textAlign) {
        switch (textAlign) {
            case LEFT:
                return "flex-start"; // left/start somewhy doesn't work with text
            case RIGHT:
                return "flex-end"; // rigt/end somewhy doesn't work with text
            default:
                return textAlign.getCssName();
        }
    }

    // should be consistent with getWidthPadding and getHeightPadding
    // and with TextBasedCellEditor.renderStaticContent
    public void render(Element element, RenderContext renderContext) {
        Style.TextAlign horzTextAlignment = getHorzTextAlignment();
        String vertAlignment = getDefaultVertAlignment();
        if(GwtClientUtils.isTDorTH(element)) {
            assert isSimpleText(renderContext);
            renderSimpleStatic(element, horzTextAlignment, vertAlignment);
        } else
            renderFlexStatic(element, getFlexAlign(horzTextAlignment), vertAlignment);

        renderStaticContent(element, renderContext);
    }

    public Style.TextAlign getHorzTextAlignment() {
        Style.TextAlign textAlign = property.getTextAlignStyle();
        if (textAlign == null)
            textAlign = getDefaultHorzTextAlignment();
        return textAlign;
    }

    public static void renderEditSelected(Element element, GPropertyDraw property) {
        if(property.hasEditObjectAction)
            element.addClassName("selectedCellHasEdit");
    }
    public static void clearEditSelected(Element element, GPropertyDraw property) {
        if(property.hasEditObjectAction)
            element.removeClassName("selectedCellHasEdit");
    }

    private static void renderFlexStatic(Element element, String horzAlignment, String vertAlignment) {
        element.addClassName("wrap-center");

        if(!vertAlignment.equals("center"))
            element.getStyle().setProperty("alignItems", vertAlignment);
        if(!horzAlignment.equals("center"))
            element.getStyle().setProperty("justifyContent", horzAlignment);
    }

    private static void renderSimpleStatic(Element element, Style.TextAlign horzAlignment, String vertAlignment) {
//        if(staticHeight != null)
//            GPropertyTableBuilder.setLineHeight(element, staticHeight);
        assert vertAlignment.equals("center");
        // actually vertical-align works only for text content or td content
        // however for td line height should not be set (!) and for div should be set, god knows why
        // it seems that vertical-align is middle by default, however just in case
        element.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
        element.getStyle().setTextAlign(horzAlignment);
    }

    public void clearRender(Element element, RenderContext renderContext) {
        GwtClientUtils.removeAllChildren(element);

        if(GwtClientUtils.isTDorTH(element)) { // optimization
            assert isSimpleText(renderContext);
            clearRenderSimpleStatic(element);
        } else
            clearRenderFlexStatic(element);

        clearRenderContent(element, renderContext);

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
    private static void clearRenderSimpleStatic(Element element) {
//        if(staticHeight != null)
//            GPropertyTableBuilder.clearLineHeight(element);
        element.getStyle().clearProperty("verticalAlign");
        element.getStyle().clearTextAlign();
    }
    private static void clearRenderFlexStatic(Element element) {
        element.removeClassName("wrap-center");
        element.getStyle().clearProperty("alignItems");
        element.getStyle().clearProperty("justifyContent");
    }

    protected boolean renderedLoadingContent(UpdateContext updateContext) {
        return false;
    }

    protected String getBaseBackground(Object value) {
        return null;
    }

    protected boolean needToRenderToolbarContent() {
        return true;
    }

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class RenderedState {
        public Object value;
        public boolean loading;
        public GColorTheme colorTheme; // for action and color cell renderer

        public String foreground;
        public String background;

        public boolean rerender;

        public ToolbarState toolbar;
    }
    private boolean equalsDynamicState(RenderedState state, Object value, boolean isLoading, GColorTheme colorTheme) {
        return GwtClientUtils.nullEquals(state.value, value) && state.loading == isLoading && state.colorTheme == colorTheme && !state.rerender;
    }
    private boolean equalsColorState(RenderedState state, String background, String foreground) {
        return GwtClientUtils.nullEquals(state.background, background) && GwtClientUtils.nullEquals(state.foreground, foreground);
    }

    private static final String RENDERED = "rendered";

    protected String getBackground(UpdateContext updateContext) {
        String baseBackground = getBaseBackground(updateContext.getValue()); // not converted (with getDisplayColor)
        return updateContext.getBackground(baseBackground); // converted (with getDisplayColor)
    }

    protected void rerenderState(Element element, boolean set) {
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

        Object value = updateContext.getValue();
        boolean loading = updateContext.isLoading() && renderedLoadingContent(updateContext);

        RenderedState renderedState = (RenderedState) element.getPropertyObject(RENDERED);
        boolean isNew = false;
        if(renderedState == null) {
            renderedState = new RenderedState();
            element.setPropertyObject(RENDERED, renderedState);

            isNew = true;
        }

        // already themed colors expected
        String background = getBackground(updateContext);
        String foreground = updateContext.getForeground();
        if(isNew || !equalsColorState(renderedState, background, foreground)) {
            renderedState.background = background;
            renderedState.foreground = foreground;

            AbstractDataGridBuilder.updateColors(element, background, foreground);
        }

        boolean cleared = false;
        if(isNew || !equalsDynamicState(renderedState, value, loading, MainFrame.colorTheme)) {
            // there might be stack overflow, if this is done after renderDynamicContent, and this is a custom cell render, which calls changeProperty in its update method
            // setting value earlier breaks the recursion
            renderedState.value = value;
            renderedState.loading = loading;
            renderedState.colorTheme = MainFrame.colorTheme;
            renderedState.rerender = false;

            cleared = renderDynamicContent(element, value, loading, updateContext);
        }

        if(needToRenderToolbarContent())
            renderToolbarContent(element, updateContext, renderedState, background, cleared);
    }

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class ToolbarState {
        public final boolean loading;
        public final ToolbarAction[] toolbarActions;

        public final String background;

        public Element element;

        public ToolbarState(boolean loading, ToolbarAction[] toolbarActions, String background) {
            this.loading = loading;
            this.toolbarActions = toolbarActions;

            this.background = background;
        }
    }

    private boolean equalsState(ToolbarState stateA, ToolbarState stateB) {
        if(stateA == null)
            return stateB == null;
        if(stateB == null)
            return false;

        if(!(stateA.loading == stateB.loading && GwtClientUtils.nullEquals(stateA.background, stateB.background)))
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
        String getImage();

        boolean matches(ToolbarAction action);

        // there are to ways of working with toolbar actions
        // 1 is setting some mark for the target element (s) and then checking it in regular event handler (this  is more flexible, for example in editOnSingleClick scenario, however needs some assertions)
        // 2 setting onMouseDown and stopping propagation (this way the row change won't be handled, when using ALL, and maybe some mor things)
        default void setToolbarAction(ImageElement actionImgElement, Object value) {
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

        void setOnPressed(ImageElement actionImgElement, UpdateContext updateContext);
    }

    public final static GPropertyDraw.QuickAccessAction[] noToolbarActions = new GPropertyDraw.QuickAccessAction[0];
    // cleared - cleared with setInnerText / setInnerHTML
    // backgroud - converted (with getDisplayColor)
    protected void renderToolbarContent(Element element, UpdateContext updateContext, RenderedState renderedState, String background, boolean cleared) {
        boolean loading = updateContext.isLoading() && !renderedLoadingContent(updateContext);
        ToolbarAction[] toolbarActions = updateContext.getToolbarActions();

        boolean needToolbar = loading || toolbarActions.length > 0;

        ToolbarState toolbarState = needToolbar ? new ToolbarState(loading, toolbarActions, background) : null;
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
            boolean start = !getHorzTextAlignment().equals(Style.TextAlign.LEFT);
            if(toolbarElement == null) {
                toolbarElement = Document.get().createDivElement();
                toolbarElement.addClassName(start ? "property-toolbar-start" : "property-toolbar-end");
                element.appendChild(toolbarElement);
                GwtClientUtils.setupEdgeStretchParent(toolbarElement, true, start);

                toolbarState.element = toolbarElement;
            } else
                GwtClientUtils.removeAllChildren(toolbarElement);

            if(loading) {
                ImageElement loadingImage = Document.get().createImageElement();
                GwtClientUtils.setThemeImage(ICON_LOADING, loadingImage::setSrc);
                loadingImage.addClassName("property-toolbar-loading");
                setToolbarBackground(loadingImage, background);

                addToToolbar(toolbarElement, start, loadingImage);
            }

            if (toolbarActions.length > 0) {
                Element propertyToolbarItemGroup = null;
                Element verticalSeparator = GwtClientUtils.createVerticalStretchSeparator().getElement();
                setToolbarBackground(verticalSeparator, background);
                if(allHover(toolbarActions)) {
                    propertyToolbarItemGroup = wrapPropertyToolbarItemGroup(null, toolbarElement, verticalSeparator, start);
                } else {
                    addToToolbar(toolbarElement, start, verticalSeparator);
                }

                int hoverCount = 0;
                for (ToolbarAction toolbarAction : toolbarActions) {
                    // there is an assertion that the DOM structure will be exactly like that in setOnPressed / for optimization reasons
                    DivElement actionDivElement = Document.get().createDivElement();
                    ImageElement actionImgElement = Document.get().createImageElement();
                    actionDivElement.appendChild(actionImgElement);
                    actionDivElement.addClassName("property-toolbar-item"); // setting paddings
                    setToolbarBackground(actionDivElement, background);

                    GwtClientUtils.setThemeImage(toolbarAction.getImage() + ".png", actionImgElement::setSrc);
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
        propertyToolbarItemGroup.addClassName(start ? "property-toolbar-item-group-start" : "property-toolbar-item-group-end");
        propertyToolbarItemGroup.addClassName("hide");

        addToToolbar(toolbarElement, start, propertyToolbarItemGroup);

        addToToolbar(propertyToolbarItemGroup, start, element);

        return propertyToolbarItemGroup;
    }

    private static void setToolbarBackground(Element element, String background) {
        element.addClassName("background-inherit");
        // we cannot inherit parent background, since it's set for element (so we can't use background-inherit technique)
        GFormController.setBackgroundColor(element, background);
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

    public abstract void renderStaticContent(Element element, RenderContext renderContext);
    public abstract boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext);
    public abstract void clearRenderContent(Element element, RenderContext renderContext);

    public int getWidthPadding() {
        return 0;
    }
    public int getHeightPadding() {
        return 0;
    }

    public abstract String format(T value);

    public boolean isAutoDynamicHeight() {
        return true;
    }

    public boolean isCustomRenderer() {
        return false;
    }
}
