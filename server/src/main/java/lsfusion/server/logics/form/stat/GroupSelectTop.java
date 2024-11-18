package lsfusion.server.logics.form.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderSet;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.nvl;

public class GroupSelectTop<T> extends SelectTop<T> {
    private static GroupSelectTop NULL = new GroupSelectTop(null, null);
    public static <T> GroupSelectTop<T> NULL() {
        return NULL;
    }

    public final ImOrderMap<GroupObjectEntity, T> selectTops;
    public final ImOrderMap<GroupObjectEntity, T> selectOffsets;

    public GroupSelectTop(ImOrderMap<GroupObjectEntity, T> selectTops, ImOrderMap<GroupObjectEntity, T> selectOffsets) {
        this.selectTops = selectTops;
        this.selectOffsets = selectOffsets;
    }

    public List<T> getParams() {
        List<T> params = new ArrayList<>(selectTops.values().toJavaCol());
        if (selectOffsets != null) {
            params.addAll(selectOffsets.values().toJavaCol());
        }
        return params;
    }

    @Override
    public GroupSelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        return mapValues(
                i -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTops.getValue(i), params),
                i -> (T) LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectOffsets.getValue(i), params)
                );
    }

    @Override
    public GroupSelectTop<ClassPropertyInterface> mapValues(ImOrderSet<ClassPropertyInterface> orderInterfaces, int extraParams) {
        return mapValues(
                i -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + i),
                i -> (T) orderInterfaces.get(orderInterfaces.size() - extraParams + i + selectTops.size()));
    }

    @Override
    public GroupSelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context) {
        return mapValues(
                i -> (T) context.getKeyObject((ClassPropertyInterface) selectTops.getValue(i)),
                i -> (T) context.getKeyObject((ClassPropertyInterface) selectOffsets.getValue(i)));
    }

    @Override
    public LimitOffset getLimitOffset(GroupObjectEntity group) {
        Integer limit = group != null ? (Integer) selectTops.get(group) : null;
        Integer offset = selectOffsets != null && group != null ? (Integer) selectOffsets.get(group) : null;
        return new LimitOffset(nvl(limit, 0), nvl(offset, 0));
    }

    @Override
    public ImOrderSet<T> getWindowInterfaces(GroupObjectEntity group) {
        MOrderSet windowInterfaces = SetFact.mOrderSet();
        windowInterfaces.add(selectTops.get(group));
        T offset = selectOffsets.get(group);
        if (offset != null) {
            windowInterfaces.add(offset);
        }
        return windowInterfaces.immutableOrder();
    }

    public GroupSelectTop mapValues(Function<Integer, T> selectTopsSupplier, Function<Integer, T> selectOffsetsSupplier) {
        MOrderMap<GroupObjectEntity, T> mSelectTops = MapFact.mOrderMap();
        for (int i = 0; i < selectTops.size(); i++) {
            mSelectTops.add(selectTops.getKey(i), selectTopsSupplier.apply(i));
        }
        MOrderMap<GroupObjectEntity, T> mSelectOffsets = MapFact.mOrderMap();
        if (selectOffsets != null) {
            for (int i = 0; i < selectOffsets.size(); i++) {
                mSelectOffsets.add(selectOffsets.getKey(i), selectOffsetsSupplier.apply(i));
            }
        }
        return new GroupSelectTop(mSelectTops.immutableOrder(), mSelectOffsets.immutableOrder());
    }
}
