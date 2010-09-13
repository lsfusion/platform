package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.ButtonCellView;
import platform.client.form.cell.CellView;
import platform.client.form.editor.ActionPropertyEditor;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientCell;
import platform.interop.ComponentDesign;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public class ClientActionClass extends ClientDataClass {

    public ClientActionClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public String getPreferredMask() {
        return "1234567";
    }

    @Override
    public int getMaximumWidth(FontMetrics fontMetrics) {
        return getPreferredWidth(fontMetrics);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new ActionPropertyRenderer(caption);
    }

    public CellView getPanelComponent(ClientCell key, ClientFormController form) {
        return new ButtonCellView(key, form);
    }

    public PropertyEditorComponent getEditorComponent(ClientFormController form, ClientCell property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        return new ActionPropertyEditor();
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, ClientCell property, Object value, Format format) throws IOException, ClassNotFoundException {
        return null;
    }

    protected PropertyEditorComponent getComponent(Object value, Format format, ComponentDesign design) {
        return null;
    }

    @Override
    public boolean shouldBeDrawn(ClientFormController form) {
        return !form.isReadOnlyMode(); // не рисуем кнопки на диалогах
    }

    public Object parseString(String s) throws ParseException {
        throw new ParseException("ActionClass не поддерживает конвертацию из строки.", 0);
    }
}
