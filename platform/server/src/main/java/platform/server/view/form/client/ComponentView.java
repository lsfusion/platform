package platform.server.view.form.client;

import platform.interop.form.layout.SimplexConstraints;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

public class ComponentView implements ClientSerialize {

    int ID;
    public ComponentView(int ID) {
        this.ID = ID;
    }

    public ContainerView container;
    public SimplexConstraints<ComponentView> constraints = new SimplexConstraints<ComponentView>();

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(ID);
        outStream.writeBoolean(container==null);
        if(container!=null)
            outStream.writeInt(container.ID);

        new ObjectOutputStream(outStream).writeObject(constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ComponentView,DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            outStream.writeInt(intersect.getKey().ID);
            new ObjectOutputStream(outStream).writeObject(intersect.getValue());
        }
    }
}
