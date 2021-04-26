package lsfusion.server.logics.form.interactive.property;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

public class IntervalValueProperty extends NoIncrementProperty<ClassPropertyInterface> {

    private static IntervalClass instance = null;

    private static IntervalClass fillIntervalInstance(ClassPropertyInterface classPropertyInterface) {
        if (instance == null)
            instance = IntervalClass.getInstance(classPropertyInterface.interfaceClass.getCaption().toString().toUpperCase());
        return instance;
    }

    public IntervalValueProperty(ImOrderSet<ClassPropertyInterface> interfaces) {
        super(fillIntervalInstance(interfaces.get(0)).getCaption(), interfaces);

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        long from = ((LocalDate) (joinImplement.get(getInterface(true)).getObjectValue(null).getValue())).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
        long to = ((LocalDate) (joinImplement.get(getInterface(false)).getObjectValue(null).getValue())).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000;
        String q = from + "." + to;
        BigDecimal interval = new BigDecimal(q);
        return new ValueExpr(interval, IntervalClass.getInstance("DATE"));
    }

    private ClassPropertyInterface getInterface(boolean first) {
        return first ? interfaces.get(0) : interfaces.get(1);
    }

    @Override
    protected Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return instance.getProperty().inferInterfaceClasses(inferType);
    }

    @Override
    protected ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return new ExClassSet(instance);
    }

    @Override
    @IdentityStrongLazy
    public ActionMapImplement<?, ClassPropertyInterface> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {

        ObjectEntity objectEntity = new ObjectEntity(instance.getTypeID(), instance.getBaseClass(), instance.getCaption());
        ActionMapImplement<ClassPropertyInterface, ClassPropertyInterface> implement = objectEntity.getChangeAction().getImplement(interfaces.sort());
        return implement;
    }
}
