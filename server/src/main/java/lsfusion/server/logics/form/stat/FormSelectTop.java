package lsfusion.server.logics.form.stat;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.nvl;

public class FormSelectTop<T> {

    private final static FormSelectTop NULL = new FormSelectTop(null, null, (ImOrderMap) null, null);
    public final ImOrderMap<GroupObjectEntity, T> selectTops;
    public final ImOrderMap<GroupObjectEntity, T> selectOffsets;
    private final T selectTop;
    private final T selectOffset;
    public FormSelectTop(T selectTop, T selectOffset, OrderedMap<GroupObjectEntity, T> selectTops, OrderedMap<GroupObjectEntity, T> selectOffsets) {
        this(selectTop, selectOffset, selectTops != null? MapFact.fromJavaOrderMap(selectTops) : null, selectOffsets != null ? MapFact.fromJavaOrderMap(selectOffsets) : null);
    }

    public FormSelectTop(T selectTop, T selectOffset, ImOrderMap<GroupObjectEntity, T> selectTops, ImOrderMap<GroupObjectEntity, T> selectOffsets) {
        this.selectTop = selectTop;
        this.selectOffset = selectOffset;

        this.selectTops = selectTops;
        this.selectOffsets = selectOffsets;
    }

    public static <T> FormSelectTop<T> NULL() {
        return NULL;
    }

    public List<T> getParams() {
        List<T> params = new ArrayList<>();
        if(selectTop != null)
            params.add(selectTop);
        if(selectOffset != null)
            params.add(selectOffset);
        if(selectTops != null)
            ListFact.addJavaAll(selectTops.valuesList(), params);
        if (selectOffsets != null)
            ListFact.addJavaAll(selectOffsets.valuesList(), params);
        return params;
    }

    public <X> FormSelectTop<X> mapParams(ImOrderSet<X> orderInterfaces) {
        X mapSelectTop = null; X mapSelectOffset = null; ImOrderMap<GroupObjectEntity, X> mapSelectTops = null; ImOrderMap<GroupObjectEntity, X> mapSelectOffsets = null;

        int n = orderInterfaces.size() - getParams().size();
        if(selectTop != null)
            mapSelectTop = orderInterfaces.get(n++);
        if(selectOffset != null)
            mapSelectOffset = orderInterfaces.get(n++);
        if(selectTops != null) {
            int fn = n;
            mapSelectTops = selectTops.mapOrderValues((int i) -> orderInterfaces.get(fn+i));
            n += selectTops.size();
        }
        if (selectOffsets != null) {
            int fn = n;
            mapSelectOffsets = selectOffsets.mapOrderValues((int i) -> orderInterfaces.get(fn+i));
            n += selectOffsets.size();
        }
        return new FormSelectTop<>(mapSelectTop, mapSelectOffset, mapSelectTops, mapSelectOffsets);
    }

    public FormSelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        return mapValues(anInterface -> LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) anInterface, params));
    }

    public LimitOffset getLimitOffset(GroupObjectEntity group) {
        return getSelectTop(group).getLimitOffset();
    }

    public FormSelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context) {
        return mapValues(anInterface -> (Integer) context.getKeyObject((ClassPropertyInterface) anInterface));
    }

    public <P> FormSelectTop<P> mapValues(ImRevMap<T, P> mapValues) {
        return mapValues(mapValues::get);
    }

    public SelectTop<T> getSelectTop(GroupObjectEntity group) {
        return new SelectTop<>(BaseUtils.nvl(selectTops != null ? selectTops.get(group) : null, selectTop), BaseUtils.nvl(selectOffsets != null ? selectOffsets.get(group) : null, selectOffset));
    }

    private <X> FormSelectTop<X> mapValues(Function<T, X> map) {
        return new FormSelectTop<>(selectTop != null ? map.apply(selectTop) : null,
                                   selectOffset != null ? map.apply(selectOffset) : null,
                                   selectTops != null ? selectTops.mapOrderValues(map) : null,
                                   selectOffsets != null ? selectOffsets.mapOrderValues(map) : null);
    }
}
