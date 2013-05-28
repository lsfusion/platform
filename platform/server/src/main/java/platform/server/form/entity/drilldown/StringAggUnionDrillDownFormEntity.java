package platform.server.form.entity.drilldown;

import platform.server.data.expr.StringAggUnionProperty;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.PropertyInterface;

public class StringAggUnionDrillDownFormEntity<I extends PropertyInterface> extends UnionDrillDownFormEntity<StringAggUnionProperty.Interface, StringAggUnionProperty> {

    public StringAggUnionDrillDownFormEntity(String sID, String caption, StringAggUnionProperty property, BusinessLogics BL) {
        super(sID, caption, property, BL);
    }
}
