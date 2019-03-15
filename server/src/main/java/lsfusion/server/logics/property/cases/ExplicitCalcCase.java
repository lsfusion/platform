package lsfusion.server.logics.property.cases;

import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.util.List;

public class ExplicitCalcCase<P extends PropertyInterface> extends AbstractCalcCase<P> {

    public ExplicitCalcCase(PropertyInterfaceImplement<P> where, PropertyInterfaceImplement<P> implement) {
        this(where, implement, null);
    }

    public ExplicitCalcCase(PropertyInterfaceImplement<P> where, PropertyInterfaceImplement<P> implement, List<ResolveClassSet> signature) {
        super(where, implement, signature);
    }

    // нужно чтобы было симметрично с CalcCase потому как иначе будет Ambigious identical implementation, причем возможно, что в Action'ах она не будет, а в Calc'ах нет и это сильно затрудняет диагностику 
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AbstractCase && where.equalsMap(((AbstractCase) o).where) && implement.equalsMap(((AbstractCase) o).implement) && signature.equals(((AbstractCase) o).signature);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * where.hashMap() + implement.hashMap()) + signature.hashCode();
    }
}
