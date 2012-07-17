package platform.gwt.view2.classes;

import java.io.Serializable;

public abstract class GType implements Serializable {
    public Object parseString(String strValue) {
        return strValue;
    }
}
