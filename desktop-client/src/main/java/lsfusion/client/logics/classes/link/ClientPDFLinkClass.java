package lsfusion.client.logics.classes.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.PropertyRenderer;
import lsfusion.client.form.property.classes.renderer.link.PDFLinkPropertyRenderer;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.interop.form.property.DataType;

public class ClientPDFLinkClass extends ClientStaticFormatLinkClass {

    public final static ClientPDFLinkClass instance = new ClientPDFLinkClass(false);

    public ClientPDFLinkClass(boolean multiple) {
        super(multiple);
    }

    public PropertyRenderer getRendererComponent(ClientPropertyDraw property) {
        return new PDFLinkPropertyRenderer(property);
    }

    public byte getTypeId() {
        return DataType.PDFLINK;
    }

    @Override
    public String toString() {
        return ClientResourceBundle.getString("logics.classes.pdf.link");
    }
}
