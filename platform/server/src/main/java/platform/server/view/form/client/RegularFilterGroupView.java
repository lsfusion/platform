package platform.server.view.form.client;

import platform.server.view.navigator.RegularFilterGroupNavigator;
import platform.server.view.navigator.RegularFilterNavigator;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class RegularFilterGroupView extends FunctionView {
    public RegularFilterGroupNavigator view;

    public RegularFilterGroupView(int ID, RegularFilterGroupNavigator view) {
        super(ID);
        this.view = view;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(view.ID);

        outStream.writeInt(view.filters.size());
        for(RegularFilterNavigator filter : view.filters) {
            outStream.writeInt(filter.ID);
            outStream.writeUTF(filter.name);

            new ObjectOutputStream(outStream).writeObject(filter.key);
            outStream.writeBoolean(filter.showKey);
        }

        outStream.writeInt(view.defaultFilter);
    }
}
