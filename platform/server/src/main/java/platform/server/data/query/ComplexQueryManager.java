package platform.server.data.query;

import org.aspectj.lang.annotation.Aspect;

/*@Aspect
public class ComplexQueryManager {

    public interface Counter {
        void start();
        void add();
    }
    public static class CounterImpl implements Counter {
        private int counter = 0;
        long millis = 0;

        public void start() {
            counter = 0;
            millis = System.currentTimeMillis();
        }

        public void add() {
            if(counter++>10000 && counter%100==0 && System.currentTimeMillis()-millis>4000)
                throw new ComplexQueryException();
        }
    }
    @DeclareParents(value="platform.server.data.query.JoinQuery",defaultImpl=CounterImpl.class)
    private Counter counterInterface;

    @Before("cflow(execution(* platform.server.view.form.RemoteForm.endApply(..))) && " +
            "call(* platform.server.data.query.JoinQuery.executeSelect(..)) && target(query)) ")
    public void callBeforeStart(Counter query) {
        query.start();
    }

    @Before("cflow(execution(* platform.server.view.form.RemoteForm.endApply(..))) && " +
            "cflow(execution(* platform.server.data.query.JoinQuery.executeSelect(..)) && target(query)) && (" +
            "execution(* platform.server.where.Where.translate(..)) || " +
            "execution(* platform.server.data.query.exprs.SourceExpr.translate(..)))")
    public void callBeforeTranslate(Counter query) {
        query.add();
    }

}

*/
