package lsfusion.server.physics.admin.drilldown;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.classes.data.MaxUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class MaxUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<MaxUnionProperty.Interface, MaxUnionProperty> {

    public MaxUnionDrillDownFormEntity(String sID, LocalizedString caption, MaxUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}
