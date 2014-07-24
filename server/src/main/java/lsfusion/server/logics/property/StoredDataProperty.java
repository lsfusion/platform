package lsfusion.server.logics.property;

import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.Message;
import lsfusion.server.ParamMessage;
import lsfusion.server.classes.BaseClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.*;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.where.Where;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class StoredDataProperty extends DataProperty {

    public StoredDataProperty(String caption, ValueClass[] classes, ValueClass value) {
        super(caption, classes, value);

        finalizeInit();
    }

    public boolean isStored() {
        return true;
    }

    // нет
    public static FunctionSet<CalcProperty> set = new FunctionSet<CalcProperty>() {
        public boolean contains(CalcProperty element) {
            return element instanceof StoredDataProperty;
        }
        public boolean isEmpty() {
            return false;
        }
        public boolean isFull() {
            return false;
        }
    };

}
