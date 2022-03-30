package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import java.util.Arrays;

public abstract class CellRenderer<T> {

    protected static final String ICON_LOADING = "loading.gif";

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

    private Style.TextAlign getHorzTextAlignment() {
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

    protected boolean needToRenderToolbarContent() {
        return true;
    }

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class RenderedState {
        public Object value;
        public boolean loading;

        public ToolbarState toolbar;
    }
    private boolean equalsDynamicState(RenderedState state, Object value, boolean isLoading) {
        return GwtClientUtils.nullEquals(state.value, value) && state.loading == isLoading;
    }

    private static final String RENDERED = "rendered";

    public void update(Element element, UpdateContext updateContext) {
        String background = updateContext.getBackground();
        AbstractDataGridBuilder.updateColors(element, background, updateContext.getForeground(), true);

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
        boolean cleared = false;
        if(isNew || !equalsDynamicState(renderedState, value, loading)) {
            cleared = renderDynamicContent(element, value, loading, updateContext);

            renderedState.value = value;
            renderedState.loading = loading;
        }

        if(needToRenderToolbarContent())
            renderToolbarContent(element, updateContext, renderedState, value, background, cleared);
    }

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class ToolbarState {
        public final boolean loading;
        public final GPropertyDraw.QuickAccessAction[] quickAccessActions;

        public final String background;

        public Element element;

        public ToolbarState(boolean loading, GPropertyDraw.QuickAccessAction[] quickAccessActions, String background) {
            this.loading = loading;
            this.quickAccessActions = quickAccessActions;

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

        if(stateA.quickAccessActions != stateB.quickAccessActions) {
            if (stateA.quickAccessActions.length != stateB.quickAccessActions.length)
                return false;
            for(int i=0;i<stateA.quickAccessActions.length;i++)
                if(stateA.quickAccessActions[i].index != stateB.quickAccessActions[i].index)
                    return false;
        }

        return true;
    }

    private final static GPropertyDraw.QuickAccessAction[] readonlyQuickAccessActions = new GPropertyDraw.QuickAccessAction[0];
    // cleared - cleared with setInnerText / setInnerHTML
    protected void renderToolbarContent(Element element, UpdateContext updateContext, RenderedState renderedState, Object value, String background, boolean cleared) {
        boolean loading = updateContext.isLoading() && !renderedLoadingContent(updateContext);
        GPropertyDraw.QuickAccessAction[] quickAccessActions = updateContext.isPropertyReadOnly() ? readonlyQuickAccessActions : property.getQuickAccessActions(updateContext.isSelectedRow(), updateContext.isFocusedColumn());

        boolean needToolbar = loading || quickAccessActions.length > 0;

        ToolbarState toolbarState = needToolbar ? new ToolbarState(loading, quickAccessActions, background) : null;
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
                toolbarElement.addClassName("property-toolbar");
                toolbarElement.addClassName("background-inherit");
                element.appendChild(toolbarElement);
                GwtClientUtils.setupEdgeStretchParent(toolbarElement, true, start);

                toolbarState.element = toolbarElement;
            } else
                GwtClientUtils.removeAllChildren(toolbarElement);

            // we cannot inherit parent background, since it's set for element (so we can't use background-inherit technique)
            GFormController.setBackgroundColor(toolbarElement, background, true);

            if(loading) {
                ImageElement loadingImage = Document.get().createImageElement();
                loadingImage.addClassName("property-toolbar-item"); // setting paddings
                GwtClientUtils.setThemeImage(ICON_LOADING, loadingImage::setSrc);

                addToToolbar(toolbarElement, start, loadingImage);
            }

            if (quickAccessActions.length > 0) {
                Element verticalSeparator = GwtClientUtils.createVerticalStretchSeparator().getElement();
                addToToolbar(toolbarElement, start, verticalSeparator);

                int hoverCount = 0;
                for (GPropertyDraw.QuickAccessAction quickAccessAction : quickAccessActions) {
                    int actionIndex = quickAccessAction.index;
                    ImageElement actionElement = Document.get().createImageElement();
                    actionElement.addClassName("property-toolbar-item"); // setting paddings
                    if (quickAccessAction.hover) {
                        actionElement.addClassName("hide");
                        hoverCount++;
                    }
                    GwtClientUtils.setThemeImage(quickAccessAction.action + ".png", actionElement::setSrc);
                    GwtClientUtils.setOnClick(actionElement, () -> updateContext.changeProperty(new GUserInputResult(value, actionIndex)));

                    addToToolbar(toolbarElement, start, actionElement);
                }

                if (hoverCount > 0) {
                    element.addClassName("property-toolbar-on-hover");
                }
                if (hoverCount == quickAccessActions.length) {
                    verticalSeparator.addClassName("hide");
                }
            }
        } else {
            if (!cleared)
                element.removeChild(prevState.element);

            clearRenderToolbarContent(element);
        }
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
