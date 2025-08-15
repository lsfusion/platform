package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class CalcClassType extends CalcType implements AlgType {
    // converted to static methods to prevent class initialization deadlocks
    
    // определение классов, prev'ы имеют base классы (что правильно, но не удобно)
    public static CalcInfoType prevBase() {
        return CalcInfoType.PREVBASE;     
    }

    // определение классов, где prev'ы имеют те же классы (а не OBJECT)
    public static CalcClassType prevSame() {
        return PREVSAME;
    }

    // тоже самое что и prevSame(), но с IS'ами нужно для одной эвристики
    public static CalcClassType prevSameKeepIS() {
        return PREVSAME_KEEPIS;
    }
    
    private final static CalcClassType PREVSAME = new CalcClassType("PREVSAME"); 
    private final static CalcClassType PREVSAME_KEEPIS = new CalcClassType("PREVSAME_KEEPIS"); 

    public CalcClassType(String caption) {
        super(caption);
    }

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(Property<P> property) {
        return property.calcClassValueWhere(this);
    }

    public <P extends PropertyInterface> ImMap<P, ValueClass> getInterfaceClasses(Property<P> property) {
        return property.calcInterfaceClasses(this);
    }

    public <P extends PropertyInterface> ValueClass getValueClass(Property<P> property) {
        return property.calcValueClass(this);
    }

    public <P extends PropertyInterface> boolean isInInterface(Property<P> property, ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        return property.calcIsInInterface(interfaceClasses, isAny, this);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkExclusiveness(Property<T> property, String caseInfo, Property<P> intersect, String intersectInfo, ImRevMap<P, T> map, String abstractInfo) {
        property.calcCheckExclusiveness(caseInfo, intersect, intersectInfo, map, this, abstractInfo);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkContainsAll(Property<T> property, Property<P> intersect, String caseInfo, ImRevMap<P, T> map, PropertyInterfaceImplement<T> value, String abstractInfo) {
        property.calcCheckContainsAll(caseInfo, intersect, map, this, value, abstractInfo);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkAllImplementations(Property<T> property, ImList<Property<P>> intersects, ImList<ImRevMap<P, T>> maps) {
        property.calcCheckAllImplementations(intersects, maps, this);
    }

}
