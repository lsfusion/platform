package platform.server.data.query;

import platform.base.AddSet;
import platform.base.BaseUtils;
import platform.server.data.Table;
import platform.server.data.where.DNFWheres;
import platform.server.data.translator.KeyTranslator;
import platform.server.data.expr.query.GroupJoin;
import platform.server.caches.hash.HashContext;
import platform.server.caches.Lazy;

import java.util.Collection;

import net.jcip.annotations.Immutable;

@Immutable
public class JoinSet extends AddSet<InnerJoin, JoinSet> implements DNFWheres.Interface<JoinSet> {

    JoinSet() {
    }

    JoinSet(InnerJoin[] iWheres) {
        super(iWheres);
    }

    JoinSet(InnerJoin where) {
        super(where);
    }

    protected JoinSet createThis(InnerJoin[] wheres) {
        return new JoinSet(wheres);
    }

    protected InnerJoin[] newArray(int size) {
        return new InnerJoin[size];
    }

    protected boolean containsAll(InnerJoin who, InnerJoin what) {
        return BaseUtils.hashEquals(who,what) || what.isIn(who.getJoinFollows());
    }

    public JoinSet and(JoinSet set) {
        return add(set);
    }

    boolean means(InnerJoin inner) {
        return means(new JoinSet(inner));
    }

    public boolean means(JoinSet set) {
        return equals(and(set));
    }

    void fillJoins(Collection<Table.Join> tables, Collection<GroupJoin> groups) {
        for(InnerJoin where : wheres)
            if (where instanceof Table.Join)
                tables.add((Table.Join) where);
            else
                groups.add((GroupJoin) where);
    }

    @Lazy
    public int hashContext(HashContext hashContext) {
        int hash = 0;
        for(InnerJoin where : wheres)
            hash += where.hashContext(hashContext);
        return hash;
    }

    public JoinSet translateDirect(KeyTranslator translator) {
        InnerJoin[] transJoins = new InnerJoin[wheres.length];
        for(int i=0;i<wheres.length;i++)
            transJoins[i] = wheres[i].translateDirect(translator);
        return new JoinSet(transJoins);
    }
}
