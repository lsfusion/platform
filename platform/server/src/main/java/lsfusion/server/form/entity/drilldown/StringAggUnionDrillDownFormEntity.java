package lsfusion.server.form.entity.drilldown;

import lsfusion.server.data.expr.StringAggUnionProperty;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.property.PropertyInterface;

public class StringAggUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<StringAggUnionProperty.Interface, StringAggUnionProperty> {

    public StringAggUnionDrillDownFormEntity(String sID, String caption, StringAggUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
