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

    protected boolean isSimpleText() {
        return false;
    }

    protected Style.TextAlign getDefaultHorzAlignment() {
        return Style.TextAlign.LEFT;
    }
    protected String getDefaultVertAlignment() {
        return "center";
    }

    // should be consistent with getWidthPadding and getHeightPadding
    // and with TextBasedCellEditor.renderDOM
    public void renderStatic(Element element, RenderContext renderContext) {
        Style.TextAlign textAlign = property.getTextAlignStyle();
        if (textAlign == null)
            textAlign = getDefaultHorzAlignment();

        String vertAlignment = getDefaultVertAlignment();

        Integer staticHeight = renderContext.getStaticHeight();
        // is simple text renderer (SINGLE LINE (!)) && has static height (otherwise when div is expanded line-height will not work)
        if(isSimpleText() && staticHeight != null) // optimization
            renderSimpleStatic(element, textAlign, vertAlignment, staticHeight);
        else {
            String horzAlignment = textAlign.getCssName();
            element = renderFlexStatic(element, horzAlignment, vertAlignment, staticHeight);
        }

        renderStaticContent(element, renderContext);
    }

    private Element renderFlexStatic(Element element, String horzAlignment, String vertAlignment, Integer staticHeight) {
        int paddings = 0;
        if(GwtClientUtils.isTD(element)) { // we need to wrap into div, at list because we cannot set display:flex to div
            element = GwtClientUtils.wrapDiv(element);
            if(staticHeight != null) // we need to remove paddings when setting maximum height (maybe in future margins might be used, and that will not be needed)
                paddings = getHeightPadding() * 2;
        }
        GwtClientUtils.setAlignedFlexCenter(element, vertAlignment, horzAlignment);

        if(staticHeight != null) // setting maxHeight for div ??? if inner context is too big, for example multi-line text (strictly speaking for now it seems that it is used only for multi-line text)
            GwtClientUtils.setMaxHeight(element, staticHeight, paddings);
        return element;
    }

    private static void renderSimpleStatic(Element element, Style.TextAlign horzAlignment, String vertAlignment, Integer staticHeight) {
        GPropertyTableBuilder.setLineHeight(element, staticHeight);

        assert vertAlignment.equals("center");
        element.getStyle().setTextAlign(horzAlignment);
    }

    // of course without optimization of using the same render element this drops won't be needed, but it is important optimization
    public void clearRender(Element element, RenderContext renderContext) {
        GwtClientUtils.removeAllChildren(element);
        Integer staticHeight = renderContext.getStaticHeight();
        // is simple text renderer (SINGLE LINE (!)) && has static height (otherwise when div is expanded line-height will not work)
        boolean sameElement = true;
        if(isSimpleText() && staticHeight != null) // optimization
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
        if(!GwtClientUtils.isTD(element)) {
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
        if(!(isSimpleText() && updateContext.isStaticHeight()) && GwtClientUtils.isTD(element))
            element = element.getFirstChildElement();

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
}
