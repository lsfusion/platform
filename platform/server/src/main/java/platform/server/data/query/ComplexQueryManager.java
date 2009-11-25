package platform.server.data.query;

/*
@Aspect
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
            if(counter++>100 && counter%15==0 && System.currentTimeMillis()-millis>4000)
                throw new ComplexQueryException();
        }
    }

    @DeclareParents(value="platform.server.view.form.RemoteForm",defaultImpl=CounterImpl.class)
    private Counter counterInterface;
    @Before("call(* platform.server.view.form.RemoteForm.endApply(..)) && target(counter)")
    public void callBeforeStart(Counter counter) {
        counter.start();
    }
    @Before("cflow(execution(* platform.server.view.form.RemoteForm.endApply(..)) && target(counter)) && " +
            "call(platform.server.table.classes.where.MeanClassWheres platform.server.where.AbstractWhere.calculateMeanClassWheres())")
    public void callBeforeLazy(Counter counter) {
        counter.add();
    }


}
  */
