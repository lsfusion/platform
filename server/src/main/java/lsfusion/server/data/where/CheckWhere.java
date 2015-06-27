package lsfusion.server.data.where;

import lsfusion.server.caches.TranslateContext;

public interface CheckWhere<T extends TranslateContext<T>> extends TranslateContext<T> {

    boolean isTrue();
    boolean isFalse();
    
    boolean checkTrue();
    boolean directMeansFrom(AndObjectWhere where);
    boolean directMeansFromNot(AndObjectWhere[] notWheres, boolean[] used, int skip);

    boolean means(CheckWhere where);
    
    CheckWhere andCheck(CheckWhere where); // чисто для means
    CheckWhere orCheck(CheckWhere where); // чисто для means

    AndObjectWhere[] getAnd(); // protected

    CheckWhere not();
}
