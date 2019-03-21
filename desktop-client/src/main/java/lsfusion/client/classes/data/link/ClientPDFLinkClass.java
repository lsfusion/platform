package lsfusion.client.classes.data.link;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.cell.classes.view.link.PDFLinkPropertyRenderer;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.interop.classes.DataType;

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
