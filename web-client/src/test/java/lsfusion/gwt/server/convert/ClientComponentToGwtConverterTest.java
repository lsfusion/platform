package lsfusion.gwt.server.convert;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.property.ClassViewType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// A group reaches the web client in the view it is in, whichever it is: a form with a group in POPUP view
// (OBJECTS i = Item POPUP) did not open in the web client, whose view types had no POPUP (#1890)
public class ClientComponentToGwtConverterTest {

    private static GGroupObject convert(ClassViewType viewType) {
        ClientGroupObject group = new ClientGroupObject();
        group.ID = 1;
        group.viewType = viewType;
        group.listViewType = ListViewType.DEFAULT;
        return new ClientComponentToGwtConverter(null, new FormSessionObject(null, null, "navigator")).convertOrCast(group);
    }

    // ... and is a list - is sent rows - in the web client exactly when it is one for the server and the desktop client
    @Test
    public void everyViewTypeOfAGroupConverts() {
        for (ClassViewType viewType : ClassViewType.values()) {
            GGroupObject group = convert(viewType);
            assertEquals(viewType.name(), group.viewType.name());
            assertEquals(viewType.name(), viewType.isList(), group.viewType.isList());
        }
    }
}
