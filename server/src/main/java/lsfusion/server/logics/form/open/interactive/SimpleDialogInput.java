package lsfusion.server.logics.form.open.interactive;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.type.Type;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.form.interactive.action.input.SimpleDataInput;
import lsfusion.server.logics.form.interactive.action.input.SimpleRequestInput;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class SimpleDialogInput<T extends PropertyInterface> extends SimpleRequestInput<T> {

    public final CustomClass customClass;
    public final LP targetProp;

    public final InputListEntity<?, T> list;

    public SimpleDialogInput(CustomClass customClass, LP targetProp, InputListEntity<?, T> list) {
        this.customClass = customClass;
        this.list = list;
        this.targetProp = targetProp;

        list.singleInterface();
    }

    private <P extends PropertyInterface> SimpleDialogInput<P> override(InputListEntity<?, P> list) {
        return new SimpleDialogInput<>(customClass, targetProp, list);
    }

    public SimpleDialogInput<T> newSession() {
        return override(list != null ? list.newSession() : null);
    }

    @Override
    public <P extends PropertyInterface> SimpleDialogInput<P> map(ImRevMap<T, P> mapping) {
        return override(list != null ? list.map(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> SimpleDialogInput<P> mapInner(ImRevMap<T, P> mapping) {
        return override(list != null ? list.mapInner(mapping) : null);
    }

    @Override
    public <P extends PropertyInterface> SimpleDialogInput<P> mapJoin(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        return override(list != null ? list.mapJoin(mapping) : null);
    }

    @Override
    public SimpleRequestInput<T> merge(SimpleRequestInput<T> input) {
        return null; // // later it maybe makes sense to "or" simpledialog lists and classes
    }
}
