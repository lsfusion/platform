package platform.server.data.expr;

import platform.base.TwinImmutableInterface;
import platform.server.caches.hash.HashContext;
import platform.server.classes.StaticClass;
import platform.server.classes.StaticCustomClass;
import platform.server.data.query.CompileSource;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;

public class StaticValueExpr extends StaticExpr<StaticClass> {

    private final Object object;
    private boolean sID;

    public StaticValueExpr(Object value, StaticClass customClass, boolean sID) {
        super(customClass);

        this.object = value;
        this.sID = sID;
    }

    public StaticValueExpr(Object value, StaticClass customClass) {
        this(value, customClass, false);
    }

    public StaticValueExpr(Object value, StaticCustomClass customClass, boolean sID) {
        this(value, (StaticClass)customClass, sID);
    }

    protected StaticValueExpr translate(MapTranslate translator) {
        return this;
    }

    public StaticValueExpr translateQuery(QueryTranslator translator) {
        return this;
    }

    protected int hash(HashContext hashContext) {
        return object.hashCode() * 31 + objectClass.hashCode() + 6;
    }

    public boolean twins(TwinImmutableInterface o) {
        return object.equals(((StaticValueExpr)o).object) && objectClass.equals(((StaticValueExpr)o).objectClass) && sID==((StaticValueExpr)o).sID;
    }

    public String getSource(CompileSource compile) {
        if(compile instanceof ToString)
            return object + " - " + objectClass + " sID";
        if(sID)
            return ((StaticCustomClass)objectClass).getString(object,compile.syntax);
        else
            return objectClass.getType().getString(object, compile.syntax);
    }
}
