package platform.server.logics.property;

import platform.base.identity.IdentityObject;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.PullExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PropertyInterface<P extends PropertyInterface<P>> extends IdentityObject implements CalcPropertyInterfaceImplement<P>, Comparable<P>, ServerIdentitySerializable {

    public PropertyInterface() {
        this(-1);

    }

    public PropertyInterface(int ID) {
        this.ID = ID;

        this.changeExpr = new PullExpr("interface " + ID);
    }

    public String toString() {
        return "I/"+ID;
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier modifier) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement) {
        return joinImplement.get((P) this);
    }

    public Object read(ExecutionContext context, Map<P, DataObject> interfaceValues) throws SQLException {
        return interfaceValues.get((P) this).object;
    }

    public ObjectValue readClasses(ExecutionContext context, Map<P, DataObject> interfaceValues) throws SQLException {
        return interfaceValues.get((P) this);
    }

    public void mapFillDepends(Set<CalcProperty> depends) {
    }

    public Set<OldProperty> mapOldDepends() {
        return new HashSet<OldProperty>();
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public Expr changeExpr;

    public DataChanges mapJoinDataChanges(Map<P, ? extends Expr> mapKeys, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return new DataChanges();
    }

    public void fill(Set<P> interfaces, Set<CalcPropertyMapImplement<?, P>> properties) {
        interfaces.add((P) this);
    }

    public Collection<P> getInterfaces() {
        return Collections.singleton((P) this);
    }

    public <K extends PropertyInterface> CalcPropertyInterfaceImplement<K> map(Map<P, K> remap) {
        return remap.get((P)this);
    }

    public ActionPropertyMapImplement<?, P> mapEditAction(String editActionSID, CalcProperty filterProperty) {
        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        changeExpr = new PullExpr("interface " + ID);
    }

    public Map<P, ValueClass> mapInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null)
            return Collections.singletonMap((P)this, commonValue);
        return new HashMap<P, ValueClass>();
    }

    public Collection<DataProperty> mapChangeProps() {
        return new HashSet<DataProperty>();
    }

    public DataChanges mapDataChanges(PropertyChange<P> pPropertyChange, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return new DataChanges();
    }
}
