package lsfusion.server.form.instance;

import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.ClassViewType;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.entity.GroupObjectEntity;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.property.Property;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public interface FormSourceInterface<PropertyDraw extends PropertyReader, GroupObject, PropertyObject, CalcPropertyObject extends Order, Order, Obj extends Order, PropertyReader> {

    BaseClass getBaseClass();

    QueryEnvironment getQueryEnv();

    DataSession getSession();

    ImOrderSet<GroupObject> getOrderGroups();
    ImList<PropertyDraw> getProperties();

    Where getWhere(GroupObject groupObject, ImMap<Obj, Expr> mapExprs) throws SQLException, SQLHandledException;
    Expr getExpr(Order o, ImMap<Obj, Expr> mapExprs) throws SQLException, SQLHandledException;

    boolean isPropertyShown(PropertyDraw propertyDraw);

    // interfaces
    int getGroupID(GroupObject groupObject);
    PropertyObject getPropertyObject(PropertyDraw propertyDraw);
    ImCol<Obj> getPObjects(PropertyObject propertyObject);
    GroupObject getGroupTo(Obj obj);
    GroupObject getToDraw(PropertyDraw propertyDraw);
    GroupObject getApplyObject(PropertyObject propertyObject);
    ImSet<Obj> getObjects(ImSet<GroupObject> groups);
    ImSet<Obj> getObjects(GroupObject groupObject);
    ImOrderSet<Obj> getOrderObjects(GroupObject groupObject);
    ImOrderSet<Obj> getOrderObjects(ImOrderSet<GroupObject> go);
    Type getType(Order o);

    ImOrderSet<GroupObject> getOrderColumnGroupObjects(PropertyDraw propertyDraw);
    ImSet<GroupObject> getColumnGroupObjects(PropertyDraw propertyDraw);

    GroupObjectEntity getEntity(GroupObject groupObject);
    Property getProperty(PropertyObject propertyObject);

    String getGroupSID(GroupObject cpo);
    String getPSID(PropertyDraw cpo);

    ClassViewType getGroupViewType(GroupObject groupObject); // interface
    ClassViewType getPViewType(PropertyDraw propertyDraw);

    ImOrderMap<Order, Boolean> getOrders(GroupObject groupObject);

    ObjectValue getObjectValue(Obj obj); // вызывается если не GRID

    CalcPropertyObject getDrawInstance(PropertyDraw propertyDraw);
    CalcPropertyObject getPropertyCaption(PropertyDraw propertyDraw);
    CalcPropertyObject getPropertyFooter(PropertyDraw propertyDraw);

    PropertyReader getCaptionReader(PropertyDraw propertyDraw);
    PropertyReader getFooterReader(PropertyDraw propertyDraw);
}
