package platformlocal;

import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public interface Where {

    DNFWhere getDNFWhere();
    CNFWhere getCNFWhere();

    Where translate(Translator translator);

    boolean means(Where where);

    OuterWhere getOr();
    boolean isFalse();
    boolean isTrue();

    String getSource(Map<QueryData, String> joinAlias, SQLSyntax syntax);
    void fillData(Collection<DataWhere> wheres);
    IntraWhere getJoinWhere();
}

abstract class OperandWhere implements Where {

    Collection<Where> operands = new ArrayList();

    OperandWhere(Where where) {
        operands.add(where);
    }

    OperandWhere(Where where1, Where where2) {
        operands.add(where1);
        operands.add(where2);
    }

    DNFWhere cachedDNFWhere;
    public DNFWhere getDNFWhere() {
        if (cachedDNFWhere == null) cachedDNFWhere = calculateDNFWhere();
        return cachedDNFWhere;
    }

    abstract DNFWhere calculateDNFWhere();

    CNFWhere cachedCNFWhere;
    public CNFWhere getCNFWhere() {
        if (cachedCNFWhere == null) cachedCNFWhere = calculateCNFWhere();
        return cachedCNFWhere;
    }
    abstract CNFWhere calculateCNFWhere();

    public Where translate(Translator translator) {

        // сначала транслируем AndWhere
        List<Where> transWheres = new ArrayList<Where>();
        boolean changedWheres = false;

        for(Where where : operands) {
            Where transWhere = where.translate(translator);
            transWheres.add(transWhere);
            changedWheres = changedWheres || (transWhere != where);
        }

        if(!changedWheres)
            return this;

        return createWhere(transWheres);
    }

    abstract Where createWhere(List<Where> wheres);

    public boolean means(Where where) {
        return getDNFWhere().means(where.getDNFWhere());
    }

    public OuterWhere getOr() {
        return getDNFWhere().getOr();
    }

    public boolean isFalse() {
        return getDNFWhere().isFalse();
    }

    public boolean isTrue() {
        return getCNFWhere().isTrue();
    }

    public String getSource(Map<QueryData, String> joinAlias, SQLSyntax syntax) {
        return getDNFWhere().getSource(joinAlias, syntax);
    }

    public void fillData(Collection<DataWhere> wheres) {
        for (Where operand : operands)
            operand.fillData(wheres);
    }

    public IntraWhere getJoinWhere() {
        return getDNFWhere().getJoinWhere();
    }
}

class OrWhere extends OperandWhere {

    Where where1, where2;

    OrWhere(Where iwhere1, Where iwhere2) {
        super(iwhere1, iwhere2);

        where1 = iwhere1;
        where2 = iwhere2;
    }

    DNFWhere calculateDNFWhere() {
        return where1.getDNFWhere().or(where2.getDNFWhere());
    }

    CNFWhere calculateCNFWhere() {
        return where1.getCNFWhere().or(where2.getCNFWhere());
    }

    Where createWhere(List<Where> wheres) {
        return new OrWhere(wheres.get(0), wheres.get(1));
    }
}

class AndWhere extends OperandWhere {

    Where where1, where2;

    AndWhere(Where iwhere1, Where iwhere2) {
        super(iwhere1, iwhere2);

        where1 = iwhere1;
        where2 = iwhere2;
    }

    DNFWhere calculateDNFWhere() {
        return where1.getDNFWhere().and(where2.getDNFWhere());
    }

    CNFWhere calculateCNFWhere() {
        return where1.getCNFWhere().and(where2.getCNFWhere());
    }

    Where createWhere(List<Where> wheres) {
        return new AndWhere(wheres.get(0), wheres.get(1));
    }
}

class NotWhere extends OperandWhere {

    Where where;

    NotWhere(Where iwhere) {
        super(iwhere);

        where = iwhere;
    }

    DNFWhere calculateDNFWhere() {
        return where.getCNFWhere().reverseNot();
    }

    CNFWhere calculateCNFWhere() {
        return where.getDNFWhere().reverseNot();
    }

    Where createWhere(List<Where> wheres) {
        return new NotWhere(wheres.get(0));
    }
}

class FollowFalseWhere extends OperandWhere {

    Where where1, where2;

    FollowFalseWhere(Where iwhere1, Where iwhere2) {
        super(iwhere1, iwhere2);

        where1 = iwhere1;
        where2 = iwhere2;
    }

    DNFWhere calculateDNFWhere() {
        return where1.getDNFWhere().followFalse(where2.getDNFWhere());
    }

    CNFWhere calculateCNFWhere() {
        return where1.getCNFWhere().followFalse(where2.getCNFWhere());
    }

    Where createWhere(List<Where> wheres) {
        return new FollowFalseWhere(wheres.get(0), wheres.get(1));
    }
}

class FollowTrueWhere extends OperandWhere {

    Where where1, where2;

    FollowTrueWhere(Where iwhere1, Where iwhere2) {
        super(iwhere1, iwhere2);

        where1 = iwhere1;
        where2 = iwhere2;
    }

    DNFWhere calculateDNFWhere() {
        return where1.getDNFWhere().followTrue(where2.getDNFWhere());
    }

    CNFWhere calculateCNFWhere() {
        return where1.getCNFWhere().followTrue(where2.getCNFWhere());
    }

    Where createWhere(List<Where> wheres) {
        return new FollowTrueWhere(wheres.get(0), wheres.get(1));
    }
}