package lsfusion.server.logics.action;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.logics.property.derived.DerivedProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class SystemAction extends BaseAction<PropertyInterface> {

    protected SystemAction(LocalizedString caption, ImOrderSet<PropertyInterface> interfaces) {
        super(caption, interfaces);
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        // TRUE AND a OR (NOT a), т.е. значение всегда TRUE, но при join'е будет учавствовать в classWhere - FULL
        MList<PropertyInterfaceImplement<PropertyInterface>> mProps = ListFact.mList(interfaces.size() * 2);
        for (PropertyInterface i : interfaces) {
            mProps.add(DerivedProperty.createAnd(DerivedProperty.createTrue(), i));
            mProps.add(DerivedProperty.createNot(i));
        }
        return DerivedProperty.createUnion(interfaces, mProps.immutableList());
    }
}
