package lsfusion.server.logics.form.interactive.property;

import com.google.common.base.Throwables;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.time.IntervalClass;
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

public class IntervalValueProperty extends NoIncrementProperty<ClassPropertyInterface> {

    private final ObjectEntity intervalObjectEntity;
    private final ImOrderSet<ObjectEntity> objects;
    private final String objectSid;

    public IntervalValueProperty(ImOrderSet<ObjectEntity> objects) {
        super(LocalizedString.NONAME, IsClassProperty.getInterfaces(new ValueClass[]{objects.get(0).baseClass, objects.get(1).baseClass}));

        this.objectSid = objects.get(0).baseClass.getSID();
        IntervalClass intervalInstance = IntervalClass.getInstance(objectSid);
        this.intervalObjectEntity = new ObjectEntity(intervalInstance.getTypeID(), intervalInstance.getBaseClass(), intervalInstance.getCaption());
        this.objects = objects;

        finalizeInit();
    }

    @Override
    protected Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        try {
            return ThreadLocalContext.getBusinessLogics().timeLM.findProperty(getPropertyName())
                    .getExpr(joinImplement.get(interfaces.get(0)),
                            joinImplement.get(interfaces.get(1)));
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
        return new ExClassSet(intervalObjectEntity.getResolveClassSet());
    }

    @Override
    @IdentityStrongLazy
    public ActionMapImplement<?, ClassPropertyInterface> getDefaultEventAction(String eventActionSID, ImList<Property> viewProperties) {
        DefaultChangeIntervalObjectAction changeIntervalObjectAction
                = new DefaultChangeIntervalObjectAction(objects, intervalObjectEntity, objectSid);

        return new ActionMapImplement<>(changeIntervalObjectAction,
                MapFact.toRevMap(changeIntervalObjectAction.interfaces.get(0), interfaces.get(0), changeIntervalObjectAction.interfaces.get(1), interfaces.get(1)));
    }
}
