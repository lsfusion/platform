package platform.server.logics.properties;

import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ValueClassSet;

import java.util.Map;

class MapRead<P extends PropertyInterface> {
    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement<P> Implement, Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        return Implement.mapSourceExpr(JoinImplement, JoinClasses);
    }

    <M extends PropertyInterface,OM> void fillMapExpr(JoinQuery<P, OM> query, OM value, Property<M> mapProperty, Map<M, ? extends SourceExpr> joinImplement, Map<PropertyInterfaceImplement<P>, SourceExpr> implementExprs, InterfaceClassSet<M> joinClasses) {
        SourceExpr ValueExpr = mapProperty.getSourceExpr(joinImplement, joinClasses);
        query.properties.put(value, ValueExpr);
        query.and(ValueExpr.getWhere());
    }

    // разные классы считывает

    ClassSet getImplementValueClass(PropertyInterfaceImplement<P> Implement, InterfaceClass<P> ClassImplement) {
        return Implement.mapValueClass(ClassImplement);
    }

    InterfaceClassSet<P> getImplementClassSet(PropertyInterfaceImplement<P> Implement, ClassSet ReqValue) {
        return Implement.mapClassSet(ReqValue);
    }

    <M extends PropertyInterface> ValueClassSet<M> getMapChangeClass(Property<M> MapProperty) {
        return MapProperty.getValueClassSet();
    }
}
