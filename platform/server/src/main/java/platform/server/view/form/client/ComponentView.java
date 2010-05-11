package platform.server.view.form.client;

import platform.interop.form.layout.SimplexConstraints;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ComponentView implements ClientSerialize {

    int ID;
    public ComponentView(int ID) {
        this.ID = ID;
    }

    public ContainerView container;
    public SimplexConstraints constraints = new SimplexConstraints();

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(ID);
        outStream.writeBoolean(container==null);
        if(container!=null)
            outStream.writeInt(container.ID);
        new ObjectOutputStream(outStream).writeObject(constraints);
    }
}
