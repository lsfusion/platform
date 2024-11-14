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
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static lsfusion.base.BaseUtils.nvl;

public class SelectTop<T> {
    private static SelectTop NULL = new SelectTop(null, null);
    public static <T> SelectTop<T> NULL() {
        return NULL;
    }

    public final T selectTop;
    public final T selectOffset;

    public final ImOrderMap<GroupObjectEntity, T> selectTops;
    public final ImOrderMap<GroupObjectEntity, T> selectOffsets;

    public SelectTop(T selectTop, T selectOffset) {
        this(selectTop, selectOffset, null, null);
    }

    public SelectTop(T selectTop, T selectOffset, ImOrderMap<GroupObjectEntity, T> selectTops, ImOrderMap<GroupObjectEntity, T> selectOffsets) {
        this.selectTop = selectTop;
        this.selectTops = selectTops;
        this.selectOffset = selectOffset;
        this.selectOffsets = selectOffsets;
    }

    public List<T> getParams() {
        List<T> params = new ArrayList<>();
        if (selectTop != null) {
            params.add(selectTop);
            if(selectOffset != null) {
                params.add(selectOffset);
            }
        } else if (selectTops != null) {
            params.addAll(selectTops.values().toJavaCol());
            if(selectOffsets != null) {
                params.addAll(selectOffsets.values().toJavaCol());
            }
        }
        return params;
    }

    public SelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        return mapValues(
                () -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTop, params),
                () -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectOffset, params),
                i -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTops.getValue(i), params),
                i -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectOffsets.getValue(i), params)
                );
    }

    public SelectTop<ClassPropertyInterface> mapValues(ImOrderSet<ClassPropertyInterface> orderInterfaces, int extraParams) {
        return mapValues(
                () -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams),
                () -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + 1),
                i -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + i),
                i -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + i + selectTops.size()));
    }

    public SelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context) {
        return mapValues(
                () -> (T) context.getKeyObject((ClassPropertyInterface) selectTop),
                () -> (T) context.getKeyObject((ClassPropertyInterface) selectOffset),
                i -> (T) context.getKeyObject((ClassPropertyInterface) selectTops.getValue(i)),
                i -> (T) context.getKeyObject((ClassPropertyInterface) selectOffsets.getValue(i)));
    }

    public static LimitOffset getLimitOffset(SelectTop<Integer> st, GroupObjectEntity group) {
        return new LimitOffset(nvl(st.selectTops != null && group != null ? nvl(st.selectTops.get(group), st.selectTop) : st.selectTop, 0),
                nvl(st.selectOffsets != null && group != null ? nvl(st.selectOffsets.get(group), st.selectOffset) : st.selectOffset, 0));
    }

    public SelectTop mapValues(Supplier<T> selectTopSupplier, Supplier<T> selectOffsetSupplier, Function<Integer, T> selectTopsSupplier, Function<Integer, T> selectOffsetsSupplier) {
        if (selectTop != null) {
            return new SelectTop(selectTopSupplier.get(), selectOffset != null ? selectOffsetSupplier.get() : null);
        } else if (selectTops != null) {
            MOrderMap<GroupObjectEntity, T> mSelectTops = MapFact.mOrderMap();
            for (int i = 0; i < selectTops.size(); i++) {
                mSelectTops.add(selectTops.getKey(i), selectTopsSupplier.apply(i));
            }
            MOrderMap<GroupObjectEntity, T> mSelectOffsets = MapFact.mOrderMap();
            if(selectOffsets != null) {
                for (int i = 0; i < selectOffsets.size(); i++) {
                    mSelectOffsets.add(selectOffsets.getKey(i), selectOffsetsSupplier.apply(i));
                }
            }
            return new SelectTop(null, null, mSelectTops.immutableOrder(), mSelectOffsets.immutableOrder());
        } else {
            return NULL();
        }
    }
}
