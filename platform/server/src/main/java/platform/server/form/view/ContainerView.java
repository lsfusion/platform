package platform.server.form.view;

import platform.server.serialization.ServerSerializationPool;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContainerView extends ComponentView {

    public String title;
    public String description;

    public ContainerView() {

    }
    
    public ContainerView(int ID) {
        super(ID);

        // по умолчанию, контейнеры не должны resize'ится вообще, то есть не стремится ни к максимальному размеру, ни к предпочитаемому
        constraints.fillVertical = -1;
        constraints.fillHorizontal = -1;
    }

    List<ComponentView> children = new ArrayList<ComponentView>();

    public void add(ComponentView comp) {

        if (comp.getContainer() != null)
            comp.getContainer().remove(comp);

        comp.setContainer(this);
        children.add(comp);
    }

    private void remove(ComponentView comp) {
        comp.setContainer(null);
        children.remove(comp);
    }

    public void fillOrderList(List<ContainerView> containers) {
        if(container!=null) container.fillOrderList(containers);
        if(!containers.contains(this)) containers.add(this);
    }

    @Override
    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        super.customSerialize(pool, outStream, serializationType);

        pool.serializeCollection(outStream, children);

        pool.writeString(outStream, title);
        pool.writeString(outStream, description);
    }

    @Override
    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        super.customDeserialize(pool, iID, inStream);

        children = pool.deserializeList(inStream);

        title = pool.readString(inStream);
        description = pool.readString(inStream);
    }
}
