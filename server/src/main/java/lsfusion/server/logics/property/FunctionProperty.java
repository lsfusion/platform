package lsfusion.server.logics.property;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.data.expr.query.AggrType;
import lsfusion.server.logics.property.infer.ExClassSet;
import lsfusion.server.logics.property.infer.InferType;
import lsfusion.server.logics.property.infer.Inferred;
import lsfusion.server.session.StructChanges;

// свойство производное от остальных свойств
public abstract class FunctionProperty<T extends PropertyInterface> extends AggregateProperty<T> {

    protected FunctionProperty(String caption, ImOrderSet<T> interfaces) {
        super(caption, interfaces);
    }

    public static void fillDepends(MSet<CalcProperty> depends, ImCol<? extends CalcPropertyInterfaceImplement> propImplements) {
        for(CalcPropertyInterfaceImplement propImplement : propImplements)
            propImplement.mapFillDepends(depends);
    }

    public ImSet<CalcProperty> calculateUsedChanges(StructChanges propChanges) {
        return propChanges.getUsedChanges(getDepends());
    }
    
    public <I extends PropertyInterface> Inferred<I> inferInnerInterfaceClasses(ImList<CalcPropertyInterfaceImplement<I>> used, final boolean isSelect, final ExClassSet commonValue, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, int skipNotNull, InferType inferType) {
        ImList<ExClassSet> valueClasses = ListFact.toList(used.size(), new GetIndex<ExClassSet>() {
            public ExClassSet getMapValue(int i) {
                return isSelect && i == 0 ? commonValue : ExClassSet.notNull(commonValue);
            }});
        return inferInnerInterfaceClasses(used, orders, ordersNotNull, skipNotNull, valueClasses, inferType);
    }

    public <I extends PropertyInterface> Inferred<I> inferInnerInterfaceClasses(ImList<CalcPropertyInterfaceImplement<I>> used, ImOrderMap<CalcPropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, int skipNotNull, ImList<ExClassSet> valueClasses, InferType inferType) {
        return op(used.addList(orders.keyOrderSet()), valueClasses.addList(ListFact.toList(ExClassSet.NULL, orders.size())),
                used.size() + (ordersNotNull ? orders.size() : 0), skipNotNull, inferType, false);
    }

    public <I extends PropertyInterface> ExClassSet inferInnerValueClass(ImList<CalcPropertyInterfaceImplement<I>> used, ImMap<I, ExClassSet> inferred, AggrType aggrType, InferType inferType) {
        ExClassSet valueClass = used.get(aggrType.getMainIndex()).mapInferValueClass(inferred, inferType);
        if(aggrType.isSelect())
            return valueClass;
        
        return ExClassSet.toExType(aggrType.getType(ExClassSet.fromExType(valueClass)));
    }

}
