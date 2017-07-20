package lsfusion.server.form.entity.drilldown;

import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.PropertyInterface;

public class StringAggUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<StringAggUnionProperty.Interface, StringAggUnionProperty> {

    public StringAggUnionDrillDownFormEntity(String sID, LocalizedString caption, StringAggUnionProperty property, LogicsModule LM) {
        super(sID, caption, property, LM);
    }
}
