package platform.client.logics.classes;

import platform.client.ClientResourceBundle;
import platform.client.form.PropertyEditorComponent;
import platform.client.form.PropertyRendererComponent;
import platform.client.form.editor.FilePropertyEditor;
import platform.client.form.renderer.WordPropertyRenderer;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Data;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;

public class ClientWordClass extends ClientStaticFormatFileClass {

    public final static ClientWordClass instance = new ClientWordClass();

    public ClientWordClass() {
    }

    public ClientWordClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    @Override
    public String[] getExtensions() {
        return new String[] {"doc", "docx"};
    }

    public String getFileSID() {
        return "WordClass";
    }

    public PropertyRendererComponent getRendererComponent(ClientPropertyDraw property) {
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
    public PropertyEditorComponent getDataClassEditorComponent(Object value, ClientPropertyDraw property) {
        return new FilePropertyEditor(multiple, ClientResourceBundle.getString("logics.classes.word"), getExtensions());
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