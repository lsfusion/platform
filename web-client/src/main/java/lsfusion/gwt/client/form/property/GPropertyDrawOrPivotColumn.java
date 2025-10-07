package lsfusion.gwt.client.form.property;

import lsfusion.gwt.client.form.object.GGroupObject;

import java.io.Serializable;
import java.util.Map;

public interface GPropertyDrawOrPivotColumn extends Serializable {
    boolean equalsGroupObject(GGroupObject group);
    String getCaption(Map<GPropertyDraw, String> columnCaptionMap);
}
