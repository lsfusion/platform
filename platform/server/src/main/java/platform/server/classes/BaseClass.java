package platform.server.classes;

import platform.server.classes.sets.ConcreteCustomClassSet;
import platform.server.logics.table.ObjectTable;
import platform.server.data.expr.BaseExpr;
import platform.server.data.PropertyField;
import platform.server.data.KeyField;
import platform.server.data.query.Join;

import java.util.Map;
import java.util.Collections;

public class BaseClass extends AbstractCustomClass {

    public ObjectTable table;

    public final UnknownClass unknown;

    public BaseClass(Integer iID, String caption) {
        super(iID, caption);
        table = new ObjectTable(this);
        unknown = new UnknownClass(this);
    }

    @Override
    public BaseClass getBaseClass() {
        return this;
    }

    public ObjectClass findClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findClassID((int)idClass);
    }

    public ConcreteClass findConcreteClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findConcreteClassID((int)idClass);
    }

    public ConcreteCustomClass getConcrete() {
        ConcreteCustomClassSet concrete = new ConcreteCustomClassSet();
        fillNextConcreteChilds(concrete);
        return concrete.get(0);
    }
}
