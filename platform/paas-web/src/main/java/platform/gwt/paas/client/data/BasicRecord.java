package platform.gwt.paas.client.data;

import com.google.gwt.core.client.JavaScriptObject;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import platform.gwt.base.shared.GwtSharedUtils;

public class BasicRecord extends ListGridRecord {
    public static final String ID_FIELD = "id";
    public static final String NAME_FIELD = "name";
    public static final String DESCRIPTION_FIELD = "description";

    public BasicRecord() {
        super();
    }

    public BasicRecord(JavaScriptObject jsObj) {
        super(jsObj);
    }

    public BasicRecord(int id) {
        setId(id);
    }

    public BasicRecord(int id, String name, String description) {
        this(id);

        setName(GwtSharedUtils.rtrim(name));
        setDescription(description);
    }

    public void setId(int id) {
        setAttribute(ID_FIELD, id);
    }

    public int getId() {
        return getAttributeAsInt(ID_FIELD);
    }

    public void setName(String icon) {
        setAttribute(NAME_FIELD, icon);
    }

    public String getName() {
        return getAttributeAsString(NAME_FIELD);
    }

    public void setDescription(String description) {
        setAttribute(DESCRIPTION_FIELD, description);
    }

    public String getDescription() {
        return getAttributeAsString(DESCRIPTION_FIELD);
    }

    public static int[] getIDs(ListGridRecord[] modules) {
        int moduleIds[] = new int[modules.length];
        for (int i = 0; i < modules.length; i++) {
            BasicRecord module = (BasicRecord) modules[i];
            moduleIds[i] = module.getId();
        }
        return moduleIds;
    }
}
