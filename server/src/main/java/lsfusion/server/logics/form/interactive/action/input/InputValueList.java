package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.query.compile.CompiledQuery;
import lsfusion.server.data.type.parse.ValueParseInterface;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.form.interactive.property.AsyncMode;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.exec.db.controller.manager.DBManager;

public abstract class InputValueList<P extends PropertyInterface, T extends ActionOrProperty<P>> {

    protected final T property;

    protected final ImMap<P, ObjectValue> mapValues; // external context

    public InputValueList(T property, ImMap<P, ObjectValue> mapValues) {
        this.property = property;

        this.mapValues = mapValues;
    }

    public boolean hasValues() {
        return !(mapValues.isEmpty() && getEnvDepends().isEmpty());
    }

    protected ImSet<CurrentEnvironmentProperty> getEnvDepends() {
        ImSet<Property> changeProps = getChangeProps();
        if(changeProps.size() == 1) // optimization
            return changeProps.single().getEnvDepends();

        MSet<CurrentEnvironmentProperty> mEnvDepends = SetFact.mSet();
        for(Property changeProp : getChangeProps())
            mEnvDepends.addAll(changeProp.getEnvDepends());
        return mEnvDepends.immutable();
    }

    public abstract ImSet<Property> getChangeProps();

    // partially duplicates getChangeProps
    public static boolean depends(ActionOrProperty property, ImSet<Property> changedSet) {
        if(property instanceof Action)
            return Property.dependsSet(((Action<?>) property).getUsedProps(), changedSet);

        return Property.depends((Property)property, changedSet);
    }

    public ActionOrProperty<?> getCacheKey() {
        return property;
    }
    public DBManager.Param<?> getCacheParam(String value, int neededCount, AsyncMode mode, QueryEnvironment env) {
        ImMap<CurrentEnvironmentProperty, Object> envValues = MapFact.EMPTY();
        ImSet<CurrentEnvironmentProperty> envDepends = getEnvDepends();
        if(!envDepends.isEmpty()) { // optimization
            ImMap<String, ValueParseInterface> queryPropParams = CompiledQuery.getQueryPropParams(env);
            envValues = envDepends.mapValues((CurrentEnvironmentProperty prop) -> queryPropParams.get(prop.paramString).getValue());
        }

        return new DBManager.Param<P>(mapValues, envValues, getCacheOrders(), value, neededCount, mode.getCacheMode());
    }

    protected abstract ImOrderMap<PropertyInterfaceImplement<P>, Boolean> getCacheOrders();
}
