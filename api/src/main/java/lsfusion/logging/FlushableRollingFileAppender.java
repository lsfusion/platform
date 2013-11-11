package lsfusion.logging;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.QuietWriter;
import org.apache.log4j.rolling.RollingPolicy;
import org.apache.log4j.rolling.RolloverDescription;
import org.apache.log4j.rolling.TriggeringPolicy;
import org.apache.log4j.rolling.helper.Action;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.xml.UnrecognizedElementHandler;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Properties;

/**
 * c/p from org.apache.log4j.rolling.RollingFileAppender to add force flushing
 */
public final class FlushableRollingFileAppender extends FileAppender implements UnrecognizedElementHandler {
    /**
     * Triggering policy.
     */
    private TriggeringPolicy triggeringPolicy;

    /**
     * Rolling policy.
     */
    private RollingPolicy rollingPolicy;

    /**
     * Length of current active log file.
     */
    private long fileLength = 0;

    /**
     * Asynchronous action (like compression) from previous rollover.
     */
    private Action lastRolloverAsyncAction = null;


    /**
     * Construct a new instance.
     */
    public FlushableRollingFileAppender() {
    }

    /**
     * Prepare instance of use.
     */
    public void activateOptions() {
        if (rollingPolicy == null) {
            LogLog.warn(
                    "Please set a rolling policy for the RollingFileAppender named '"
                    + getName() + "'");

            return;
        }

        //
        //  if no explicit triggering policy and rolling policy is both.
        //
        if (
                (triggeringPolicy == null) && rollingPolicy instanceof TriggeringPolicy) {
            triggeringPolicy = (TriggeringPolicy) rollingPolicy;
        }

        if (triggeringPolicy == null) {
            LogLog.warn(
                    "Please set a TriggeringPolicy for the RollingFileAppender named '"
                    + getName() + "'");

            return;
        }

        Exception exception = null;

        synchronized (this) {
            triggeringPolicy.activateOptions();
            rollingPolicy.activateOptions();

            try {
                RolloverDescription rollover =
                        rollingPolicy.initialize(getFile(), getAppend());

                if (rollover != null) {
                    Action syncAction = rollover.getSynchronous();

                    if (syncAction != null) {
                        syncAction.execute();
                    }

                    setFile(rollover.getActiveFileName());
                    setAppend(rollover.getAppend());
                    lastRolloverAsyncAction = rollover.getAsynchronous();

                    if (lastRolloverAsyncAction != null) {
                        Thread runner = new Thread(lastRolloverAsyncAction);
                        runner.start();
                    }
                }

                File activeFile = new File(getFile());

                if (getAppend()) {
                    fileLength = activeFile.length();
                } else {
                    fileLength = 0;
                }

                super.activateOptions();
            } catch (Exception ex) {
                exception = ex;
            }
        }

        if (exception != null) {
            LogLog.warn(
                    "Exception while initializing RollingFileAppender named '" + getName()
                    + "'", exception);
        }
    }


    private QuietWriter createQuietWriter(final Writer writer) {
        ErrorHandler handler = errorHandler;
        if (handler == null) {
            handler = new DefaultErrorHandler(this);
        }
        return new QuietWriter(writer, handler);
    }


