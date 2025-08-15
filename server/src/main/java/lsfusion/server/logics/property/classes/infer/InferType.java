package lsfusion.server.logics.property.classes.infer;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

public class InferType implements AlgType {
    public InferType() {
    }
    
    // converted to static methods to prevent class initialization deadlocks
    public static InferInfoType prevBase() {
        return InferInfoType.PREVBASE;    
    }
    
    public static InferType prevSame() {
        return PREVSAME;
    }

    public static InferType prevSameNoExplicit() {
        return PREVSAME_NO_EXPLICIT;
    }

    public static InferType resolve() {
        return RESOLVE;
    }
    
    private static final InferType PREVSAME = new InferType();
    private static final InferType PREVSAME_NO_EXPLICIT = new InferType();
    private static final InferType RESOLVE = new InferType(); // PREVSAME

    public <P extends PropertyInterface> ClassWhere<Object> getClassValueWhere(Property<P> property) {
        assert this != RESOLVE;
        return property.inferClassValueWhere(this);
    }

    public <P extends PropertyInterface> ImMap<P, ValueClass> getInterfaceClasses(Property<P> property) {
        return property.inferGetInterfaceClasses(this);
    }

    public <P extends PropertyInterface> ValueClass getValueClass(Property<P> property) {
        return property.inferGetValueClass(this);
    }

    public <P extends PropertyInterface> boolean isInInterface(Property<P> property, ImMap<P, ? extends AndClassSet> interfaceClasses, boolean isAny) {
        assert this != RESOLVE;
        return property.inferIsInInterface(interfaceClasses, isAny, this);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkExclusiveness(Property<T> property, String caseInfo, Property<P> intersect, String intersectInfo, ImRevMap<P, T> map, String abstractInfo) {
        assert this != RESOLVE;
        property.inferCheckExclusiveness(caseInfo, intersect, intersectInfo, map, this, abstractInfo);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkContainsAll(Property<T> property, Property<P> intersect, String caseInfo, ImRevMap<P, T> map, PropertyInterfaceImplement<T> value, String abstractInfo) {
        assert this != RESOLVE;
        property.inferCheckContainsAll(intersect, caseInfo, map, this, value, abstractInfo);
    }

    public <T extends PropertyInterface, P extends PropertyInterface> void checkAllImplementations(Property<T> property, ImList<Property<P>> intersects, ImList<ImRevMap<P, T>> maps) {
        assert this != RESOLVE;        
        property.inferCheckAllImplementations(intersects, maps, this);
    }

    public AlgInfoType getAlgInfo() {
        assert this != RESOLVE;
        return prevBase();
    }
}
