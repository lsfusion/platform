package platform.server.view.form.client;

import platform.server.view.navigator.GroupObjectNavigator;
import platform.server.view.navigator.ObjectNavigator;
import platform.base.IDGenerator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectImplementView extends ArrayList<ObjectImplementView> implements ClientSerialize {
    public GroupObjectNavigator view;

    public GroupObjectImplementView(IDGenerator idGen, GroupObjectNavigator iView) {
        view = iView;

        for(ObjectNavigator object : view)
            add(new ObjectImplementView(idGen, object));
        
        fixedClassView = view.banClassView;

        gridView = new GridView(idGen.genID());
        showTypeView = new ShowTypeView(idGen.genID());
    }

    public Byte fixedClassView = 0;

    public GridView gridView;
    public ShowTypeView showTypeView;

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(view.ID);
        outStream.writeByte(fixedClassView);

        outStream.writeInt(size());
        for(ObjectImplementView object : this)
            object.serialize(outStream);

        gridView.serialize(outStream);
        showTypeView.serialize(outStream);
    }
}
