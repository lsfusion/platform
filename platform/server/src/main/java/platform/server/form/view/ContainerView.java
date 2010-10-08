package platform.server.form.view;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ContainerView extends ComponentView {

    public String title;
    public String description;

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

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(ID);
        outStream.writeBoolean(title==null);
        if(title!=null)
            outStream.writeUTF(title);
        outStream.writeBoolean(description==null);
        if (description!=null)
            outStream.writeUTF(description);
    }

    public void fillOrderList(List<ContainerView> containers) {
        if(container!=null) container.fillOrderList(containers);
        if(!containers.contains(this)) containers.add(this);
    }
}
