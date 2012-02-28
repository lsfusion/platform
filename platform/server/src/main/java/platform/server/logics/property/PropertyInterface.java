package platform.server.logics.property;

import platform.base.identity.IdentityObject;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;
import platform.server.session.PropertyChanges;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PropertyInterface<P extends PropertyInterface<P>> extends IdentityObject implements PropertyInterfaceImplement<P>, Comparable<P>, ServerIdentitySerializable {

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
        return read(context.getSession(), interfaceValues, context.getModifier());
    }

    public Object read(DataSession session, Map<P, DataObject> interfaceValues, Modifier modifier) {
        return interfaceValues.get((P) this).object;
    }

    public ObjectValue readClasses(DataSession session, Map<P, DataObject> interfaceValues, Modifier modifier) {
        return interfaceValues.get((P) this);
    }

    public Expr mapIncrementExpr(Map<P, ? extends Expr> joinImplement, PropertyChanges newChanges, PropertyChanges prevChanges, WhereBuilder changedWhere, IncrementType incrementType) {
        return mapExpr(joinImplement);
    }

    public void mapFillDepends(Collection<Property> depends) {
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public Expr changeExpr;

    public MapDataChanges<P> mapJoinDataChanges(Map<P, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return new MapDataChanges<P>();
    }

    public void fill(Set<P> interfaces, Set<PropertyMapImplement<?, P>> properties) {
        interfaces.add((P) this);
    }

    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(Map<P, K> remap) {
        return remap.get((P)this);
    }

    public PropertyMapImplement<?, P> mapChangeImplement(Map<P, DataObject> interfaceValues, DataSession session, Modifier modifier) throws SQLException {
        return null;
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ServerSerializationPool pool, DataInputStream inStream) throws IOException {
        changeExpr = new PullExpr("interface " + ID);
    }
}
