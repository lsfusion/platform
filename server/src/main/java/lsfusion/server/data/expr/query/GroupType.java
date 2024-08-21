package lsfusion.server.data.expr.query;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.ExtInt;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.JSONBuildSingleArrayFormulaImpl;
import lsfusion.server.data.expr.formula.SumFormulaImpl;
import lsfusion.server.data.query.Query;
import lsfusion.server.data.query.compile.CompileOrder;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.stat.Stat;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.reader.ClassReader;
import lsfusion.server.data.type.reader.NullReader;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.AJSONClass;
import lsfusion.server.logics.classes.data.file.FileClass;
import lsfusion.server.logics.classes.data.file.JSONTextClass;
import lsfusion.server.logics.classes.data.link.LinkClass;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.set.GroupProperty;
import lsfusion.server.logics.property.set.MaxGroupProperty;
import lsfusion.server.logics.property.set.SumGroupProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Objects;

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
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
            return !needValue && getMainExpr(exprs, orders).isAlwaysPositiveOrNull();
        }
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return syntax.getNotZero(getAggrSource("SUM", getSafeExprSource(0, exprs, exprReaders, type, syntax, typeEnv), orders, syntax), type, typeEnv);
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
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
            return true;
        }
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            // in postgres somewhy in MAX function varchar is converted to text, and (anyelement, anyelement( functions can not implicitly cast text to varchar
            return type.getCast(getAggrSource(type instanceof ConcatenateType && syntax.hasAggConcProblem() ? "MAXC" : "MAX", exprs.get(0), orders, syntax), syntax, typeEnv);
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
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
            return true;
        }
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            // in postgres somewhy in MIN function varchar is converted to text, and (anyelement, anyelement( functions can not implicitly cast text to varchar
            return type.getCast(getAggrSource(type instanceof ConcatenateType && syntax.hasAggConcProblem() ? "MINC" : "MIN", exprs.get(0), orders, syntax), syntax, typeEnv);
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
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
            return true;
        }
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource(syntax.getAnyValueFunc(), getSafeExprSource(0, exprs, exprReaders, type, syntax, typeEnv), orders, syntax);
        }
        public String name() {
            return "ANY";
        }
    };

    public static final GroupType CONCAT = new GroupType() {
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return type.getCast(getAggrSource("STRING_AGG" ,castToVarStrings(exprs, exprReaders, type, syntax, typeEnv).toString(","), orders, syntax), syntax, typeEnv);
        }
        public Type getType(Type exprType) {
            if(exprType == null)
                return null;

            return StringClass.getv(((StringClass)exprType).caseInsensitive, ExtInt.UNLIMITED);
        }
        public String name() {
            return "CONCAT";
        }
    };

    public static final GroupType AGGAR_SETADD = new GroupType() {
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource("AGGAR_SETADD", exprs.get(0), orders, syntax);
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
        public int getMainIndex(int props) {
            return 1;
        }
        public Expr getSingleExpr(ImList<Expr> exprs) {
            assert exprs.size()==2;
            return exprs.get(1).and(exprs.get(0).getWhere());
        }
        public boolean nullsNotAllowed() {
            return true;
        }
        public boolean isLastOpt(boolean needValue, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
            return true;
        }
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            assert exprs.size() == 2;
            return getAggrSource(syntax.getLastFunc(), getSafeExprSource(1, exprs, exprReaders, type, syntax, typeEnv), orders, syntax);
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
        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource((type instanceof JSONTextClass ? "JSON_AGG" : "JSONB_AGG"), exprs.get(0), orders, syntax);
        }
        public String name() {
            return "JSON_CONCAT";
        }
    };

    public static class Custom extends GroupType {
        public final String aggrFunc;
        public final boolean setOrdered;

        public final DataClass dataClass;
        public final boolean valueNull;

        public Custom(String aggrFunc, boolean setOrdered, DataClass dataClass, boolean valueNull) {
            this.aggrFunc = aggrFunc;
            this.setOrdered = setOrdered;

            this.dataClass = dataClass;
            this.valueNull = valueNull;
        }

        public String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv) {
            return getAggrSource(aggrFunc, exprs.toString(","), orders, syntax, setOrdered);
        }
        public String name() {
            return aggrFunc;
        }
        public boolean hasSingle() {
            return false;
        }

        public Type getType(Type exprType) {
            if(dataClass != null)
                return dataClass;

            return super.getType(exprType);
        }
        public Stat getTypeStat(Stat typeStat, boolean forJoin) {
            if(dataClass != null)
                return dataClass.getTypeStat(forJoin);

            return super.getTypeStat(typeStat, forJoin);
        }

        @Override
        public int getMainIndex(int props) {
            if(setOrdered)
                return props;

            return super.getMainIndex(props);
        }

        public boolean canBeNull() {
            return valueNull;
        }

        public boolean equals(Object o) {
            return this == o || o instanceof Custom && setOrdered == ((Custom) o).setOrdered && aggrFunc.equals(((Custom) o).aggrFunc) && BaseUtils.nullEquals(dataClass, ((Custom) o).dataClass) && valueNull == ((Custom) o).valueNull;
        }

        public int hashCode() {
            return Objects.hash(aggrFunc, setOrdered, dataClass, valueNull);
        }
    }
    public static GroupType CUSTOM(String aggrFunc, boolean setOrdered, DataClass dataClass, boolean valueNull) {
        return new Custom(aggrFunc, setOrdered, dataClass, valueNull);
    }

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

    public int getSkipWhereIndex() {
        return -1;
    }

    public boolean hasSingle() {
        return true;
    }
    public Expr getSingleExpr(ImList<Expr> exprs) {
        assert hasSingle();
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
    
    public boolean isLastOpt(boolean needValue, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders) {
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

    public abstract String getSource(ImList<String> exprs, ImList<ClassReader> exprReaders, ImOrderMap<String, CompileOrder> orders, Type type, SQLSyntax syntax, TypeEnvironment typeEnv);

    private static String getAggrSource(String fnc, String exprs, ImOrderMap<String, CompileOrder> orders, SQLSyntax syntax) {
        return getAggrSource(fnc, exprs, orders, syntax, false);
    }
    private static String getAggrSource(String fnc, String exprs, ImOrderMap<String, CompileOrder> orders, SQLSyntax syntax, boolean setOrdered) {
        return fnc + "(" + exprs + (setOrdered ? ") WITHIN GROUP (" : "") + BaseUtils.clause("ORDER BY", Query.stringOrder(orders, syntax)) + ")";
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

    public abstract String name();
}
