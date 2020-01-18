package lsfusion.server.logics.form.open;

import lsfusion.base.Pair;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.SQLException;
import java.util.function.Function;

// вообще по хорошему надо бы generiть интерфейсы, но тогда с DataChanges (из-за дебилизма generics в современных языках) будут проблемы
public abstract class FormAction<O extends ObjectSelector> extends SystemExplicitAction {

    public final FormSelector<O> form;
    public final ImRevMap<O, ClassPropertyInterface> mapObjects;

    private static <O extends ObjectSelector> ValueClass[] getValueClasses(FormSelector<O> form, ImList<O> objects, ValueClass[] extraValueClasses) {
        ImList<ValueClass> objectClasses = objects.mapListValues((Function<O, ValueClass>) o -> form.getBaseClass(o));
        return ArrayUtils.addAll(objectClasses.toArray(new ValueClass[objectClasses.size()]), extraValueClasses);
    }

    @Override
    protected boolean allowNulls() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean checkNulls(ImSet<ClassPropertyInterface> dataKeys) {
        return !dataKeys.containsAll(notNullInterfaces);
    }
    
    private final ImSet<ClassPropertyInterface> notNullInterfaces;

    public FormAction(LocalizedString caption,
                      FormSelector<O> form,
                      final ImList<O> objectsToSet,
                      final ImList<Boolean> nulls, boolean extraNotNull,
                      ValueClass... extraValueClasses) {
        super(caption, getValueClasses(form, objectsToSet, extraValueClasses));

        this.form = form;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();        
        ImOrderSet<ClassPropertyInterface> objectInterfaces = orderInterfaces.subOrder(0, objectsToSet.size());
        
        mapObjects = objectInterfaces.mapOrderRevKeys(objectsToSet::get);        
        ImSet<ClassPropertyInterface> notNullInterfaces = objectInterfaces.mapOrderValues(nulls::get).filterFnValues(element -> !element).keys();
        if(extraNotNull)
            notNullInterfaces = notNullInterfaces.addExcl(orderInterfaces.subOrder(objectsToSet.size(), interfaces.size()).getSet());
        this.notNullInterfaces = notNullInterfaces;
    }

    protected abstract void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects) throws SQLException, SQLHandledException;

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<O, ? extends ObjectValue> mapObjectValues = mapObjects.join(context.getKeys());
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> resolvedForm = form.getForm(context.getBL().LM, context.getSession(), mapObjectValues);
        if(resolvedForm == null)
            return;
        executeInternal(resolvedForm.first, resolvedForm.second.rightJoin(mapObjectValues), context, resolvedForm.second);
    }

    @Override
    protected boolean isSync() {
        return true; // тут сложно посчитать что изменяется, поэтому пока просто считаем синхронным, чтобы не компилировался FOR
    }

    @IdentityLazy
    @Override    
    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return mapObjects.filterFnRev(ObjectSelector::noClasses).valuesSet();
    }
}
