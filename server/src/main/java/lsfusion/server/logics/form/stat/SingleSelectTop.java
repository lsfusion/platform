package lsfusion.server.logics.form.stat;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class SingleSelectTop<T> extends SelectTop<T> {
    private static SingleSelectTop NULL = new SingleSelectTop(null, null);
    public static <T> SingleSelectTop<T> NULL() {
        return NULL;
    }

    public final T selectTop;
    public final T selectOffset;

    public SingleSelectTop(T selectTop, T selectOffset) {
        this.selectTop = selectTop;
        this.selectOffset = selectOffset;
    }

    public List<T> getParams() {
        List<T> params = new ArrayList<>();
        if (selectTop != null) {
            params.add(selectTop);
            if(selectOffset != null) {
                params.add(selectOffset);
            }
        }
        return params;
    }

    @Override
    public SingleSelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        return mapValues(
                () -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTop, params),
                () -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectOffset, params));
    }

    @Override
    public SingleSelectTop<ClassPropertyInterface> mapValues(ImOrderSet<ClassPropertyInterface> orderInterfaces, int extraParams) {
        return mapValues(
                () -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams),
                () -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + 1));
    }

    @Override
    public SingleSelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context) {
        return mapValues(
                () -> (T) context.getKeyObject((ClassPropertyInterface) selectTop),
                () -> (T) context.getKeyObject((ClassPropertyInterface) selectOffset));
    }

    @Override
    public LimitOffset getLimitOffset(GroupObjectEntity group) {
        return new LimitOffset(nvl((Integer) selectTop, 0), nvl((Integer) selectOffset, 0));
    }

    @Override
    public ImOrderSet<T> getWindowInterfaces(GroupObjectEntity group) {
        MOrderSet windowInterfaces = SetFact.mOrderSet();
        if (selectTop != null) {
            windowInterfaces.add(selectTop);
            if (selectOffset != null) {
                windowInterfaces.add(selectOffset);
            }
        }
        return windowInterfaces.immutableOrder();
    }

    public SingleSelectTop mapValues(Supplier<T> selectTopSupplier, Supplier<T> selectOffsetSupplier) {
        if (selectTop != null) {
            return new SingleSelectTop(selectTopSupplier.get(), selectOffset != null ? selectOffsetSupplier.get() : null);
        } else {
            return NULL();
        }
    }
}
