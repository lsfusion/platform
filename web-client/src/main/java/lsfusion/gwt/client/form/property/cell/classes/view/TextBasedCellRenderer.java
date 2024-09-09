package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.TextAreaElement;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.TextBasedCellEditor;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import java.util.Objects;

// the renderer which may be renderered as plain input (or td in table)
public abstract class TextBasedCellRenderer extends InputBasedCellRenderer {

    public TextBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    private static void renderText(Element element, boolean multiLine) {
        element.addClassName("text-based-prop-value");
    }

    private static void clearRenderText(Element element, boolean multiLine) {
        element.removeClassName("text-based-prop-value");
    }

    public static void setTextPadding(Element element) {
        element.addClassName("text-based-prop-sized");
    }

    public static void clearTextPadding(Element element) {
        element.removeClassName("text-based-prop-sized");
    }

    public static boolean isMultiLineInput(Element parent) {
        return TextAreaElement.is(getInputElement(parent));
    }


    protected boolean isMultiLine() {
        return false;
    }

    @Override
    protected Object getExtraValue(UpdateContext updateContext) {
        return new ExtraValue(updateContext.getPlaceholder(), updateContext.getPattern());
    }

    @Override
    public boolean canBeRenderedInTD() {
        if(super.canBeRenderedInTD())
            return true;

        // td always respects the inner text height, so if it is multi line and not autosized, we have wrap the content into a div
        if (isMultiLine() && !property.hasAutoHeight())
            return false;

        // input we have to render in td, since input is a void element, and it can not have children (and they are needed for the toolbar)
        // so the hack is to render it
        return getTag() == null;
    }

    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        boolean renderedAlignment = super.renderContent(element, renderContext);

        Element mainElement = getMainElement(element);
        GwtClientUtils.initDataHtmlOrText(mainElement, property.getDataHtmlOrTextType());
        if(!renderedAlignment) // not rendered text alignment
            CellRenderer.renderWrapTextAlignment(mainElement, property.getHorzTextAlignment(), property.getVertTextAlignment());

        // TEXT PART
        setTextPadding(getSizeElement(element));

        if(property.isEditableNotNull())
            element.addClassName("text-based-value-required");

        if(getInputElement(element) == null)
            renderText(element, isMultiLine());

        return renderedAlignment;
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {

        // TEXT PART
        clearTextPadding(getSizeElement(element));

        if (property.isEditableNotNull())
            element.removeClassName("text-based-value-required");

        element.removeClassName("text-based-value-null");
        element.removeClassName("text-based-value-empty");

        Element inputElement = getInputElement(element);
        if(property.isEditableNotNull()) {
            if(inputElement != null) {
                inputElement.removeClassName("is-invalid");
            }
        }

        if(inputElement == null) // !isTagInput()
            clearRenderText(element, isMultiLine());

        GwtClientUtils.clearDataHtmlOrText(getMainElement(element), property.getDataHtmlOrTextType());

        return super.clearRenderContent(element, renderContext);
    }

    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        boolean isNull = value == null;

        String placeholder = ((ExtraValue) extraValue).placeholder;
        String pattern = ((ExtraValue) extraValue).pattern;

        RendererType rendererType = updateContext.getRendererType();
        String innerText = isNull ? "" : format(value, rendererType, pattern);
        if(isNull) {
            element.addClassName("text-based-value-null");
        } else {
            element.removeClassName("text-based-value-null");
            if(innerText.isEmpty()) {
                innerText = EMPTY_VALUE;
                element.addClassName("text-based-value-empty");
            } else
                element.removeClassName("text-based-value-empty");
        }
        //title is shown as an embedded tooltip on mouseover
        element.setTitle(property.echoSymbols || property.valueTooltip != null ? "" : innerText);

        Element inputElement = getInputElement(element);
        if(inputElement != null) {
            assert isTagInput();
            if(property.isEditableNotNull()) {
                if (isNull) {
                    inputElement.addClassName("is-invalid");
                } else {
                    inputElement.removeClassName("is-invalid");
                }
            }
            updateInputContent(inputElement.cast(), innerText, value, rendererType);
            if (placeholder != null)
                inputElement.setAttribute("placeholder", placeholder);
            else
                inputElement.removeAttribute("placeholder");
            return false;
        }

        // important to make paste work (otherwise DataGrid.sinkPasteEvent cannot put empty selection), plus for sizing
        GwtClientUtils.setDataHtmlOrText(element, isNull ? (placeholder != null ? placeholder : EscapeUtils.UNICODE_NBSP) : innerText, false);
        return true;
    }

    protected void updateInputContent(InputElement inputElement, String innerText, PValue value, RendererType rendererType) {
        TextBasedCellEditor.setTextInputValue(inputElement, innerText);
    }

    public static class ExtraValue {
        public final String placeholder;
        public final String pattern;

        public ExtraValue(String placeholder, String pattern) {
            this.placeholder = placeholder;
            this.pattern = pattern;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof ExtraValue
                    && GwtClientUtils.nullEquals(placeholder, ((ExtraValue) o).placeholder)
                    && GwtClientUtils.nullEquals(pattern, ((ExtraValue) o).pattern);
        }

        @Override
        public int hashCode() {
            return Objects.hash(placeholder, pattern);
        }
    }
}
