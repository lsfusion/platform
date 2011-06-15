package platform.gwt.view.classes;

import com.smartgwt.client.types.ListGridFieldType;

public class GStringType extends GDataType {
    @Override
    public ListGridFieldType getFieldType() {
        return ListGridFieldType.TEXT;
    }
}
