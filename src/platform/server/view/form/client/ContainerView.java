package platform.server.view.form.client;

import java.awt.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.List;

public class ContainerView extends ComponentView {

    int ID;

    static int idCount = 0;
    public ContainerView() {
        ID = idCount++;
    }

    public String title;

    LayoutManager layout;

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);

        outStream.writeInt(ID);
        outStream.writeBoolean(title==null);
        if(title!=null)
            outStream.writeUTF(title);
        new ObjectOutputStream(outStream).writeObject(layout);
    }

    public void fillOrderList(List<ContainerView> containers) {
        if(container!=null) container.fillOrderList(containers);
        if(!containers.contains(this)) containers.add(this);
    }
}
