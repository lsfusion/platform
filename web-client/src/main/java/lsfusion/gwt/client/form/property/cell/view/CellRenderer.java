package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

public abstract class CellRenderer<T> {

    protected final GPropertyDraw property;

    public CellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererEmpty();
    protected final String NOT_DEFINED_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    public void render(Element element, Object value, RenderContext renderContext, UpdateContext updateContext) {
        renderStatic(element, renderContext);
        renderDynamic(element, value, updateContext);
    }

    protected boolean isSimpleText(RenderContext renderContext) {
        return false;
    }
    protected boolean isSimpleText(UpdateContext updateContext) {
        return false;
    }

    protected Style.TextAlign getDefaultHorzAlignment() {
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
    // and with TextBasedCellEditor.renderDOM
    public void renderStatic(Element element, RenderContext renderContext) {
        Style.TextAlign textAlign = property.getTextAlignStyle();
        if (textAlign == null)
            textAlign = getDefaultHorzAlignment();

        String vertAlignment = getDefaultVertAlignment();

        if(renderContext.isAlwaysSelected())
            renderEditSelected(element, property);

        Integer staticHeight = renderContext.getStaticHeight();
        // is simple text renderer (SINGLE LINE (!)) && has static height (otherwise when div is expanded line-height will not work)
        // maybe later it makes sense to add optimization for ActionOrPropertyValue to look at the upper container if it's has static height
        if(staticHeight != null && isSimpleText(renderContext)) // optimization
            renderSimpleStatic(element, textAlign, vertAlignment, staticHeight);
        else {
            String horzAlignment = getFlexAlign(textAlign);
            element = renderFlexStatic(element, horzAlignment, vertAlignment, staticHeight);
        }

        renderStaticContent(element, renderContext);
    }

    public static void renderEditSelected(Element element, GPropertyDraw property) {
        if(property.hasEditObjectAction)
            element.addClassName("selectedCellHasEdit");
    }
    public static void clearEditSelected(Element element, GPropertyDraw property) {
        if(property.hasEditObjectAction)
            element.removeClassName("selectedCellHasEdit");
    }

    private Element renderFlexStatic(Element element, String horzAlignment, String vertAlignment, Integer staticHeight) {
        int paddings = 0;
        if(GwtClientUtils.isTDorTH(element)) { // we need to wrap into div, at list because we cannot set display:flex to div
            element = wrapTD(element);
            if(staticHeight != null) // we need to remove paddings when setting maximum height (maybe in future margins might be used, and that will not be needed)
                paddings = getHeightPadding() * 2;
        }
        GwtClientUtils.setAlignedFlexCenter(element, vertAlignment, horzAlignment);

        if(staticHeight != null) // setting maxHeight for div ??? if inner context is too big, for example multi-line text (strictly speaking for now it seems that it is used only for multi-line text)
            GwtClientUtils.setMaxHeight(element, staticHeight, paddings);
        return element;
    }

    private static Element wrapTD(Element element) {
        assert GwtClientUtils.isTDorTH(element);
        return GwtClientUtils.wrapDiv(element);
    }
    public static Element unwrapTD(Element element) {
        assert GwtClientUtils.isTDorTH(element);
        return element.getFirstChildElement();
    }

    private static void renderSimpleStatic(Element element, Style.TextAlign horzAlignment, String vertAlignment, Integer staticHeight) {
        GPropertyTableBuilder.setLineHeight(element, staticHeight);

        assert vertAlignment.equals("center");
        element.getStyle().setTextAlign(horzAlignment);
    }

    // of course without optimization of using the same render element this drops won't be needed, but it is important optimization
    public void clearRender(Element element, RenderContext renderContext) {
        GwtClientUtils.removeAllChildren(element);

        if(renderContext.isAlwaysSelected())
            clearEditSelected(element, property);

        Integer staticHeight = renderContext.getStaticHeight();
        // is simple text renderer (SINGLE LINE (!)) && has static height (otherwise when div is expanded line-height will not work)
        boolean sameElement = true;
        if(staticHeight != null && isSimpleText(renderContext)) // optimization
            clearRenderSimpleStatic(element);
        else
            sameElement = clearRenderFlexStatic(element, staticHeight);

        if(sameElement)
            clearRenderContent(element, renderContext);
    }
    private void clearRenderSimpleStatic(Element element) {
        GPropertyTableBuilder.clearLineHeight(element);

        element.getStyle().clearTextAlign();
    }
    private boolean clearRenderFlexStatic(Element element, Integer staticHeight) {
        if(!GwtClientUtils.isTDorTH(element)) {
            GwtClientUtils.clearAlignedFlexCenter(element);
            element.getStyle().clearProperty("alignItems");
            element.getStyle().clearProperty("justifyContent");

            if(staticHeight != null)
                GwtClientUtils.clearMaxHeight(element);

            return true;
        }
        return false;
    }

    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
        if(!(updateContext.isStaticHeight() && isSimpleText(updateContext)) && GwtClientUtils.isTDorTH(element)) // there is another unwrapping in GPropertyTableBuilder, so it also should be kept consistent
            element = unwrapTD(element);

        renderDynamicContent(element, value, updateContext);
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
