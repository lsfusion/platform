package lsfusion.client.descriptor.nodes;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.logics.ClientForm;

public class LayoutFolder extends PlainTextNode<LayoutFolder> {

    public LayoutFolder(ClientForm clientForm) {
        super(ClientResourceBundle.getString("descriptor.editor.arrangement"));

        add(new ContainerNode(clientForm.mainContainer));
    }
}
