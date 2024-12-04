package lsfusion.server.logics.form.stat;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.WindowExpr;
import lsfusion.server.data.expr.WindowFormulaImpl;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.nvl;

public class SelectTop<T> {
    private static SelectTop NULL = new SelectTop(null, null);
    public final T selectTop;
    public final T selectOffset;
    public SelectTop(T selectTop, T selectOffset) {
        this.selectTop = selectTop;
        this.selectOffset = selectOffset;
    }

    public static <T> SelectTop<T> NULL() {
        return NULL;
    }

    public List<T> getParams() {
        List<T> params = new ArrayList<>();
        if (selectTop != null)
            params.add(selectTop);
        if(selectOffset != null)
            params.add(selectOffset);
        return params;
    }

    public WindowExpr getParamExpr(T param) {
        if(selectTop != null && param.equals(selectTop))
            return WindowExpr.limit;
        if(selectOffset != null && param.equals(selectOffset))
            return WindowExpr.offset;

        return null;
    }

    public FormulaJoinImpl getParamFormula(T param) {
        if(selectTop != null && param.equals(selectTop))
            return WindowFormulaImpl.limit;
        if(selectOffset != null && param.equals(selectOffset))
            return WindowFormulaImpl.offset;

        return null;
    }

    public ImSet<T> getParamsSet() {
        return SetFact.fromJavaSet(getParams());
    }
    public ImOrderSet<T> getParamsOrderSet() {
        return SetFact.fromJavaOrderSet(getParams());
    }
    public boolean contains(T param) {
        return getParams().contains(param);
    }
    public boolean isEmpty() {
        return getParams().isEmpty();
    }
    public int size() {
        return getParams().size();
    }

    public FormSelectTop<T> getFormSelectTop() {
        return new FormSelectTop<T>(selectTop, selectOffset, (ImOrderMap<GroupObjectEntity, T>) null, null);
    }

    public <X extends PropertyInterface> SelectTop<X> genInterfaces() {
        return mapValues(anInterface -> (X) ActionOrProperty.genInterface.apply(0));
    }

    public SelectTop<Integer> CONST() {
        return mapValues(anInterface -> 0);
    }

    public SelectTop<ValueClass> mapValues(ScriptingLogicsModule LM, List<ScriptingLogicsModule.TypedParameter> params) {
        return mapValues(anInterface -> LM.getValueClassByParamProperty((ScriptingLogicsModule.LPWithParams) anInterface, params));
    }

    public LimitOffset getLimitOffset() {
        return new LimitOffset(nvl((Integer) selectTop, 0), nvl((Integer) selectOffset, 0));
    }

    public <P> SelectTop<P> mapValues(ImRevMap<T, P> mapValues) {
        return mapValues(mapValues::get);
    }

    public <X> SelectTop<X> mapValues(Function<T, X> map) {
        return new SelectTop<X>(selectTop != null ? map.apply(selectTop) : null, selectOffset != null ? map.apply(selectOffset) : null);
    }
}
