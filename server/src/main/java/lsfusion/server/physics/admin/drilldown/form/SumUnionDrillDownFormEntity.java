package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.classes.data.SumUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class SumUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<SumUnionProperty.Interface, SumUnionProperty> {

    public SumUnionDrillDownFormEntity(LocalizedString caption, SumUnionProperty property, LogicsModule LM) {
        super(caption, property, LM);
    }
}
