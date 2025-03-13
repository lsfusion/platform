package lsfusion.server.base.controller.stack;

import lsfusion.base.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.io.PrintWriter;

public class NestedThreadException extends RuntimeException {
    private final ThrowableWithStack[] causes;

    public NestedThreadException(ThrowableWithStack[] causes) {
        this("Nested thread exception", causes);
    }

    public NestedThreadException(String message, ThrowableWithStack[] causes) {
        super(message);
        this.causes = causes;
    }

    public NestedThreadException(Throwable cause, ThrowableWithStack[] causes) {
        super(cause);
        this.causes = causes;
    }

    public String getAsyncStacks() {
        StringBuilder asyncStacks = new StringBuilder();
        for(ThrowableWithStack cause : causes) {
            if(asyncStacks.length() > 0)
                asyncStacks.append("\n");
            asyncStacks.append(cause.getString());
        }
        return asyncStacks.toString();
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
            String stack = cause.getLsfStack();
            if(!stack.isEmpty())
                s.println("LSF stack : " + stack);
            s.printStackTrace(throwable);
        }
    }

    public ThrowableWithStack[] getThrowables() {
        return causes;
    }

    private abstract static class WriterOrPrintStream {
        abstract void println(Object o);

        abstract void printStackTrace(Throwable t);
    }
}
