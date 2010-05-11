package platform.server.view.form.client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ContainerView extends ComponentView {

    public String title;

    public ContainerView(int ID) {
        super(ID);
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(ID);
        outStream.writeBoolean(title==null);
        if(title!=null)
            outStream.writeUTF(title);
    }

    public void fillOrderList(List<ContainerView> containers) {
        if(container!=null) container.fillOrderList(containers);
        if(!containers.contains(this)) containers.add(this);
    }
}
