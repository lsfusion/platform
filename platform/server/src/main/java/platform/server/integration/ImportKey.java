package platform.server.integration;

import platform.server.classes.ConcreteCustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.query.Join;
import platform.server.data.query.Query;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: DAle
 * Date: 24.12.10
 * Time: 16:33
 */

public class ImportKey <P extends PropertyInterface> implements ImportKeyInterface {
    private ConcreteCustomClass keyClass;
    private final PropertyImplement<ImportFieldInterface, P> implement;

    public ImportKey(ConcreteCustomClass keyClass, PropertyImplement<ImportFieldInterface, P> implement) {
        this.keyClass = keyClass;
        this.implement = implement;
    }

    public ConcreteCustomClass getCustomClass() {
        return keyClass;
    }

    public Map<P, ImportFieldInterface> getMapping() {
        return implement.mapping;
    }

    public Property<P> getProperty() {
        return implement.property;
    }

    public Map<P, DataObject> mapObjects(ImportTable.Row row) {
        Map<P, DataObject> map = new HashMap<P, DataObject>();
        for (Map.Entry<P, ImportFieldInterface> entry : getMapping().entrySet()) {
            DataObject obj = entry.getValue().getDataObject(row);
            map.put(entry.getKey(), obj);
        }
        return map;
    }

    Object readValue(DataSession session, ImportTable.Row row) throws SQLException {
        return getProperty().read(session.sql, mapObjects(row), session.modifier, session.env);
    }

    void writeValue(DataSession session, ImportTable.Row row, DataObject obj) throws SQLException {
        getProperty().execute(mapObjects(row), session, obj.object, session.modifier);
    }

    public Expr getExpr(Map<ImportField, ? extends Expr> importKeys, Map<ImportKey<?>, SinglePropertyTableUsage<?>> addedKeys, Modifier<? extends Changes> modifier) {
        Map<P, Expr> implementExprs = getImplementExprs(importKeys);

        Join<String> addedJoin = ((SinglePropertyTableUsage<P>) addedKeys.get(this)).join(implementExprs);
        return addedJoin.getExpr("value").ifElse(addedJoin.getWhere(), implement.property.getExpr(implementExprs, modifier));
    }

    private Map<P, Expr> getImplementExprs(Map<ImportField, ? extends Expr> importKeys) {
        Map<P, Expr> mapExprs = new HashMap<P, Expr>();
        for(Map.Entry<P, ImportFieldInterface> entry : implement.mapping.entrySet())
            mapExprs.put(entry.getKey(), entry.getValue().getExpr(importKeys));
        return mapExprs;
    }

    // не будет виден CGProp, который тут неявно assert'ися но это и не важно
    public SinglePropertyTableUsage<P> synchronize(DataSession session, SingleKeyTableUsage<ImportField> importTable) throws SQLException {
        SinglePropertyTableUsage<P> propertyTable = new SinglePropertyTableUsage<P>(new ArrayList<P>(implement.property.interfaces), new Type.Getter<P>() { // именно так а не createChangeTable, потому как у property могут быть висячие ключи
            public Type getType(P key) {
                return implement.mapping.get(key).getType();
            }
        }, implement.property.getType());

        Query<P, Object> noKeysQuery = new Query<P, Object>(implement.property);
        noKeysQuery.and(GroupExpr.create(getImplementExprs(importTable.join(importTable.getMapKeys()).getExprs()), ValueExpr.TRUE, true, noKeysQuery.mapKeys).getWhere()); // в импортируемой таблице
        noKeysQuery.and(implement.property.getExpr(noKeysQuery.mapKeys, session.modifier).getWhere().not()); // для которых неопределился объект

        for (Iterator<Map<P, DataObject>> iterator = noKeysQuery.executeClasses(session.sql, session.env, session.baseClass).keySet().iterator(); iterator.hasNext();) {
            Map<P, DataObject> noKeysRow = iterator.next();
            propertyTable.insertRecord(session.sql, noKeysRow, session.addObject(keyClass, session.modifier, false, !iterator.hasNext()), false);
        }

        return propertyTable;
    }
}
