package platform.client.logics;

import platform.interop.ComponentDesign;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

public class ClientComponentView implements Serializable {

    public int compID; // ID есть и у свойст и у объектов, так что чтобы не путаться

    public ComponentDesign design;

    public ClientContainerView container;
    public SimplexConstraints<Integer> constraints;

    public boolean defaultComponent = false;

    public final boolean show;

    ClientComponentView(DataInputStream inStream, Collection<ClientContainerView> containers) throws IOException, ClassNotFoundException {

        compID = inStream.readInt();

        design = (ComponentDesign) new ObjectInputStream(inStream).readObject();

        if(!inStream.readBoolean()) {
            int containerID = inStream.readInt();
            for(ClientContainerView parent : containers)
                if(parent.getID()==containerID) {
                    container = parent;
                    break;
                }
        }

        constraints = (SimplexConstraints<Integer>) new ObjectInputStream(inStream).readObject();

        constraints.intersects = new HashMap<Integer, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            constraints.intersects.put(inStream.readInt(), (DoNotIntersectSimplexConstraint) new ObjectInputStream(inStream).readObject());
        }
        constraints.ID = compID;

        defaultComponent = inStream.readBoolean();

        show = inStream.readBoolean();
    }
}
