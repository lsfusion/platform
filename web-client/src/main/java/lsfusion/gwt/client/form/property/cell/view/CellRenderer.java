package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.async.GAsyncChange;

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

        if (renderContext.isAlwaysSelected())
            renderEditSelected(element, property);

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

        if(renderContext.isAlwaysSelected())
            clearEditSelected(element, property);

        if(GwtClientUtils.isTDorTH(element)) { // optimization
            assert isSimpleText(renderContext);
            clearRenderSimpleStatic(element);
        } else
            clearRenderFlexStatic(element);

        clearRenderContent(element, renderContext);
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

    private static final String STATE = "state";

    public void update(Element element, UpdateContext updateContext) {
        renderDynamicContent(element, updateContext.getValue(), updateContext);
    }

    // in theory in most case we can get previous state without storing it in Element, but for now it's the easiest way
    private static class ToolbarState {
        public final boolean loading;
        public final boolean selectedQuickAccess;

        public Element toolbarElement;

        public ToolbarState(boolean loading, boolean selectedQuickAccess) {
            this.loading = loading;
            this.selectedQuickAccess = selectedQuickAccess;
        }
    }
    private boolean equalsState(ToolbarState stateA, ToolbarState stateB) {
        if(stateA == null)
            return stateB == null;
        if(stateB == null)
            return false;

        return stateA.loading == stateB.loading && stateA.selectedQuickAccess == stateB.selectedQuickAccess;
    }

    optimization to update only if selected has quick access ???

    // cleared - cleared with setInnerText / setInnerHTML
    protected void renderToolbarContent(Element element, UpdateContext updateContext, boolean cleared) {
        boolean loading = updateContext.isLoading();
        Pair<String, GAsyncChange>[] selectedActions = null;
        boolean selectedQuickAccess = updateContext.isSelected() && (selectedActions = property.getSelectedActions()).length > 0;

        boolean needToolbar = loading || selectedQuickAccess;
        ToolbarState toolbarState = needToolbar ? new ToolbarState(loading, selectedQuickAccess) : null;

        ToolbarState prevState = (ToolbarState) element.getPropertyObject(STATE);
        if (equalsState(toolbarState, prevState)) { // already rendered
            if(!(cleared && needToolbar)) // if cleared we still need to rerender the toolbar
                return;

            toolbarState = prevState;
        } else {
            if(prevState != null)
                toolbarState.toolbarElement = prevState.toolbarElement;

            element.setPropertyObject(STATE, toolbarState);
        }

        Element toolbarElement = cleared ? null : toolbarState.toolbarElement;
        if(needToolbar) {
            boolean start = getHorzTextAlignment().equals(Style.TextAlign.RIGHT);
            if(toolbarElement == null) {
                toolbarElement = Document.get().createDivElement();
                toolbarElement.addClassName("wrap-img-paddings");dssd
                element.appendChild(toolbarElement);
                GwtClientUtils.setupEdgeCenteredParent(toolbarElement, true, start);

                toolbarState.toolbarElement = toolbarElement;
            } else
                GwtClientUtils.removeAllChildren(toolbarElement);

            if(loading) {
                ImageElement loadingImage = Document.get().createImageElement();
                loadingImage.addClassName("wrap-img-paddings"); // setting paddings
                GwtClientUtils.setThemeImage(ICON_LOADING, loadingImage::setSrc);

                addToToolbar(toolbarElement, start, loadingImage);
            }

            if(selectedQuickAccess) {
                for(Pair<String, GAsyncChange> selectedAction : selectedActions) {
                    ImageElement actionImage = Document.get().createImageElement();
                    actionImage.addClassName("wrap-img-paddings"); // setting paddings
                    GwtClientUtils.setThemeImage(selectedAction + ".png", actionImage::setSrc);
                    sdsd
                    GwtClientUtils.setOnClick(actionImage, () -> {
                        updateContext.changeProperty(new GUserInputResult(null, ));
                    });

                    addToToolbar(toolbarElement, start, actionImage);
                }
            }
        } else {
            if (!cleared)
                element.removeChild(toolbarElement);

            GwtClientUtils.clearFillParentElement(element);
            toolbarState.toolbarElement = null;
        }


        if (loading) {
            loadingImage = Document.get().createImageElement();
            loadingImage.addClassName("wrap-img-paddings"); // setting paddings
            GwtClientUtils.setThemeImage(ICON_LOADING, loadingImage::setSrc);

            Style.TextAlign horzTextAlignment = getHorzTextAlignment();
            if(horzTextAlignment.equals(Style.TextAlign.CENTER)) {
                element.insertFirst(loadingImage); // just adding first assuming that it will be centered by the element parent
            } else {
                element.appendChild(loadingImage);
                GwtClientUtils.setupEdgeCenteredParent(loadingImage, true, horzTextAlignment.equals(Style.TextAlign.RIGHT));
            }

            element.setPropertyObject(LOADING, loadingImage);
        } else {
            if (!cleared)
                element.removeChild(loadingImage);

            GwtClientUtils.clearFillParentElement(element);
            element.setPropertyObject(LOADING, null);
        }
    }

    private void addToToolbar(Element toolbarElement, boolean start, Element element) {
        if(start)
            toolbarElement.insertFirst(element);
        else
            toolbarElement.appendChild(element);
    }

    protected void clearRenderLoadingContent(Element element, RenderContext renderContext) {
        ImageElement loadingImage = (ImageElement) element.getPropertyObject(LOADING);
        if(loadingImage != null) {
            GwtClientUtils.clearFillParentElement(element);
            element.setPropertyObject(LOADING, null);
        }
    }

    public abstract void renderStaticContent(Element element, RenderContext renderContext);
    public abstract void renderDynamicContent(Element element, Object value, UpdateContext updateContext);
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
