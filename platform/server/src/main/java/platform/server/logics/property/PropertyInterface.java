package platform.server.logics.property;

import platform.base.IdentityObject;
import platform.interop.serialization.IdentitySerializable;
import platform.interop.serialization.SerializationPool;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.PullExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.serialization.ServerIdentitySerializable;
import platform.server.serialization.ServerSerializationPool;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.MapDataChanges;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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

    public Expr mapExpr(Map<P, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        return joinImplement.get((P) this);
    }

    public ObjectValue read(DataSession session, Map<P, DataObject> interfaceValues, Modifier<? extends Changes> modifier) {
        return interfaceValues.get((P) this);
    }

    public void mapFillDepends(Collection<Property> depends) {
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    public Expr changeExpr;

    public MapDataChanges<P> mapJoinDataChanges(Map<P, KeyExpr> joinImplement, Expr expr, Where where, WhereBuilder changedWhere, Modifier<? extends Changes> modifier) {
        return new MapDataChanges<P>();
    }

    public void fill(Set<P> interfaces, Set<PropertyMapImplement<?, P>> properties) {
        interfaces.add((P) this);
    }

    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(Map<P, K> remap) {
        return remap.get((P)this);
    }

    public void customSerialize(ServerSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
    }

    public void customDeserialize(ServerSerializationPool pool, int iID, DataInputStream inStream) throws IOException {
        ID = iID;

        changeExpr = new PullExpr("interface " + ID);
    }
}
