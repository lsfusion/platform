package lsfusion.server.physics.admin.drilldown.form;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.property.classes.data.StringAggUnionProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class StringAggUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<StringAggUnionProperty.Interface, StringAggUnionProperty> {

    public StringAggUnionDrillDownFormEntity(LocalizedString caption, StringAggUnionProperty property, LogicsModule LM) {
        super(caption, property, LM);
    }
}
