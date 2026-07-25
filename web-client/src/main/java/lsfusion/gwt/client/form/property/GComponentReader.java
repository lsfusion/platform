package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.form.design.GComponent;

public interface GComponentReader extends GPropertyReader {
    GComponent getReaderComponent();

    @Override
    default GComponent getAttributeComponent(GForm form) { return getReaderComponent(); } // a container's reader carries the component itself
}
