package platform.client.descriptor.nodes;

import platform.client.logics.ClientForm;

public class LayoutFolder extends PlainTextNode<LayoutFolder> {

    public LayoutFolder(ClientForm clientForm) {
        super("Расположение");

        add(new ContainerNode(clientForm.mainContainer));
    }
}
