package lsfusion.server.form.entity.drilldown;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.XorUnionProperty;

public class XorUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<XorUnionProperty.Interface, XorUnionProperty> {

    public XorUnionDrillDownFormEntity(String sID, String caption, XorUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}
