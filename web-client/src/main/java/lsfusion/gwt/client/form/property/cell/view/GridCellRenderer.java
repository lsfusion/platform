package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.form.design.GFont;

public abstract class GridCellRenderer<T> {
    private static final ClientMessages messages = ClientMessages.Instance.get();
    protected final String EMPTY_VALUE = messages.formRendererEmpty();
    protected final String NOT_DEFINED_VALUE = messages.formRendererNotDefined();
    protected final String REQUIRED_VALUE = messages.formRendererRequired();

    public void render(Element element, GFont font, Object value, boolean isSingle) {
        renderStatic(element, font, isSingle);
        renderDynamic(element, font, value, isSingle);
    }

    public void renderStatic(Element element, GFont font, boolean isSingle) {}

    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
    }

    public abstract String format(T value);
}
