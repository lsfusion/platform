package lsfusion.server.logics.property.cases;

import com.google.common.base.Objects;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.property.CalcPropertyInterfaceImplement;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.UnionProperty;

import java.util.List;

public class ExplicitCalcCase<P extends PropertyInterface> extends AbstractCalcCase<P> {

    public ExplicitCalcCase(CalcPropertyInterfaceImplement<P> where, CalcPropertyInterfaceImplement<P> implement) {
        this(where, implement, null);
    }

    public ExplicitCalcCase(CalcPropertyInterfaceImplement<P> where, CalcPropertyInterfaceImplement<P> implement, List<ResolveClassSet> signature) {
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