    /**
     Implements the usual roll over behaviour.

     <p>If <code>MaxBackupIndex</code> is positive, then files
     {<code>File.1</code>, ..., <code>File.MaxBackupIndex -1</code>}
     are renamed to {<code>File.2</code>, ...,
     <code>File.MaxBackupIndex</code>}. Moreover, <code>File</code> is
     renamed <code>File.1</code> and closed. A new <code>File</code> is
     created to receive further log output.

     <p>If <code>MaxBackupIndex</code> is equal to zero, then the
     <code>File</code> is truncated with no backup files created.

     * @return true if rollover performed.
     */
    public boolean rollover() {
        //
        //   can't roll without a policy
        //
        if (rollingPolicy != null) {
            Exception exception = null;

            synchronized (this) {
                //
                //   if a previous async task is still running
                //}
                if (lastRolloverAsyncAction != null) {
                    //
                    //  block until complete
                    //
                    lastRolloverAsyncAction.close();

                    //
                    //    or don't block and return to rollover later
                    //
                    //if (!lastRolloverAsyncAction.isComplete()) return false;
                }

                try {
                    RolloverDescription rollover = rollingPolicy.rollover(getFile());

                    if (rollover != null) {
                        if (rollover.getActiveFileName().equals(getFile())) {
                            closeWriter();

                            boolean success = true;

                            if (rollover.getSynchronous() != null) {
                                success = false;

                                try {
                                    success = rollover.getSynchronous().execute();
                                } catch (Exception ex) {
                                    exception = ex;
                                }
                            }

                            if (success) {
                                if (rollover.getAppend()) {
                                    fileLength = new File(rollover.getActiveFileName()).length();
                                } else {
                                    fileLength = 0;
                                }

                                if (rollover.getAsynchronous() != null) {
                                    lastRolloverAsyncAction = rollover.getAsynchronous();
                                    new Thread(lastRolloverAsyncAction).start();
                                }

                                setFile(
                                        rollover.getActiveFileName(), rollover.getAppend(),
                                        bufferedIO, bufferSize);
                            } else {
                                setFile(
                                        rollover.getActiveFileName(), true, bufferedIO, bufferSize);

                                if (exception == null) {
                                    LogLog.warn("Failure in post-close rollover action");
                                } else {
                                    LogLog.warn(
                                            "Exception in post-close rollover action", exception);
                                }
                            }
                        } else {
                            Writer newWriter =
                                    createWriter(
                                            new FileOutputStream(
                                                    rollover.getActiveFileName(), rollover.getAppend()));
                            closeWriter();
                            setFile(rollover.getActiveFileName());
                            this.qw = createQuietWriter(newWriter);

                            boolean success = true;

                            if (rollover.getSynchronous() != null) {
                                success = false;

                                try {
                                    success = rollover.getSynchronous().execute();
                                } catch (Exception ex) {
                                    exception = ex;
                                }
                            }

                            if (success) {
                                if (rollover.getAppend()) {
                                    fileLength = new File(rollover.getActiveFileName()).length();
                                } else {
                                    fileLength = 0;
                                }

                                if (rollover.getAsynchronous() != null) {
                                    lastRolloverAsyncAction = rollover.getAsynchronous();
                                    new Thread(lastRolloverAsyncAction).start();
                                }
                            }

                            writeHeader();
                        }

                        return true;
                    }
                } catch (Exception ex) {
                    exception = ex;
                }
            }

            if (exception != null) {
                LogLog.warn(
                        "Exception during rollover, rollover deferred.", exception);
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected void subAppend(final LoggingEvent event) {
        // The rollover check must precede actual writing. This is the
        // only correct behavior for time driven triggers.
        if (
                triggeringPolicy.isTriggeringEvent(
                        this, event, getFile(), getFileLength())) {
            //
            //   wrap rollover request in try block since
            //    rollover may fail in case read access to directory
            //    is not provided.  However appender should still be in good
            //     condition and the append should still happen.
            try {
                rollover();
            } catch (Exception ex) {
                LogLog.warn("Exception during rollover attempt.", ex);
            }
        }

        super.subAppend(event);
    }

    /**
     * Get rolling policy.
     * @return rolling policy.
     */
    public RollingPolicy getRollingPolicy() {
        return rollingPolicy;
    }

    /**
     * Get triggering policy.
     * @return triggering policy.
     */
    public TriggeringPolicy getTriggeringPolicy() {
        return triggeringPolicy;
    }

    /**
     * Sets the rolling policy.
     * @param policy rolling policy.
     */
    public void setRollingPolicy(final RollingPolicy policy) {
        rollingPolicy = policy;
    }

    /**
     * Set triggering policy.
     * @param policy triggering policy.
     */
    public void setTriggeringPolicy(final TriggeringPolicy policy) {
        triggeringPolicy = policy;
    }

    /**
     * Close appender.  Waits for any asynchronous file compression actions to be completed.
     */
    public void close() {
        synchronized (this) {
            if (lastRolloverAsyncAction != null) {
                lastRolloverAsyncAction.close();
            }
        }

        super.close();
    }

    public synchronized void flush() {
        synchronized (this) {
            this.qw.flush();
        }
    }

    /**
     Returns an OutputStreamWriter when passed an OutputStream.  The
     encoding used will depend on the value of the
     <code>encoding</code> property.  If the encoding value is
     specified incorrectly the writer will be opened using the default
     system encoding (an error message will be printed to the loglog.
     @param os output stream, may not be null.
     @return new writer.
     */
    protected OutputStreamWriter createWriter(final OutputStream os) {
        return super.createWriter(new CountingOutputStream(os, this));
    }

    /**
     * Get byte length of current active log file.
     * @return byte length of current active log file.
     */
    public long getFileLength() {
        return fileLength;
    }

    /**
     * Increments estimated byte length of current active log file.
     * @param increment additional bytes written to log file.
     */
    public synchronized void incrementFileLength(int increment) {
        fileLength += increment;
    }



    /**
     * {@inheritDoc}
     */
    public boolean parseUnrecognizedElement(final Element element,
                                            final Properties props) throws Exception {
        final String nodeName = element.getNodeName();
        if ("rollingPolicy".equals(nodeName)) {
            OptionHandler rollingPolicy =
                    org.apache.log4j.extras.DOMConfigurator.parseElement(
                            element, props, RollingPolicy.class);
            if (rollingPolicy != null) {
                rollingPolicy.activateOptions();
                this.setRollingPolicy((RollingPolicy) rollingPolicy);
            }
            return true;
        }
        if ("triggeringPolicy".equals(nodeName)) {
            OptionHandler triggerPolicy =
                    org.apache.log4j.extras.DOMConfigurator.parseElement(
                            element, props, TriggeringPolicy.class);
            if (triggerPolicy != null) {
                triggerPolicy.activateOptions();
                this.setTriggeringPolicy((TriggeringPolicy) triggerPolicy);
            }
            return true;
        }
        return false;
    }

    /**
     * Wrapper for OutputStream that will report all write
     * operations back to this class for file length calculations.
     */
    private static class CountingOutputStream extends OutputStream {
        /**
         * Wrapped output stream.
         */
        private final OutputStream os;

        /**
         * Rolling file appender to inform of stream writes.
         */
        private final FlushableRollingFileAppender rfa;

        /**
         * Constructor.
         * @param os output stream to wrap.
         * @param rfa rolling file appender to inform.
         */
        public CountingOutputStream(
                final OutputStream os, final FlushableRollingFileAppender rfa) {
            this.os = os;
            this.rfa = rfa;
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            os.close();
        }

        /**
         * {@inheritDoc}
         */
        public void flush() throws IOException {
            os.flush();
        }

        /**
         * {@inheritDoc}
         */
        public void write(final byte[] b) throws IOException {
            os.write(b);
            rfa.incrementFileLength(b.length);
        }

        /**
         * {@inheritDoc}
         */
        public void write(final byte[] b, final int off, final int len)
                throws IOException {
            os.write(b, off, len);
            rfa.incrementFileLength(len);
        }

        /**
         * {@inheritDoc}
         */
        public void write(final int b) throws IOException {
            os.write(b);
            rfa.incrementFileLength(1);
        }
    }

    private static final class DefaultErrorHandler implements ErrorHandler {
        private final FlushableRollingFileAppender appender;
        public DefaultErrorHandler(final FlushableRollingFileAppender appender) {
            this.appender = appender;
        }
        /**@since 1.2*/
        public void setLogger(Logger logger) {

        }
        public void error(String message, Exception ioe, int errorCode) {
            appender.close();
            LogLog.error("IO failure for appender named "+ appender.getName(), ioe);
        }
        public void error(String message) {

        }
        /**@since 1.2*/
        public void error(String message, Exception e, int errorCode, LoggingEvent event) {

        }
        /**@since 1.2*/
        public void setAppender(Appender appender) {

        }
        /**@since 1.2*/
        public void setBackupAppender(Appender appender) {

        }

        public void activateOptions() {
        }

    }

}
