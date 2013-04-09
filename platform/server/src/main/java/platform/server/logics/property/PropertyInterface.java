package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.ListFact;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.add.MAddSet;
import platform.base.identity.IdentityObject;
import platform.server.caches.LazyInit;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.PullExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class PropertyInterface<P extends PropertyInterface<P>> extends IdentityObject implements CalcPropertyInterfaceImplement<P>, Comparable<P>, ServerIdentitySerializable {

    public PropertyInterface() {
        this(-1);

    }

    public PropertyInterface(int ID) {
        this.ID = ID;
    }

    public static <T, P extends PropertyInterface> ImRevMap<T, P> getIdentityMap(ImMap<T, CalcPropertyInterfaceImplement<P>> mapping) {
        MAddSet<PropertyInterface> checked = SetFact.mAddSet();
        for(CalcPropertyInterfaceImplement<P> propImplement : mapping.valueIt())
            if(!(propImplement instanceof PropertyInterface && !checked.add((PropertyInterface) propImplement)))
                return null;
        return BaseUtils.immutableCast(mapping.toRevExclMap());
    }

    public String toString() {
        return "I/"+ID;
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, Modifier modifier) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement) {
        return joinImplement.get((P) this);
    }

    public Object read(ExecutionContext context, ImMap<P, DataObject> interfaceValues) throws SQLException {
        return interfaceValues.get((P) this).object;
    }

    public ObjectValue readClasses(ExecutionContext context, ImMap<P, DataObject> interfaceValues) throws SQLException {
        return interfaceValues.get((P) this);
    }

    public void mapFillDepends(MSet<CalcProperty> depends) {
    }

    public ImSet<OldProperty> mapOldDepends() {
        return SetFact.EMPTY();
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    @LazyInit
    public Expr getChangeExpr() {
        if(changeExpr==null)
            changeExpr = new PullExpr(ID);
        return changeExpr;
    }

    public Expr changeExpr;

    public DataChanges mapJoinDataChanges(ImMap<P, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return DataChanges.EMPTY;
    }

    public void fill(MSet<P> interfaces, MSet<CalcPropertyMapImplement<?, P>> properties) {
        interfaces.add((P) this);
    }

    public ImCol<P> getInterfaces() {
        return SetFact.singleton((P) this);
    }

    public <K extends PropertyInterface> CalcPropertyInterfaceImplement<K> map(ImRevMap<P, K> remap) {
        return remap.get((P)this);
    }

    public ActionPropertyMapImplement<?, P> mapEditAction(String editActionSID, CalcProperty filterProperty) {
        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
    }

    public ImMap<P, ValueClass> mapInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null)
            return MapFact.singleton((P) this, commonValue);
        return MapFact.EMPTY();
    }

    public ImSet<DataProperty> mapChangeProps() {
        return SetFact.EMPTY();
    }

    public DataChanges mapDataChanges(PropertyChange<P> pPropertyChange, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return DataChanges.EMPTY;
    }
}
