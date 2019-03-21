package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.WordLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.classes.DataType;

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