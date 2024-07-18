package lsfusion.server.logics.form.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class SelectTop<T> {
    public static SelectTop NULL = new SelectTop(null, null);

    public final T selectTop;
    public final ImOrderMap<GroupObjectEntity, T> selectTops;

    public SelectTop(T selectTop) {
        this(selectTop, null);
    }

    public SelectTop(T selectTop, ImOrderMap<GroupObjectEntity, T> selectTops) {
        this.selectTop = selectTop;
        this.selectTops = selectTops;
    }

    public List<T> getParams() {
        if (selectTop != null)
            return Collections.singletonList(selectTop);
        else if (selectTops != null)
            return new ArrayList<>(selectTops.values().toJavaCol());
        else return new ArrayList<>();
    }

    public SelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        return mapValues(() -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTop, params), i -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTops.getValue(i), params));
    }

    public SelectTop<ClassPropertyInterface> mapValues(ImOrderSet<ClassPropertyInterface> orderInterfaces, int extraParams) {
        return mapValues(() -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams), i -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + i));
    }

    public SelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context) {
        return mapValues(() -> (T) context.getKeyObject((ClassPropertyInterface) selectTop), i -> (T) context.getKeyObject((ClassPropertyInterface) selectTops.getValue(i)));
    }

    public SelectTop mapValues(Supplier<T> selectTopSupplier, Function<Integer, T> selectTopsSupplier) {
        if (selectTop != null) {
            return new SelectTop(selectTopSupplier.get());
        } else if (selectTops != null) {
            MOrderMap<GroupObjectEntity, T> mSelectTops = MapFact.mOrderMap();
            for (int i = 0; i < selectTops.size(); i++) {
                mSelectTops.add(selectTops.getKey(i), selectTopsSupplier.apply(i));
            }
            return new SelectTop(null, mSelectTops.immutableOrder());
        } else {
            return NULL;
        }
    }

    public int get(GroupObjectEntity group) {
        return (int) nvl(selectTops != null && group != null ? nvl(selectTops.get(group), selectTop) : selectTop, 0);
    }
}
