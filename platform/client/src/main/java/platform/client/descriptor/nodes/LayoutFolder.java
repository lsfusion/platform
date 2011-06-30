package platform.client.descriptor.nodes;

import platform.client.ClientResourceBundle;
import platform.client.logics.ClientForm;

public class LayoutFolder extends PlainTextNode<LayoutFolder> {

    public LayoutFolder(ClientForm clientForm) {
        super(ClientResourceBundle.getString("descriptor.editor.arrangement"));

        add(new ContainerNode(clientForm.mainContainer));
    }
}
