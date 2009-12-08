package platform.server.logics.property;

import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.NullValue;
import platform.server.session.*;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.query.Query;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.Where;
import platform.server.auth.ChangePropertySecurityPolicy;
import platform.interop.Compare;

import java.util.Collection;
import java.util.Map;
import java.sql.SQLException;

public class CycleGroupProperty<T extends PropertyInterface> extends MaxGroupProperty<T> {

    final DataProperty toChange;

    public CycleGroupProperty(String sID, String caption, Collection<GroupPropertyInterface<T>> interfaces, Property<T> property, DataProperty toChange) {
        super(sID, caption, interfaces, property);

        this.toChange = toChange;
    }

    public Property getConstrainedProperty() {
        // создает ограничение на "одинаковость" всех группировочных св-в
        // I1=I1' AND … In = In' AND !(G=G') == false
//        new AndFormulaProperty();
        throw new RuntimeException("Not supported yet");
    }

    public void change(Map<GroupPropertyInterface<T>, DataObject> mapping, DataSession session, TableModifier<? extends TableChanges> modifier, ObjectValue newValue, boolean externalID) throws SQLException {

        // для G=newValue - изменением DataProperty
        // сделать чтобы все\хоть один I1=M1 AND I2=M2 AND … In=Mn AND G=newValue выполнялось - !FALSE - была хоть одна
        // берем I1=M1 AND I2=M2 AND … In=Mn, G=newValue and changed, "заменяя" DataProperty (C1=J1..CN=JN,D), группируем по C1,…,Cn,D ставим getWhere - отбираем
        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys();
        Map<DataPropertyInterface, KeyExpr> interfaceKeys = DataPropertyInterface.getMapKeys(toChange.interfaces);

        if(newValue instanceof DataObject) {
            DataChangeModifier newModifier = new DataChangeModifier(modifier,toChange,false);
            WhereBuilder newChangedWhere = new WhereBuilder();
            Where newWhere = groupProperty.getExpr(mapKeys,newModifier,newChangedWhere).compare((DataObject)newValue, Compare.EQUALS);
            for(GroupPropertyInterface<T> groupInterface : interfaces)
                newWhere = newWhere.and(groupInterface.implement.mapExpr(mapKeys,newModifier,newChangedWhere).compare(mapping.get(groupInterface),Compare.EQUALS));

            for(Map.Entry<Map<DataPropertyInterface,DataObject>,Map<String,ObjectValue>> newChange : new Query<DataPropertyInterface,String>(interfaceKeys,
                    Expr.groupBy(interfaceKeys,toChange.valueInterface.keyExpr,newWhere.and(newChangedWhere.toWhere()),true,interfaceKeys),"value").
                    executeClasses(session, session.baseClass).entrySet())
                session.changeProperty(toChange,newChange.getKey(),newChange.getValue().get("value"),externalID);
        }

        // для G!=newValue, изменением DataProperty на null
	    // сделать чтобы I1=M1 AND I2=M2 … In=Mn не выполнялось == FALSE - не было вообще
        // берем I1=M1, I2=M2, …, In=Mn, G!=newValue and changed (and один из всех Ii - null), группируем по C1, …, Cn получаем те кого null'им в changeProperty
        Expr oldExpr = groupProperty.getExpr(mapKeys,modifier,null);
        Where oldWhere = (newValue instanceof DataObject?oldExpr.compare((DataObject) newValue,Compare.NOT_EQUALS):oldExpr.getWhere());
        DataChangeModifier newOldModifier = new DataChangeModifier(modifier,toChange,true);
        WhereBuilder newOldChangedWhere = new WhereBuilder();
        Where newOldWhere = Where.FALSE;
        for(GroupPropertyInterface<T> groupInterface : interfaces) {
            oldWhere = oldWhere.and(groupInterface.implement.mapExpr(mapKeys,modifier,null).compare(mapping.get(groupInterface),Compare.EQUALS));
            newOldWhere = newOldWhere.or(groupInterface.implement.mapExpr(mapKeys,newOldModifier,newOldChangedWhere).getWhere().not());
        }
        for(Map<DataPropertyInterface, DataObject> oldChange : new Query<DataPropertyInterface,Object>(interfaceKeys,
                Expr.groupBy(interfaceKeys, ValueExpr.TRUE,oldWhere.and(newOldWhere).and(newOldChangedWhere.toWhere()),true,interfaceKeys).getWhere()).
                executeClasses(session, session.baseClass).keySet())
            session.changeProperty(toChange,oldChange, NullValue.instance,externalID);
    }

    @Override
    public DataChange getChangeProperty(DataSession session, Map<GroupPropertyInterface<T>, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return getJoinChangeProperty(session, interfaceValues, modifier, securityPolicy, externalID);
    }

    @Override
    public PropertyChange getJoinChangeProperty(DataSession session, Map<GroupPropertyInterface<T>, DataObject> interfaceValues, TableModifier<? extends TableChanges> modifier, ChangePropertySecurityPolicy securityPolicy, boolean externalID) throws SQLException {
        return new CyclePropertyChange<T>(this,interfaceValues); 
    }
}
