package platform.server.logics.property;

import platform.server.session.MapDataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.data.where.WhereBuilder;
import platform.base.BaseUtils;

// чисто для оптимизации
public class ExclusiveUnionProperty extends OverrideUnionProperty {
    
    public ExclusiveUnionProperty(String sID, String caption, int intNum) {
        super(sID, caption, intNum);
    }

    @Override
    protected MapDataChanges<Interface> calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        MapDataChanges<Interface> result = new MapDataChanges<Interface>();
        for(PropertyMapImplement<?, Interface> operand : BaseUtils.reverse(operands))
            result = result.add(operand.mapDataChanges(change, changedWhere, modifier));
        return result;
    }
}
