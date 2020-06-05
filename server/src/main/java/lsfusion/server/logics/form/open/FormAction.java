package lsfusion.server.logics.form.open;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.caches.IdentityInstanceLazy;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.stat.ExportAction;
import lsfusion.server.logics.form.open.stat.FormStaticAction;
import lsfusion.server.logics.form.open.stat.PrintAction;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilterInstance;
import lsfusion.server.logics.form.struct.filter.ContextFilterSelector;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyReaderEntity;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
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
    
    protected final FormEntity getForm() {
        return form.getStaticForm(getBaseLM());
    }

    private static <O extends ObjectSelector> ValueClass[] getValueClasses(FormSelector<O> form, ImList<O> objects, int contextInterfaces, ValueClass[] extraValueClasses) {
        ImList<ValueClass> objectClasses = objects.mapListValues((Function<O, ValueClass>) o -> form.getBaseClass(o));
        return ArrayUtils.addAll(ArrayUtils.addAll(objectClasses.toArray(new ValueClass[objectClasses.size()]), BaseUtils.genArray(null, contextInterfaces, ValueClass[]::new)), extraValueClasses);
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

    // CONTEXT
    protected final ImSet<ClassPropertyInterface> contextInterfaces;
    protected final ImSet<ContextFilterSelector<?, ClassPropertyInterface, O>> contextFilters;

    public <C extends PropertyInterface> FormAction(LocalizedString caption,
                      FormSelector<O> form,
                      final ImList<O> objectsToSet,
                      final ImList<Boolean> nulls,
                      ImOrderSet<C> orderContextInterfaces, ImList<ContextFilterSelector<?, C, O>> contextFilters,
                      ValueClass... extraValueClasses) {
        super(caption, getValueClasses(form, objectsToSet, orderContextInterfaces.size(), extraValueClasses));

        this.form = form;

        ImOrderSet<ClassPropertyInterface> orderInterfaces = getOrderInterfaces();        
        ImOrderSet<ClassPropertyInterface> objectInterfaces = orderInterfaces.subOrder(0, objectsToSet.size());
        
        mapObjects = objectInterfaces.mapOrderRevKeys(objectsToSet::get);
        this.notNullInterfaces = objectInterfaces.mapOrderValues(nulls::get).filterFnValues(element -> !element).keys();

        ImRevMap<C, ClassPropertyInterface> mapContextInterfaces = orderContextInterfaces.mapSet(orderInterfaces.subOrder(objectsToSet.size(), objectsToSet.size() + orderContextInterfaces.size()));
        this.contextInterfaces = mapContextInterfaces.valuesSet();
        this.contextFilters = contextFilters.mapListValues((Function<ContextFilterSelector<?, C, O>, ContextFilterSelector<?, ClassPropertyInterface, O>>)
                filter -> filter.map(mapContextInterfaces)).toOrderExclSet().getSet();
    }

    protected abstract void executeInternal(FormEntity form, ImMap<ObjectEntity, ? extends ObjectValue> mapObjectValues, ExecutionContext<ClassPropertyInterface> context, ImRevMap<ObjectEntity, O> mapResolvedObjects, ImSet<ContextFilterInstance> contextFilters) throws SQLException, SQLHandledException;

    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ImMap<O, ? extends ObjectValue> mapObjectValues = mapObjects.join(context.getKeys());
        Pair<FormEntity, ImRevMap<ObjectEntity, O>> resolvedForm = form.getForm(context.getBL().LM, context.getSession(), mapObjectValues);
        if(resolvedForm == null)
            return;

        // context filters
        MExclSet<ContextFilterInstance> mContextFilters = SetFact.mExclSet();
        for(ContextFilterSelector<?, ClassPropertyInterface, O> contextFilter : this.contextFilters)
            mContextFilters.exclAddAll(contextFilter.getInstances(context.getKeys(), resolvedForm.second.reverse()));
        ImSet<ContextFilterInstance> contextFilters = mContextFilters.immutable();

        executeInternal(resolvedForm.first, resolvedForm.second.rightJoin(mapObjectValues), context, resolvedForm.second, contextFilters);
    }

    @Override
    protected boolean isSync() {
        return true; // тут сложно посчитать что изменяется, поэтому пока просто считаем синхронным, чтобы не компилировался FOR
    }

    @IdentityLazy
    @Override    
    protected ImSet<ClassPropertyInterface> getNoClassesInterfaces() {
        return mapObjects.filterFnRev(ObjectSelector::noClasses).valuesSet().addExcl(contextInterfaces);
    }

    @IdentityInstanceLazy
    @Override
    public PropertyMapImplement<?, ClassPropertyInterface> calcWhereProperty() {
        PropertyMapImplement<?, ClassPropertyInterface> result = super.calcWhereProperty();
        if(!contextFilters.isEmpty()) { // filters don't stop form from showing, however they can be used for param classes, so we're using the same hack as in SystemAction
            PropertyMapImplement<?, ClassPropertyInterface> contextFilterWhere = PropertyFact.createUnion(interfaces, contextFilters.mapSetValues((Function<ContextFilterSelector<?, ClassPropertyInterface, O>, PropertyMapImplement<?, ClassPropertyInterface>>) filter -> filter.getWhereProperty(mapObjects)).addExcl(PropertyFact.createTrue()).toList());
            result = PropertyFact.createAnd(result, contextFilterWhere);
        }
        return result;
    }

    @Override
    protected ImMap<Property, Boolean> aspectUsedExtProps() {
        FormEntity formEntity = getForm();

        MSet<Property> mProps = SetFact.mSet();
        for (PropertyDrawEntity<?> propertyDraw : this instanceof FormStaticAction ? formEntity.getStaticPropertyDrawsList() : formEntity.getPropertyDrawsList()) {
            if (this instanceof ExportAction)
                mProps.add(propertyDraw.getValueProperty().property);
            else {
                MExclSet<PropertyReaderEntity> mReaders = SetFact.mExclSet();
                propertyDraw.fillQueryProps(mReaders);
                for (PropertyReaderEntity reader : mReaders.immutable()) {
                    ActionOrProperty property = reader.getPropertyObjectEntity().property;
                    if(property instanceof Property)
                        mProps.add((Property) property);
                }
            }
        }
        return mProps.immutable().toMap(false);
    }
}
