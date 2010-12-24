package platform.client.logics.classes;

import platform.client.form.ClientFormController;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.cell.ButtonCellView;
import platform.client.form.cell.CellView;
import platform.client.form.editor.ActionPropertyEditor;
import platform.client.form.renderer.ActionPropertyRenderer;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;

public class ClientActionClass extends ClientDataClass implements ClientTypeClass {

    private final String sID = "ActionClass";

    @Override
    public String getSID() {
        return sID;
    }

    public ClientActionClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public byte getTypeId() {
        return Data.ACTION;
    }

    public String getPreferredMask() {
        return "1234567";
    }

    @Override
    public int getMaximumWidth(int maxCharWidth, FontMetrics fontMetrics) {
        return getPreferredWidth(0, fontMetrics);
    }

    public Format getDefaultFormat() {
        return null;
    }

    public PropertyRendererComponent getRendererComponent(Format format, String caption, ComponentDesign design) {
        return new ActionPropertyRenderer(caption);
    }

    public CellView getPanelComponent(ClientPropertyDraw key, ClientGroupObjectValue columnKey, ClientFormController form) {
        return new ButtonCellView(key, columnKey, form);
    }

    public PropertyEditorComponent getEditorComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format, ComponentDesign design) throws IOException, ClassNotFoundException {
        return new ActionPropertyEditor();
    }

    public PropertyEditorComponent getClassComponent(ClientFormController form, ClientPropertyDraw property, Object value, Format format) throws IOException, ClassNotFoundException {
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

    @Override
    public String getConformedMessage() {
        return "Вы действительно хотите выполнить действие";
    }

    @Override
    public String toString() {
        return "Класс действия";
    }
}
