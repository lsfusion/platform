package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.DocumentPropertyEditor;
import platform.client.form.renderer.WordPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.ComponentDesign;
import platform.interop.Data;

import java.awt.*;
import java.text.Format;

public class ClientWordClass extends ClientFileClass {

    public final static ClientWordClass instance = new ClientWordClass();

    private final String sID = "WordClass";

    @Override
    public String getSID() {
        return sID;
    }

    public PropertyRendererComponent getRendererComponent(String caption, ClientPropertyDraw property) {
        return new WordPropertyRenderer(property);
    }

    @Override
    public String formatString(Object obj) {
        return obj.toString();
    }

    public byte getTypeId() {
        return Data.WORD;
    }

    @Override
    public PropertyEditorComponent getComponent(Object value, ClientPropertyDraw property) {
        return new DocumentPropertyEditor(value, ClientResourceBundle.getString("logics.classes.word"), "doc", "docx");
    }

    @Override
    public int getPreferredHeight(FontMetrics font) {
        return 18;
    }

    @Override
    public int getPreferredWidth(int prefCharWidth, FontMetrics font) {
        return 18;
    }

    @Override
    public int getMinimumWidth(int minCharWidth, FontMetrics font) {
        return 15;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.word.file");
    }
}