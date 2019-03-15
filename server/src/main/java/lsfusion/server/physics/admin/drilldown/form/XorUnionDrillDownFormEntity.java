package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.classes.data.XorUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class XorUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<XorUnionProperty.Interface, XorUnionProperty> {

    public XorUnionDrillDownFormEntity(String sID, LocalizedString caption, XorUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}
