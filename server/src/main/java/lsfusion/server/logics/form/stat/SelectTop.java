package lsfusion.server.logics.form.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.mutable.MOrderMap;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.base.BaseUtils.nvl;

public class SelectTop<T> {
    public static SelectTop NULL = new SelectTop(0, null);

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
        List<T> mapping = new ArrayList<>();
        if (selectTop != null)
            mapping.add(selectTop);
        if (selectTops != null)
            mapping.addAll(selectTops.values().toJavaCol());
        return mapping;
    }

    public SelectTop<ValueClass> mapValueClass(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        ValueClass selectTop = this.selectTop != null ? LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) this.selectTop, params) : null;
        MOrderMap<GroupObjectEntity, ValueClass> mSelectTops = null;
        if(this.selectTops != null) {
            mSelectTops = MapFact.mOrderMap();
            for(int i = 0; i < this.selectTops.size(); i++) {
                mSelectTops.add(this.selectTops.getKey(i), LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) this.selectTops.getValue(i), params));
            }
        }
        return new SelectTop<>(selectTop, mSelectTops != null ? mSelectTops.immutableOrder() : null);
    }

    public int get(GroupObjectEntity group) {
        return (int) nvl(selectTops != null && group != null ? nvl(selectTops.get(group), selectTop) : selectTop, 0);
    }
}
