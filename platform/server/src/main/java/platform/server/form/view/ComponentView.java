package platform.server.form.view;

import platform.interop.ComponentDesign;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class ComponentView implements ClientSerialize, ServerIdentitySerializable {

    public ComponentDesign design = new ComponentDesign();

    protected ContainerView container;

    int ID;

    public SimplexConstraints<ComponentView> constraints = new SimplexConstraints<ComponentView>();

    public boolean defaultComponent = false;

    public boolean show = true;

    public ComponentView() {

    }
    
    public ComponentView(int ID) {
        this.ID = ID;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(ID);

        new ObjectOutputStream(outStream).writeObject(design);
        
        outStream.writeBoolean(container==null);
        if(container!=null)
            outStream.writeInt(container.ID);

        new ObjectOutputStream(outStream).writeObject(constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ComponentView,DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            outStream.writeInt(intersect.getKey().ID);
            new ObjectOutputStream(outStream).writeObject(intersect.getValue());
        }

        outStream.writeBoolean(defaultComponent);

        outStream.writeBoolean(show);
    }

    public ContainerView getContainer() {
        return container;
    }

    public void setContainer(ContainerView container) {
        this.container = container;
    }

    public int getID() {
        return ID;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);
        pool.writeObject(outStream, constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ComponentView, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            outStream.writeInt(intersect.getKey().getID());
            pool.writeObject(outStream, intersect.getValue());
        }

        outStream.writeBoolean(defaultComponent);
        outStream.writeBoolean(show);
    }

    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        constraints = pool.readObject(inStream);

        constraints.intersects = new HashMap<ComponentView, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            //todo:!!
//            constraints.intersects.put(inStream.readInt(), (DoNotIntersectSimplexConstraint) pool.readObject(inStream));
        }
        constraints.ID = ID;

        defaultComponent = inStream.readBoolean();
        show = inStream.readBoolean();
    }
}
