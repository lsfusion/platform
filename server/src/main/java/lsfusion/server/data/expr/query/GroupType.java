package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.JSONBuildSingleArrayFormulaImpl;
import lsfusion.server.data.expr.formula.SumFormulaImpl;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.AJSONClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;
import lsfusion.server.logics.classes.data.integral.IntegralClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.GroupProperty;
import lsfusion.server.logics.property.set.MaxGroupProperty;
import lsfusion.server.logics.property.set.SumGroupProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class GroupType implements AggrType {

    public static final GroupType SUM = new GroupType() {
        public <T extends PropertyInterface> GroupProperty<T> createProperty(LocalizedString caption, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> property, ImSet<? extends PropertyInterfaceImplement<T>> interfaces) {
            return new SumGroupProperty<>(caption, innerInterfaces, interfaces, property);
        }
        public boolean hasAdd() {
            return true;
        }
        public Expr add(Expr op1, Expr op2) {
            return op1.sum(op2);
        }
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs) {
            return !needValue && getMainExpr(exprs).isAlwaysPositiveOrNull();
        }
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return syntax.getNotZero(getAggrSource("SUM", getSafeExprSource(0, exprs, exprReaders, type, syntax, typeEnv), orderClause), type, typeEnv);
        }
        public Type getType(Type exprType) {
            assert exprType instanceof IntegralClass;
            return exprType;
        }
        public String name() {
            return "STRING";
        }
    };

    public static final GroupType MAX = new GroupType() {
        public <T extends PropertyInterface> GroupProperty<T> createProperty(LocalizedString caption, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> property, ImSet<? extends PropertyInterfaceImplement<T>> interfaces) {
            return new MaxGroupProperty<>(caption, innerInterfaces, interfaces, property, false);
        }
        public boolean hasAdd() {
            return true;
        }
        public Expr add(Expr op1, Expr op2) {
            return op1.max(op2);
        }
        public boolean isSelect() {
            return true;
        }
        public boolean isMaxMin() {
            return true;
        }
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs) {
            return true;
        }
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource(type instanceof ConcatenateType && syntax.hasAggConcProblem() ? "MAXC" : "MAX", exprs.get(0), orderClause);
        }
        public String name() {
            return "MAX";
        }
    };

    public static final GroupType MIN = new GroupType() {
        public <T extends PropertyInterface> GroupProperty<T> createProperty(LocalizedString caption, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> property, ImSet<? extends PropertyInterfaceImplement<T>> interfaces) {
            return new MaxGroupProperty<>(caption, innerInterfaces, interfaces, property, true);
        }
        public boolean hasAdd() {
            return true;
        }
        public Expr add(Expr op1, Expr op2) {
            return op1.min(op2);
        }
        public boolean isSelect() {
            return true;
        }
        public boolean isMaxMin() {
            return true;
        }
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs) {
            return true;
        }
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource(type instanceof ConcatenateType && syntax.hasAggConcProblem() ? "MINC" : "MIN", exprs.get(0), orderClause);
        }
        public String name() {
            return "MIN";
        }
    };

    public static final GroupType ANY = new GroupType() {
        public boolean hasAdd() {
            return true;
        }
        public Expr add(Expr op1, Expr op2) {
            return op1.nvl(op2);
        }
        public boolean isSelect() {
            return true;
        }
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs) {
            return true;
        }
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource(syntax.getAnyValueFunc(), getSafeExprSource(0, exprs, exprReaders, type, syntax, typeEnv), orderClause);
        }
        public String name() {
            return "ANY";
        }
    };

    public static final GroupType CONCAT = new GroupType() {
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return type.getCast(getAggrSource("STRING_AGG" ,castToVarStrings(exprs, exprReaders, type, syntax, typeEnv).toString(","), orderClause), syntax, typeEnv);
        }
        public Type getType(Type exprType) {
            return StringClass.getv(((StringClass)exprType).caseInsensitive, ExtInt.UNLIMITED);
        }
        public String name() {
            return "CONCAT";
        }
    };

    public static final GroupType AGGAR_SETADD = new GroupType() {
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource("AGGAR_SETADD", exprs.get(0), orderClause);
        }
        public String name() {
            return "AGGAR_SETADD";
        }
    };

    public static final GroupType LAST = new GroupType() {
        public boolean isSelect() {
            return true;
        }
        public boolean isSelectNotInWhere() {
            return true;
        }
        public Where getWhere(ImList<Expr> exprs) {
            assert exprs.size()==2;
            return exprs.get(0).getWhere();
        }
        public int getSkipWhereIndex() {
            return 1;
        }
        public ImList<Expr> followFalse(Where falseWhere, ImList<Expr> exprs, boolean pack) {
            assert exprs.size()==2;
            Expr firstExpr = exprs.get(0).followFalse(falseWhere, pack);
            Expr secondExpr = exprs.get(1).followFalse(falseWhere.or(firstExpr.getWhere().not()), pack);
            return ListFact.toList(firstExpr, secondExpr);
        }
        public int getMainIndex() {
            return 1;
        }
        public Expr getSingleExpr(ImList<Expr> exprs) {
            assert exprs.size()==2;
            return exprs.get(1).and(exprs.get(0).getWhere());
        }
        public boolean nullsNotAllowed() {
            return true;
        }
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs) {
            return true;
        }
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            assert exprs.size() == 2;
            return getAggrSource(syntax.getLastFunc(), getSafeExprSource(1, exprs, exprReaders, type, syntax, typeEnv), orderClause);
        }
        public String name() {
            return "LAST";
        }
    };

    public static final GroupType JSON_CONCAT = new GroupType() {
        public Expr getSingleExpr(ImList<Expr> exprs) {
            return FormulaExpr.create(JSONBuildSingleArrayFormulaImpl.instance, ListFact.singleton(exprs.get(0)));
        }
        public boolean nullsNotAllowed() {
            return true;
        }
        protected String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource((type instanceof JSONTextClass ? "JSON_AGG" : "JSONB_AGG"), exprs.get(0), orderClause);
        }
        public String name() {
            return "JSON_CONCAT";
        }
    };

    public static GroupType LOGICAL() {
        return ANY;
    }
    
    public static GroupType CHANGE(Type type) {
        return MAXCHECK(type);
    }

    public static GroupType ASSERTSINGLE_CHANGE() {
        return ASSERTSINGLE();
    }
    
    public static GroupType MAXCHECK(Type type) {
        if (type instanceof FileClass || type instanceof LinkClass)  // для File MAX not supported
            return ANY;

        return MAX; 
    }

    public static GroupType ASSERTSINGLE() {
        return ANY;
    }

    public <T extends PropertyInterface> GroupProperty<T> createProperty(LocalizedString caption, ImSet<T> innerInterfaces, PropertyInterfaceImplement<T> property, ImSet<? extends PropertyInterfaceImplement<T>> interfaces) {
        throw new UnsupportedOperationException();
    }

    public Expr add(Expr op1, Expr op2) {
        assert hasAdd();
        throw new UnsupportedOperationException();
    }

    public boolean isSelect() {
        return false;
    }

    public boolean canBeNull() {
        return false;
    }

    public boolean isSelectNotInWhere() { // в общем то оптимизационная вещь потом можно убрать
        assert isSelect();
        return false;
    }
    public Where getWhere(ImList<Expr> exprs) {
        return Expr.getWhere(exprs);
    }
    public int getSkipWhereIndex() {
        return -1;
    }

    public ImList<Expr> followFalse(Where falseWhere, ImList<Expr> exprs, boolean pack) {
        return falseWhere.followFalse(exprs, pack);
    }

    public Expr getMainExpr(ImList<Expr> exprs) {
        return exprs.get(getMainIndex());
    }

    public int getMainIndex() {
        return 0;
    }

    public boolean hasSingle() {
        return true;
    }
    public Expr getSingleExpr(ImList<Expr> exprs) {
        return exprs.get(0);
    }

    public boolean hasAdd() {
        return false;
    }
    
    public boolean isMaxMin() {
        return false;
    }

    // if not comutative and not invariant to the occurrence of nulls in the selection
    public boolean nullsNotAllowed() {
        return false;
    }
    
    public boolean isLastOpt(boolean needValue, ImList<Expr> exprs) {
        return !needValue;
    }

    public boolean splitExprCases() {
        assert hasAdd();
        return isSelect() && Settings.get().isSplitGroupSelectExprcases();
    }

    public boolean splitInnerJoins() {
        assert hasAdd();
        return isSelect() && Settings.get().isSplitSelectGroupInnerJoins();
    }

    public boolean splitInnerCases() {
        assert hasAdd();
        return false;
    }

    public boolean exclusive() {
        assert hasAdd();
        return !isSelect();
    }

    public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
        return getSource(exprs, exprReaders, BaseUtils.clause("ORDER BY", Query.stringOrder(orders, syntax)), type, syntax, typeEnv);
    }

    protected abstract String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, String orderClause, Type type, SQLSyntax syntax, TypeEnvironment typeEnv);

    private static String getAggrSource(String fnc, String exprs, String orderClause) {
        return getAggrSource(fnc, exprs, orderClause, false);
    }
    private static String getAggrSource(String fnc, String exprs, String orderClause, boolean setOrdered) {
        return fnc + "(" + exprs + (setOrdered ? ") WITHIN GROUP (" : "") + orderClause + ")";
    }

    public static ImList<String> castToVarStrings(ImList<String> exprs, final ImList<? extends ClassReader> readers, final Type resultType, final SQLSyntax syntax, final TypeEnvironment typeEnv) {
        return exprs.mapListValues((i, value) -> {
            ClassReader reader = readers.get(i);
            if(reader instanceof Type) {
                if(resultType instanceof AJSONClass)
                    value = resultType.getCast(value, syntax, typeEnv, (Type) reader);
                else
                    value = SumFormulaImpl.castToVarString(value, ((StringClass) resultType), (Type) reader, syntax, typeEnv);
            }
            return value;
        });
    }

    // it seems that null cast is needed for all aggr types, but for now we faced only SUM and ANYVALUE
    private static String getSafeExprSource(int index, ImList<String> exprs, ImList<ClassReader> exprReaders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
        String exprSource = exprs.get(index);
        if(exprReaders != null) {
            ClassReader classReader = exprReaders.get(index);
            if (classReader instanceof NullReader) // если null cast'им, на самом деле это частично хак, так как может протолкнуться условие, но emptyselect не получится, а empty будет конкретное выражение (возможно тоже самое нужно для partition и т.п.)
                exprSource = type.getCast(exprSource, syntax, typeEnv);
        }
        return exprSource;
    }

    public Type getType(Type exprType) {
        return exprType;
    }
    public abstract String name();
}
