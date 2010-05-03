package platform.server.view.form.client;

import platform.server.view.navigator.GroupObjectNavigator;
import platform.server.view.navigator.ObjectNavigator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GroupObjectImplementView extends ArrayList<ObjectImplementView> implements ClientSerialize {
    public GroupObjectNavigator view;

    public GroupObjectImplementView(GroupObjectNavigator iView) {
        view = iView;

        for(ObjectNavigator object : view)
            add(new ObjectImplementView(object));
        
        fixedClassView = view.banClassView;
    }

    public Byte fixedClassView = 0;

    public GridView gridView = new GridView();
    public ShowTypeView showTypeView = new ShowTypeView();

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
