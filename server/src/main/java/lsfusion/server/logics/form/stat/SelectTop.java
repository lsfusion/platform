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
        List<T> mapping = new ArrayList<>();
        if (selectTop != null)
            mapping.add(selectTop);
        if (selectTops != null)
            mapping.addAll(selectTops.values().toJavaCol());
        return mapping;
    }

    public SelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        if (selectTop != null) {
            return new SelectTop(LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTop, params));
        } else if (selectTops != null) {
            MOrderMap<GroupObjectEntity, ValueClass> mSelectTops = MapFact.mOrderMap();
            for (int i = 0; i < selectTops.size(); i++) {
                mSelectTops.add(selectTops.getKey(i), LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) selectTops.getValue(i), params));
            }
            return new SelectTop(null, mSelectTops.immutableOrder());
        } else {
            return NULL;
        }
    }

    public SelectTop<ClassPropertyInterface> mapValues(ImOrderSet<ClassPropertyInterface> orderInterfaces, int extraParams) {
        if (selectTop != null) {
            return new SelectTop(orderInterfaces.get(orderInterfaces.size() - extraParams));
        } else if (selectTops != null) {
            MOrderMap<GroupObjectEntity, ClassPropertyInterface> mSelectTops = MapFact.mOrderMap();
            for (int i = 0; i < selectTops.size(); i++) {
                mSelectTops.add(selectTops.getKey(i), orderInterfaces.get(orderInterfaces.size() - extraParams + i));
            }
            return new SelectTop(null, mSelectTops.immutableOrder());
        } else {
            return NULL;
        }
    }

    public SelectTop<Integer> mapValues(ExecutionContext<ClassPropertyInterface> context) {
        if(selectTop != null) {
            return new SelectTop(context.getKeyObject((ClassPropertyInterface) selectTop));
        } else if(selectTops != null) {
            ImOrderMap<GroupObjectEntity, ClassPropertyInterface> selectTopsInt = (ImOrderMap<GroupObjectEntity, ClassPropertyInterface>) selectTops;
            MOrderMap<GroupObjectEntity, Integer> mSelectTops = MapFact.mOrderMap();
            for (int i = 0; i < selectTopsInt.size(); i++) {
                mSelectTops.add(selectTopsInt.getKey(i), (Integer) context.getKeyObject(selectTopsInt.getValue(i)));
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
