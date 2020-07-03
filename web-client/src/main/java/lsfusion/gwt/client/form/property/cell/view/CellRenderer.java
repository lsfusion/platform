package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.ClientMessages;

public abstract class CellRenderer<T> {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererEmpty();
    protected final String NOT_DEFINED_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    public void render(Element element, Object value, RenderContext renderContext, UpdateContext updateContext) {
        renderStatic(element, renderContext);
        renderDynamic(element, value, updateContext);
    }

    // should be consistent with getWidthPadding
    public void renderStatic(Element element, RenderContext renderContext) {}

    public int getWidthPadding() {
        return 0;
    }

    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
    }

    public abstract String format(T value);
}
