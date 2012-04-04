package platform.server.classes.sets;

import platform.server.data.expr.Expr;
import platform.server.data.expr.query.Stat;
import platform.server.data.type.Type;
import platform.server.data.where.Where;

// по сути на Or
public interface AndClassSet {

    AndClassSet getKeepClass();

    AndClassSet and(AndClassSet node);

    // если не or'ся возвращаем null
    AndClassSet or(AndClassSet node);

    boolean isEmpty();

    boolean containsAll(AndClassSet node);

    OrClassSet getOr();

    Type getType();
    Stat getTypeStat();
}
