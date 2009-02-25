package platform.server.view.form.client;

import platform.server.view.form.RegularFilterGroup;
import platform.server.view.form.RegularFilter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class RegularFilterGroupView extends FunctionView {
    public RegularFilterGroup view;

    public RegularFilterGroupView(RegularFilterGroup iView) {
        view = iView;
    }

    public void serialize(DataOutputStream outStream) throws IOException {
        super.serialize(outStream);
        outStream.writeInt(view.ID);

        outStream.writeInt(view.filters.size());
        for(RegularFilter filter : view.filters) {
            outStream.writeInt(filter.ID);
            outStream.writeUTF(filter.name);

            new ObjectOutputStream(outStream).writeObject(filter.key);
        }
    }
}
