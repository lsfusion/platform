package lsfusion.server.data.expr;

import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.data.caches.hash.HashContext;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.logics.classes.data.DoubleClass;

public class RandomExpr extends StaticExpr<DoubleClass> {

    public static RandomExpr instance = new RandomExpr(); 
    private RandomExpr() {
        super(DoubleClass.instance);
    }

    protected BaseExpr translate(MapTranslate translator) {
        return this;
    }

    public boolean calcTwins(TwinImmutableObject obj) {
        return true;
    }

    public int hash(HashContext hashContext) {
        return 3821;
    }
    
    public String getSource(CompileSource compile, boolean needValue) {
        return compile.syntax.getRandom();
    }

}
