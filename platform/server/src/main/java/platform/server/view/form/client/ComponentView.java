package platform.server.view.form.client;

import platform.interop.ComponentDesign;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

public class ComponentView implements ClientSerialize {

    int ID;
    public ComponentView(int ID) {
        this.ID = ID;
    }

    public ComponentDesign design = new ComponentDesign();

    protected ContainerView container;
    public ContainerView getContainer() {
        return container;
    }
    public void setContainer(ContainerView container) {
        this.container = container;
    }
    
    public SimplexConstraints<ComponentView> constraints = new SimplexConstraints<ComponentView>();

    public boolean defaultComponent = false;

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
    }
}
