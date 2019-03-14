package lsfusion.server.physics.admin.drilldown;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.classes.data.SumUnionProperty;

public class SumUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<SumUnionProperty.Interface, SumUnionProperty> {

    public SumUnionDrillDownFormEntity(String sID, LocalizedString caption, SumUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}
