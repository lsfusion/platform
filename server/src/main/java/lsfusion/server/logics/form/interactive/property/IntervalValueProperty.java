package lsfusion.server.logics.form.interactive.property;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.time.IntervalClass;
import lsfusion.server.logics.classes.data.utils.time.TimeLogicsModule;
import lsfusion.server.logics.form.interactive.action.change.DefaultChangeIntervalObjectAction;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.NoIncrementProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.IsClassProperty;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Iterator;

public class IntervalValueProperty extends NoIncrementProperty<ClassPropertyInterface> {

    private final ObjectEntity objectFrom;
    private final ObjectEntity objectTo;
    private final String objectSid;
    private final ClassPropertyInterface interfaceFrom;
    private final ClassPropertyInterface interfaceTo;
    private final TimeLogicsModule timeLogicsModule;

    public IntervalValueProperty(ObjectEntity objectFrom, ObjectEntity objectTo, TimeLogicsModule timeLogicsModule) {
        super(LocalizedString.NONAME, IsClassProperty.getInterfaces(new ValueClass[]{objectFrom.baseClass, objectTo.baseClass}));

        this.objectSid = objectFrom.baseClass.getSID();
        this.objectFrom = objectFrom;
        this.objectTo = objectTo;

        Iterator<ClassPropertyInterface> iterator = getOrderInterfaces().iterator();
        this.interfaceFrom = iterator.next();
        this.interfaceTo = iterator.next();

        this.timeLogicsModule = timeLogicsModule;

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        try {
            return timeLogicsModule.findProperty(getPropertyName())
                    .getExpr(joinImplement.get(interfaceFrom),
                            joinImplement.get(interfaceTo));
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            Throwables.propagate(e);
            return null;
        }
    }

    private String getPropertyName() {
        int timeIndex = objectSid.indexOf("TIME");
        String type = timeIndex < 1 ? objectSid.toLowerCase() :
                objectSid.substring(0, timeIndex).toLowerCase() +
                        objectSid.charAt(timeIndex) +
                        objectSid.substring(timeIndex + 1).toLowerCase();
        return type + "Interval" + "[" + type.toUpperCase() + ", " + type.toUpperCase() + "]";
    }

    @Override
    protected Inferred<ClassPropertyInterface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return Inferred.EMPTY();
    }

    @Override
    public ExClassSet calcInferValueClass(ImMap<ClassPropertyInterface, ExClassSet> inferred, InferType inferType) {
        return new ExClassSet(IntervalClass.getInstance(objectSid).getResolveSet());
    }

    @Override
    @IdentityStrongLazy
    public ActionMapImplement<?, ClassPropertyInterface> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {
        DefaultChangeIntervalObjectAction changeIntervalObjectAction
                = new DefaultChangeIntervalObjectAction(objectFrom, objectTo, objectSid);
        Iterator<ClassPropertyInterface> iterator = changeIntervalObjectAction.interfaces.iterator();

        return new ActionMapImplement<>(changeIntervalObjectAction,
                MapFact.toRevMap(iterator.next(), interfaceFrom, iterator.next(), interfaceTo));
    }
}
