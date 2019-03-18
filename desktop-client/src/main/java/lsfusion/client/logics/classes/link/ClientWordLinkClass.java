package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.WordLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientWordLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientWordLinkClass instance = new ClientWordLinkClass(false);

    public ClientWordLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new WordLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.WORDLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.word.link");
    }
}