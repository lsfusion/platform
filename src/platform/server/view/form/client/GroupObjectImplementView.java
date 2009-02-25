package platform.server.view.form.client;

import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;

import java.util.ArrayList;
import java.io.DataOutputStream;
import java.io.IOException;

public class GroupObjectImplementView extends ArrayList<ObjectImplementView> implements ClientSerialize {
    public GroupObjectImplement view;

    public GroupObjectImplementView(GroupObjectImplement iView) {
        view = iView;

        for(ObjectImplement object : view)
            add(new ObjectImplementView(object));
        
        singleViewType = view.singleViewType;
    }

    public Boolean singleViewType = false;

    public GridView gridView = new GridView();
    public FunctionView addView = new FunctionView();
    public FunctionView changeClassView = new FunctionView();
    public FunctionView delView = new FunctionView();

    public void serialize(DataOutputStream outStream) throws IOException {
        outStream.writeInt(view.ID);
        outStream.writeBoolean(singleViewType);

        outStream.writeInt(size());
        for(ObjectImplementView object : this)
            object.serialize(outStream);

        gridView.serialize(outStream);
        addView.serialize(outStream);
        changeClassView.serialize(outStream);
        delView.serialize(outStream);
    }
}
