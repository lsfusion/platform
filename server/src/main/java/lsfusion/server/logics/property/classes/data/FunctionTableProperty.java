package lsfusion.server.logics.property.classes.data;

import lsfusion.base.Result;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.CustomFormulaImpl;
import lsfusion.server.data.expr.formula.CustomFormulaSyntax;
import lsfusion.server.data.table.FunctionTable;
import lsfusion.server.data.table.KeyField;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class FunctionTableProperty extends FormulaProperty<FunctionTableProperty.Interface> {

    public final FunctionTable table;
    public final PropertyField field;

    public static class Interface extends PropertyInterface {

        public Interface(int ID, KeyField field) {
            super(ID);
            this.field = field;
        }

        public final KeyField field;

        public KeyField getField() {
            return field;
        }

        public String getParamName() {
            return field.getName();
        }
    }

    public static ImOrderSet<FunctionTableProperty.Interface> getInterfaces(ImList<DataClass> keyClasses, ImList<String> keyNames) {
        return SetFact.toOrderExclSet(keyClasses.size(), (index) -> new Interface(index, new KeyField(keyNames.get(index), keyClasses.get(index))));
    }

    public FunctionTableProperty(CustomFormulaSyntax syntax, ImList<DataClass> keyClasses, ImList<String> keyNames, DataClass propertyClass, String propertyName) {
        this(LocalizedString.create(syntax.getDefaultSyntax()), syntax, getInterfaces(keyClasses, keyNames), new PropertyField(propertyName, propertyClass));
    }
    public FunctionTableProperty(LocalizedString caption, CustomFormulaSyntax syntax, ImOrderSet<FunctionTableProperty.Interface> interfaces, PropertyField propertyField) {
        super(caption, interfaces);

        this.table = new FunctionTable(syntax, interfaces.mapOrderSetValues(Interface::getField), interfaces.getSet().mapRevKeyValues(Interface::getParamName, Interface::getField).filterFnRev(syntax.params::contains));
        this.field = propertyField;

        finalizeInit();
    }

    protected Expr calculateExpr(final ImMap<FunctionTableProperty.Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return table.join(joinImplement.mapKeys(anInterface -> anInterface.field)).getExpr(field);
    }

    @Override
    protected Inferred<FunctionTableProperty.Interface> calcInferInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        return new Inferred<>(interfaces.mapValues((Interface anInterface) -> ExClassSet.toEx((DataClass)anInterface.field.type)));
    }

    public ExClassSet calcInferValueClass(final ImMap<FunctionTableProperty.Interface, ExClassSet> inferred, InferType inferType) {
        return ExClassSet.toEx((DataClass)field.type);
    }
}
