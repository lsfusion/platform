package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.ArrayInstancer;
import platform.server.data.Table;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.OrderExpr;
import platform.server.data.expr.query.TranslateContext;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.logics.BusinessLogics;

import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

abstract public class AbstractSourceJoin<T extends TranslateContext<T>> implements SourceJoin, TranslateContext<T> {

    public SourceJoin[] getEnum() {
        return new SourceJoin[]{this};
    }
   
    public abstract int hashContext(HashContext hashContext);

    boolean hashCoded = false;
    int hashCode;
    public int hashCode() {
        if(!hashCoded) {
            hashCode = hashContext(new HashContext() {
                public int hash(KeyExpr expr) {
                    return expr.hashCode();
                }
                public int hash(ValueExpr expr) {
                    return expr.hashCode();
                }
            });
            hashCoded = true;
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || obj!=null && getClass() == obj.getClass() && twins((AbstractSourceJoin) obj);
    }

    public abstract boolean twins(AbstractSourceJoin obj);

    protected static class ToString extends CompileSource  {
        public ToString(Set<ValueExpr> values, Set<KeyExpr> keys) {
            super(BaseUtils.mapString(values), BusinessLogics.debugSyntax);
            keySelect.putAll(BaseUtils.mapString(keys));
        }

        public String getSource(Table.Join.Expr expr) {
            return expr.toString();
        }

        public String getSource(Table.Join.IsIn where) {
            return where.toString();
        }

        public String getSource(GroupExpr groupExpr) {
            return groupExpr.toString();
        }

        public String getSource(OrderExpr orderExpr) {
            return orderExpr.toString();
        }
    }

    @Override
    public String toString() {
        return getSource(new ToString(enumValues(this),enumKeys(this)));
    }

    public void enumKeys(final Set<KeyExpr> keys) {
        enumerate(new SourceEnumerator() {
            @Override
            public void add(KeyExpr keyExpr) {
                keys.add(keyExpr);
            }
        });
    }

    public void enumValues(final Set<ValueExpr> values) {
        enumerate(new SourceEnumerator() {
            @Override
            public void add(ValueExpr keyExpr) {
                values.add(keyExpr);
            }
        });
    }

    public final static ArrayInstancer<SourceJoin> instancer = new ArrayInstancer<SourceJoin>() {
        public SourceJoin[] newArray(int size) {
            return new SourceJoin[size];
        }
    };

    public static SourceJoin[] merge(Collection<? extends SourceJoin> set,SourceJoin... array) {
        return BaseUtils.add(set.toArray(new SourceJoin[set.size()]),array,instancer);
    }
    
    public static Set<KeyExpr> enumKeys(Collection<? extends SourceJoin> set, SourceJoin... array) {
        return enumKeys(merge(set,array));
    }

    public static Set<KeyExpr> enumKeys(SourceJoin... array) {
        Set<KeyExpr> result = new HashSet<KeyExpr>();
        for(SourceJoin element : array)
            element.enumKeys(result);
        return result;
    }

    public static Set<ValueExpr> enumValues(Collection<? extends SourceJoin> set, SourceJoin... array) {
        return enumValues(merge(set,array));
    }

    public static Set<ValueExpr> enumValues(SourceJoin... array) {
        Set<ValueExpr> result = new HashSet<ValueExpr>();
        for(SourceJoin element : array)
            element.enumValues(result);
        return result;
    }
}
