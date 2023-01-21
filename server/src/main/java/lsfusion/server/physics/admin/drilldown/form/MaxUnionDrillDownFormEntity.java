package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.classes.data.MaxUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class MaxUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<MaxUnionProperty.Interface, MaxUnionProperty> {

    public MaxUnionDrillDownFormEntity(LocalizedString caption, MaxUnionProperty property, BaseLogicsModule LM) {
        super(caption, property, LM);
    }
}
