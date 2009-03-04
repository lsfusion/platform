package platform.server.logics.properties;

import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.session.DataSession;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class MapChangedRead<P extends PropertyInterface> extends MapRead<P> {

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, Collection<PropertyInterfaceImplement<P>> iImplementChanged) {
        session = iSession;
        mapChanged = iMapChanged;
        mapType = iMapType;
        ImplementType = iImplementType;
        ImplementChanged = iImplementChanged;
    }

    MapChangedRead(DataSession iSession, boolean iMapChanged, int iMapType, int iImplementType, PropertyInterfaceImplement<P> iImplementChanged) {
        this(iSession,iMapChanged,iMapType,iImplementType, Collections.singleton(iImplementChanged));
    }

    DataSession session;

    boolean mapChanged;
    // 0 - J P
    // 1 - просто NULL
    // 2 - NULL JOIN P (то есть Join'им но null'им)
    int mapType;

    Collection<PropertyInterfaceImplement<P>> ImplementChanged;
    int ImplementType;

    // проверяет изменились ли вообще то что запрашивается
    <M extends PropertyInterface> boolean check(Property<M> MapProperty) {
        for(PropertyInterfaceImplement<P> Implement : ImplementChanged)
            if(!Implement.mapHasChanges(session)) return false;
        return !(mapChanged && !session.propertyChanges.containsKey(MapProperty));
    }

    // делает разные вызовы Changed/Main
    SourceExpr getImplementExpr(PropertyInterfaceImplement<P> Implement, Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        if(ImplementChanged.contains(Implement))
            return Implement.mapChangeExpr(session, JoinImplement, ImplementType);
        else
            return super.getImplementExpr(Implement, JoinImplement, JoinClasses);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // ImplementExprs специально для MapType==1
    <M extends PropertyInterface,OM> void fillMapExpr(JoinQuery<P, OM> query, OM value, Property<M> mapProperty, Map<M, ? extends SourceExpr> joinImplement, Map<PropertyInterfaceImplement<P>, SourceExpr> implementExprs, InterfaceClassSet<M> joinClasses) {

        // закинем всем условия на Implement'ы (Join'у нужно только для !MapChanged и MapType==1, но переживет)
        for(Map.Entry<PropertyInterfaceImplement<P>,SourceExpr> ImplementExpr : implementExprs.entrySet())
            if(ImplementChanged.contains(ImplementExpr.getKey())) // нужно закинуть не Changed'ы на notNull, а Changed'ы на InJoin
                query.and(((JoinExpr)ImplementExpr.getValue()).from.inJoin);
            else
                query.and(ImplementExpr.getValue().getWhere());

        if(mapChanged) {
            JoinExpr mapExpr = session.getChange(mapProperty).getExpr(joinImplement, mapType ==3?0: mapType);
            query.and(mapExpr.from.inJoin);
            SourceExpr expr = (mapType ==3?mapExpr.getType().getExpr(null):mapExpr);
            query.properties.put(value, expr);
        } else {
            if(mapType ==1) // просто null кидаем
                query.properties.put(value, mapProperty.getType().getExpr(null));
            else {
                SourceExpr mapExpr = mapProperty.getSourceExpr(joinImplement, joinClasses);
                query.and(mapExpr.getWhere());
                SourceExpr expr = (mapType ==2?mapExpr.getType().getExpr(null):mapExpr);
                query.properties.put(value, expr);
            }
        }
    }

    ClassSet getImplementValueClass(PropertyInterfaceImplement<P> Implement, InterfaceClass<P> ClassImplement) {
        if(ImplementChanged.contains(Implement) && ImplementType!=2) {
            return Implement.mapChangeValueClass(session, ClassImplement);
        } else
            return super.getImplementValueClass(Implement, ClassImplement);    //To change body of overridden methods use File | Settings | File Templates.
    }

    InterfaceClassSet<P> getImplementClassSet(PropertyInterfaceImplement<P> Implement, ClassSet ReqValue) {
        if(ImplementChanged.contains(Implement)) {
            if(ImplementType==2) // если ImplementType=2 то And'им базовый класс с новыми
                return Implement.mapClassSet(ClassSet.universal).and(Implement.mapChangeClassSet(session, ClassSet.universal));
            else
                return Implement.mapChangeClassSet(session, ReqValue);
        } else
            return super.getImplementClassSet(Implement, ReqValue);    //To change body of overridden methods use File | Settings | File Templates.
    }

    <M extends PropertyInterface> ValueClassSet<M> getMapChangeClass(Property<M> MapProperty) {
        if(mapChanged) {
            ValueClassSet<M> MapChange = session.getChange(MapProperty).classes;
            if(mapType >=2) // если старые затираем возвращаем ссылку на nullClass
                return new ValueClassSet<M>(new ClassSet(), MapChange.getClassSet(ClassSet.universal));
            else
                return MapChange;
        } else {
            if(mapType ==2) // если 2 то NullClass
                return new ValueClassSet<M>(new ClassSet(), MapProperty.getClassSet(ClassSet.universal));
            else
            if(mapType ==1)
                return new ValueClassSet<M>(new ClassSet(),MapProperty.getUniversalInterface());
            else
                return super.getMapChangeClass(MapProperty);
        }
    }
}
