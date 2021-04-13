package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.form.open.interactive.SimpleDialogInput;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class SimpleDataInput<T extends PropertyInterface> extends SimpleRequestInput<T> {

    public final DataClass type;

    public final InputListEntity<?, T> list;

    public final LP targetProp;

    public SimpleDataInput(DataClass type, InputListEntity<?, T> list, LP targetProp) {
        this.type = type;
        this.list = list;
        this.targetProp = targetProp;
    }

    private <P extends PropertyInterface> SimpleDataInput<P> override(InputListEntity<?, P> list) {
        return new SimpleDataInput<P>(type, list, targetProp);
    }

    public SimpleDataInput<T> newSession() {
        return override(list != null ? list.newSession() : null);
    }

    @Override
    public <P extends PropertyInterface> SimpleRequestInput<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> SimpleRequestInput<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> SimpleRequestInput<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null);
    }

    @Override
    public SimpleRequestInput<T> merge(SimpleRequestInput<T> input) {
        if(!(input instanceof SimpleDataInput))
            return null;

        SimpleDataInput<T> dataInput = ((SimpleDataInput<T>)input);
        if(list != null || dataInput.list != null) // later it maybe makes sense to "or" this lists
            return null;

        DataClass compatibleType = ((DataClass<?>)type).getCompatible(dataInput.type, true);
        if(compatibleType != null && targetProp.property.equals(dataInput.targetProp.property))
            return new SimpleDataInput<T>(compatibleType, null, targetProp);
        return null;
    }
}
