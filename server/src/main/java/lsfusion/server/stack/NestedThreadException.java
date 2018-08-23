package lsfusion.server.stack;

import java.io.PrintStream;
import java.io.PrintWriter;

public class NestedThreadException extends RuntimeException {
    private final ThrowableWithStack[] causes;
    
    public NestedThreadException(ThrowableWithStack[] causes) {
        super("Nested thread exception");
        this.causes = causes;
    }

    private abstract static class WriterOrPrintStream {
        abstract void println(Object o);
        
        abstract void printStackTrace(Throwable t);
    }

    private static class WrapPrintStream extends WriterOrPrintStream {
        private final PrintStream printStream;

        WrapPrintStream(PrintStream printStream) {
            this.printStream = printStream;
        }

        void println(Object o) {
            printStream.println(o);
        }

        void printStackTrace(Throwable t) {
            t.printStackTrace(printStream);
        }
    }

    private static class WrapPrintWriter extends WriterOrPrintStream {
        private final PrintWriter printWriter;

        WrapPrintWriter(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        void println(Object o) {
            printWriter.println(o);
        }

        void printStackTrace(Throwable t) {
            t.printStackTrace(printWriter);
        }
    }

    @Override
    public void printStackTrace(PrintStream s) {
        synchronized (s) {
            super.printStackTrace(s);
            printStackTrace(new WrapPrintStream(s));
        }
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        synchronized (s) {
            super.printStackTrace(s);
            printStackTrace(new WrapPrintWriter(s));
        }
    }

    private void printStackTrace(WriterOrPrintStream s) {

        for(ThrowableWithStack cause : causes) {
            Throwable throwable = cause.getThrowable();
            s.println("Exception : " + throwable);
            String stack = cause.getStack();
            if(!stack.isEmpty())
                s.println("LSF stack : " + stack);
            s.printStackTrace(throwable);
        }
    }

    public ThrowableWithStack[] getThrowables() {
        return causes;
    }
}
