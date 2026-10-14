package lsfusion.interop.form.remote;

import lsfusion.interop.form.event.ChangeSelection;
import lsfusion.interop.form.property.EventSource;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// One of the form's own requests, as its remote interface takes it (RemoteFormInterface.executeBatch): a kind per
// request, holding the request's arguments, which the form makes in its turn (Requests). Every client sends such a
// request so - the web server and the desktop client alike, one alone as a batch of one
public abstract class BatchRequest implements Serializable {

    // made by the form: what the request does, on the execution stack of the batch's request
    public abstract <S> void make(Requests<S> requests, S stack) throws Exception;

    // ... and named with its arguments, as a remote method is, in the log of the remote call that carries it
    // (RemoteLoggerAspect) and on the execution stack
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ArgumentsStyle.INSTANCE);
    }

    // a value is given as its serialized bytes, which say nothing and can be many - a text a user enters -, so they are
    // named by their number (byte[26]); a collection as an array, for its values to be too
    private static final class ArgumentsStyle extends ToStringStyle {
        static final ArgumentsStyle INSTANCE = new ArgumentsStyle();

        private ArgumentsStyle() {
            setUseShortClassName(true);
            setUseIdentityHashCode(false);
            setNullText("null");
            setSizeStartText("byte[");
            setSizeEndText("]");
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, byte[] array) {
            appendSummary(buffer, fieldName, array);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Collection<?> coll) {
            appendDetail(buffer, fieldName, coll.toArray());
        }
    }

    // what the form makes a request with (RemoteForm implements it): the code the request runs, inside the remote call
    // of the batch that carries it. A method per kind, taking the execution stack it runs on (S: the server's, which
    // this module does not know) and the request
    public interface Requests<S> {
        void changeGroupObject(S stack, ChangeGroupObject request) throws Exception;

        void executeEventAction(S stack, ExecuteEventAction request) throws Exception;

        void expandGroupObject(S stack, ExpandGroupObject request) throws Exception;

        void collapseGroupObject(S stack, CollapseGroupObject request) throws Exception;

        void expandGroupObjectRecursive(S stack, ExpandGroupObjectRecursive request) throws Exception;

        void collapseGroupObjectRecursive(S stack, CollapseGroupObjectRecursive request) throws Exception;

        void setPropertyOrders(S stack, SetPropertyOrders request) throws Exception;

        void setUserFilters(S stack, SetUserFilters request) throws Exception;
    }

    public static final class ChangeGroupObject extends BatchRequest {
        public final int groupID;
        public final byte[] value;
        public final ChangeSelection changeSelection;

        public ChangeGroupObject(int groupID, byte[] value, ChangeSelection changeSelection) {
            this.groupID = groupID;
            this.value = value;
            this.changeSelection = changeSelection;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.changeGroupObject(stack, this);
        }
    }

    public static final class ExecuteEventAction extends BatchRequest {
        public final String actionSID;
        public final int[] propertyIDs;
        public final byte[][] fullKeys;
        public final EventSource[] eventSources;
        public final byte[][] pushAsyncResults;

        public ExecuteEventAction(String actionSID, int[] propertyIDs, byte[][] fullKeys, EventSource[] eventSources, byte[][] pushAsyncResults) {
            this.actionSID = actionSID;
            this.propertyIDs = propertyIDs;
            this.fullKeys = fullKeys;
            this.eventSources = eventSources;
            this.pushAsyncResults = pushAsyncResults;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.executeEventAction(stack, this);
        }
    }

    public static final class ExpandGroupObject extends BatchRequest {
        public final int groupID;
        public final byte[] value;

        public ExpandGroupObject(int groupID, byte[] value) {
            this.groupID = groupID;
            this.value = value;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.expandGroupObject(stack, this);
        }
    }

    public static final class CollapseGroupObject extends BatchRequest {
        public final int groupID;
        public final byte[] value;

        public CollapseGroupObject(int groupID, byte[] value) {
            this.groupID = groupID;
            this.value = value;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.collapseGroupObject(stack, this);
        }
    }

    public static final class ExpandGroupObjectRecursive extends BatchRequest {
        public final int groupID;
        public final boolean current;

        public ExpandGroupObjectRecursive(int groupID, boolean current) {
            this.groupID = groupID;
            this.current = current;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.expandGroupObjectRecursive(stack, this);
        }
    }

    public static final class CollapseGroupObjectRecursive extends BatchRequest {
        public final int groupID;
        public final boolean current;

        public CollapseGroupObjectRecursive(int groupID, boolean current) {
            this.groupID = groupID;
            this.current = current;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.collapseGroupObjectRecursive(stack, this);
        }
    }

    public static final class SetPropertyOrders extends BatchRequest {
        public final int groupObjectID;
        public final List<Integer> propertyList;
        public final List<byte[]> columnKeyList;
        public final List<Boolean> orderList;

        public SetPropertyOrders(int groupObjectID, List<Integer> propertyList, List<byte[]> columnKeyList, List<Boolean> orderList) {
            this.groupObjectID = groupObjectID;
            this.propertyList = propertyList;
            this.columnKeyList = columnKeyList;
            this.orderList = orderList;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.setPropertyOrders(stack, this);
        }
    }

    public static final class SetUserFilters extends BatchRequest {
        public final Map<Integer, byte[][]> filters;

        public SetUserFilters(Map<Integer, byte[][]> filters) {
            this.filters = filters;
        }

        public <S> void make(Requests<S> requests, S stack) throws Exception {
            requests.setUserFilters(stack, this);
        }
    }

    // a request of none of these kinds, carried by a batch all the same - its sender breaking the batch's contract (a
    // React view's: fn calls members only): made, it fails the batch on the form. Failed before, by the web server, the
    // batch would never reach the form, which would wait for its index, every later request held up behind it
    public static final class Unsupported extends BatchRequest {
        private final String name; // the request's, which the failure names

        public Unsupported(String name) {
            this.name = name;
        }

        public <S> void make(Requests<S> requests, S stack) {
            throw new UnsupportedOperationException(name + " is no request a batch carries: inside a batch, call a view's members only");
        }
    }
}
