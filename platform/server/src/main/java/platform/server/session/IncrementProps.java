package platform.server.session;

import platform.server.data.translator.MapValuesTranslate;
import platform.server.logics.property.Property;

import java.util.Set;

public abstract class IncrementProps<T> extends AbstractIncrementProps<T, IncrementProps.UsedChanges> {

    protected IncrementProps() {
    }

    protected IncrementProps(Set<Property> noUpdate) {
        super(noUpdate);
    }

    public static class UsedChanges extends AbstractIncrementProps.UsedChanges<UsedChanges> {

        private UsedChanges() {
        }
        protected final static UsedChanges EMPTY = new UsedChanges();

        public <T> UsedChanges(IncrementProps<T> modifier) {
            super(modifier);
        }

        public UsedChanges(Property property, SessionTableUsage<?, Property> incrementTable) {
            super(property, incrementTable);
        }

        public UsedChanges(Property property) {
            super(property);
        }

        private UsedChanges(UsedChanges changes, Changes merge) {
            super(changes, merge);
        }
        protected UsedChanges calculateAddChanges(Changes changes) {
            return new UsedChanges(this, changes);
        }

        public UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
        }
        protected UsedChanges calculateAdd(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges usedChanges, MapValuesTranslate mapValues) {
            super(usedChanges, mapValues);
        }
        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }
    }

    protected UsedChanges newUsedChanges(Property property, SessionTableUsage<T, Property> incrementTable) {
        return new UsedChanges(property, incrementTable);
    }

    protected UsedChanges newUsedChanges(Property property) {
        return new UsedChanges(property);
    }

    public UsedChanges newChanges() {
        return UsedChanges.EMPTY;
    }

    protected UsedChanges newFullChanges() {
        return new UsedChanges(this);
    }
}
