package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class CellRenderer<T> {

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
    public void render(Element element, RenderContext renderContext) {
        Style.TextAlign textAlign = property.getTextAlignStyle();
        if (textAlign == null)
            textAlign = getDefaultHorzAlignment();

        String vertAlignment = getDefaultVertAlignment();

        if (renderContext.isAlwaysSelected())
            renderEditSelected(element, property);

        if(GwtClientUtils.isTDorTH(element)) {
            assert isSimpleText(renderContext);
            renderSimpleStatic(element, textAlign, vertAlignment);
        } else
            renderFlexStatic(element, getFlexAlign(textAlign), vertAlignment);

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

    public void update(Element element, UpdateContext updateContext) {
        renderDynamicContent(element, updateContext.getValue(), updateContext);
    }

    private static final String MOREASYNC = "more-async";
    public void renderMoreAsync(Element element, boolean enable) {
        if (enable) {
            ImageElement imageElement = Document.get().createImageElement();
            imageElement.setId(MOREASYNC);

            GwtClientUtils.setThemeImage("loading.gif", imageElement::setSrc);

            element.insertFirst(imageElement);
        } else {
            NodeList<Node> children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Element child = (Element) children.getItem(i);
                if (MOREASYNC.equals(child.getId())) {
                    element.removeChild(child);
                }
            }
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
