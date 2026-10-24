package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.view.Column;
import lsfusion.gwt.client.view.MainFrame;

import java.util.*;
import java.util.function.Predicate;

// THE FORM'S CONTROLLER OF A TABLE'S USER FILTERS - a grid's group, or a tree's groups, one box for all -: one model,
// whoever draws them and whoever states them. It holds the WORKING list, its entries in their order, of which the
// active ones are what is sent, each group's in that order; the lists sent are the form's (Transport). A FILTER of the
// table's properties is a SLOT of it: it shows the first active entry on its column, wherever that stands, and, with
// none, its placeholder - an inactive entry holding its place and what it remembers of its condition. What draws the
// entries - the platform's rows, a view's parts, the filter controls - is a view of it, told of every change (View);
// what states them - the platform's rows, the inputs, a view's members, a pivot's
// drill-down, the server - goes through its operations. A write is sent at once, but in the manual apply mode, where it
// waits for the apply; what the platform's rows prepare - F3's value, another property chosen - waits in either mode
public final class GUserFiltersController {
    // what sends a table's list - each group's its own, in one request, its index returned -, and has each group's
    // list as it was sent last or reported: the form's (GFormController)
    public interface Transport {
        long changeFilters(List<GGroupObject> groups, ArrayList<GPropertyFilter> conditions);
        ArrayList<GPropertyFilterDTO> getFilterDTOs(GGroupObject group);
    }
    // ... whether the table's apply button is shown, which the manual apply mode asks for (GFilterControlsToggle)
    public interface ApplyControls {
        boolean isApplyShown();
    }
    // ... and what draws the model, told of every change: the request it was sent in, if it was, and the entries
    // whose change sent it, which their rows show loading till the answer
    public interface View {
        void changed(Change change);
    }
    public static final class Change {
        public final long requestIndex; // -1: nothing sent
        public final Collection<GUserFilter> sent;

        Change(long requestIndex, Collection<GUserFilter> sent) {
            this.requestIndex = requestIndex;
            this.sent = sent;
        }
    }
    private static final Change NOTHING_SENT = new Change(-1, Collections.emptyList());

    // a FILTER of the table's properties - one the design has -, its column - its property's, the key of the column
    // it is on where the property is grouped in columns (setSlotColumn) -, and what it remembers of the condition it
    // showed last: compared, negated and joined so
    private static final class Slot {
        final GFilter filter;
        Column column;
        GCompare compare;
        boolean negation;
        boolean junction = true;

        Slot(GFilter filter) {
            this.filter = filter;
            this.column = new Column(filter.property, GGroupObjectValue.EMPTY);
            this.compare = filter.property.getDefaultCompare();
        }
        void remember(GPropertyFilter condition) {
            compare = condition.compare;
            negation = condition.negation;
            junction = condition.junction;
        }
    }

    private final List<GGroupObject> groups;
    private final Transport transport;
    private final ApplyControls applyControls;
    private final ArrayList<GUserFilter> entries = new ArrayList<>();
    private final ArrayList<Slot> slots = new ArrayList<>();
    private final ArrayList<View> views = new ArrayList<>();

    // the table's groups, top down, and its FILTERs - those the design keeps, in the order it declares them, their
    // placeholders the list's first entries
    public GUserFiltersController(List<GGroupObject> groups, List<GFilter> filters, Transport transport, ApplyControls applyControls) {
        this.groups = groups;
        this.transport = transport;
        this.applyControls = applyControls;
        for (GFilter filter : filters)
            if (filter.container != null) // one the design removes is no slot
                slots.add(new Slot(filter));
        normalize();
    }
    // the user filters of a group in PANEL view, which has no table: none, nothing stated
    public static final GUserFiltersController NONE = new GUserFiltersController(Collections.emptyList(), Collections.emptyList(), null, () -> false);

    // ... a view of it told of every change - none of NONE, which nothing changes
    public void addView(View view) {
        if (!groups.isEmpty())
            views.add(view);
    }

    // ===== READ
    // the active entries' conditions, each group's in order, the groups top down: what is sent, what a view lists
    public ArrayList<GPropertyFilter> getConditions() {
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        for (GGroupObject group : groups)
            conditions.addAll(getConditions(group));
        return conditions;
    }
    private ArrayList<GPropertyFilter> getConditions(GGroupObject group) {
        ArrayList<GPropertyFilter> conditions = new ArrayList<>();
        for (GUserFilter entry : entries)
            if (entry.condition.groupObject == group && entry.isActive())
                conditions.add(entry.condition);
        return conditions;
    }
    // ... the entries the FILTERS box shows - none a slot shows, no placeholder -, in order
    public List<GUserFilter> getFreeEntries() {
        List<GUserFilter> free = new ArrayList<>();
        for (GUserFilter entry : entries)
            if (getSlot(entry) == null)
                free.add(entry);
        return free;
    }
    // ... what a FILTER shows: the first active entry on its column, else its placeholder; null for no slot of it
    public GUserFilter getSlotEntry(GFilter filter) {
        Slot slot = getSlot(filter);
        if (slot == null)
            return null;
        GUserFilter active = getFirstActive(slot.column);
        return active != null ? active : getPlaceholder(slot);
    }
    // ... the FILTER that shows an entry, or whose placeholder it is; null for one the box shows
    public GFilter getSlotOf(GUserFilter entry) {
        Slot slot = getSlot(entry);
        return slot != null ? slot.filter : null;
    }
    // ... the FILTER on a column, if any: an entry made active there is its while it shows none
    public GFilter getFilter(Column column) {
        Slot slot = getSlot(column);
        return slot != null ? slot.filter : null;
    }
    // ... the first entry on a column, whatever it is - active, inactive, a placeholder -: where the platform's F3 and
    // typing put a value
    public GUserFilter getFirstEntry(Column column) {
        for (GUserFilter entry : entries)
            if (entry.condition.column.equals(column))
                return entry;
        return null;
    }
    // ... what a value given a column starts from (GPropertyFilter.getBase): the first active entry on it, else - for a
    // FILTER's writers and F3, the slot's memory - the placeholder of the slot on it, else none: the default way.
    // `filters.change` with one condition takes no slot's memory
    public GPropertyFilter getValueBase(Column column, boolean memory) {
        GUserFilter active = getFirstActive(column);
        if (active != null)
            return active.condition;
        Slot slot = memory ? getSlot(column) : null;
        return slot != null ? getPlaceholder(slot).condition : null;
    }
    // ... whether an entry is the last of its group with a condition after it - its junction joins it to nothing then
    public boolean isLast(GUserFilter entry) {
        for (int i = entries.indexOf(entry) + 1; i < entries.size(); i++) {
            GUserFilter next = entries.get(i);
            if (next.condition.groupObject == entry.condition.groupObject && next.isActive())
                return false;
        }
        return true;
    }
    public boolean hasActive() {
        for (GUserFilter entry : entries)
            if (entry.isActive())
                return true;
        return false;
    }
    // ... whether the table has a FILTER - one the design keeps -: a row of it, whoever draws it, for good
    public boolean hasFilters() {
        return !slots.isEmpty();
    }
    // ... the manual apply mode: the client's setting, and the table's apply button shown - whoever draws it
    public boolean isManual() {
        return MainFrame.userFiltersManualApplyMode && applyControls.isApplyShown();
    }
    // ... and whether anything waits for the apply: the working list's active conditions, each group's in order, as
    // they are sent, other than the ones sent
    public boolean isPending() {
        for (GGroupObject group : groups)
            if (!GPropertyFilter.getFilterDTOs(getConditions(group)).equals(getSent(group)))
                return true;
        return false;
    }
    private ArrayList<GPropertyFilterDTO> getSent(GGroupObject group) {
        ArrayList<GPropertyFilterDTO> sent = transport.getFilterDTOs(group);
        return sent != null ? sent : new ArrayList<>();
    }

    // ===== WRITE - each sent, but in the manual apply mode (written); the commands sent whatever the mode (sent)

    // a list a writer states, its conditions in its order - the array of `filters.change`, Alt+F3 over a view's
    // conditions -: the entries it replaces (replace), every one but the placeholders, or, where the writer names only
    // some (nameable - a view, which names no property grouped in columns, of no integration name), the active
    // entries on those, the rest keeping their places
    public void changeConditions(List<GPropertyFilter> conditions, Predicate<GPropertyDraw> nameable) {
        if (groups.isEmpty())
            return;
        List<GUserFilter> replaced = new ArrayList<>();
        for (GUserFilter entry : entries)
            if (entry.placeholderOf == null && (nameable == null || (entry.isActive() && nameable.test(entry.condition.column.property))))
                replaced.add(entry);
        replace(replaced, conditions, false);
        normalize();
        written(Collections.emptyList());
    }
    // ... and a pivot's drill-down, every entry replaced, sent at once
    public void replaceConditions(List<GPropertyFilter> conditions) {
        if (groups.isEmpty())
            return;
        List<GUserFilter> replaced = new ArrayList<>();
        for (GUserFilter entry : entries)
            if (entry.placeholderOf == null)
                replaced.add(entry);
        replace(replaced, conditions, false);
        normalize();
        sent(Collections.emptyList());
    }
    // ... a group's list the server reports, which it has applied already - its entries replaced, each one its row
    // shows applied -; nothing sent
    public void updateFilters(GGroupObject group, List<GPropertyFilter> conditions) {
        if (groups.isEmpty())
            return;
        List<GUserFilter> replaced = new ArrayList<>();
        for (GUserFilter entry : entries)
            if (entry.condition.groupObject == group && entry.placeholderOf == null)
                replaced.add(entry);
        replace(replaced, conditions, true);
        normalize();
        changed(NOTHING_SENT);
    }

    // one condition a writer states - F3 over a view's conditions, `filters.change` with one condition and a value -:
    // in the place of the first active entry on its column, else in the placeholder of the slot on it, else appended; a
    // NULL value is the condition comparing with NULL
    public void putCondition(GPropertyFilter condition) {
        if (groups.isEmpty() || !isOfGroups(condition))
            return;
        GUserFilter target = getFirstActive(condition.column);
        if (target == null) {
            Slot slot = getSlot(condition.column);
            if (slot != null)
                target = getPlaceholder(slot);
        }
        if (target != null) {
            copy(condition, target, true);
            target.allowNull = condition.nullValue() || target.allowNull;
            target.placeholderOf = null;
        } else {
            target = newEntry(condition);
            entries.add(target);
        }
        target.confirmed = true;
        normalize();
        written(Collections.singletonList(target));
    }
    // ... and none on a column - `filters.change` with one condition and no value -: the first active entry on it
    // taken off, alone (a range's other bound stays); the last one a slot shows stays as its placeholder
    public void removeCondition(Column column) {
        if (groups.isEmpty())
            return;
        GUserFilter target = getFirstActive(column);
        if (target != null) {
            Slot slot = getSlot(target);
            if (slot != null)
                clearSlotEntry(slot, target);
            else
                entries.remove(target);
        }
        normalize();
        written(Collections.emptyList());
    }

    // a value a FILTER is given - its member, its row's value, F3 into it -: in the entry it shows, else in its
    // placeholder; its column the slot's
    public void changeFilter(GFilter filter, GPropertyFilter condition) {
        Slot slot = getSlot(filter);
        if (slot == null)
            return;
        GUserFilter target = getSlotEntry(filter);
        copy(condition, target, false);
        target.allowNull = condition.nullValue();
        target.placeholderOf = null;
        target.confirmed = true;
        normalize();
        written(Collections.singletonList(target));
    }
    // ... and none: the entry it shows taken off - the next one on its column shown then -, else that entry its
    // placeholder, in its place: a FILTER is never removed
    public void clearFilter(GFilter filter) {
        Slot slot = getSlot(filter);
        if (slot == null)
            return;
        GUserFilter target = getFirstActive(slot.column);
        if (target != null)
            clearSlotEntry(slot, target);
        normalize();
        written(Collections.emptyList());
    }

    // THE PLATFORM'S ROWS: a new entry - the controls' add, F3 on a column none is on -, its value the cell's,
    // prepared: not sent, not the user's yet (Escape takes it away); the box's, or the FILTER's on its column showing
    // none. One on a property of no group of the table's - a QUICKFILTER of a property of a group in PANEL view, in the
    // grid - is kept in its row all the same, left out of what is sent (sent)
    public GUserFilter add(GPropertyFilter condition) {
        if (groups.isEmpty())
            return null;
        GUserFilter entry = new GUserFilter(copyOf(condition), true);
        entries.add(entry);
        normalize();
        changed(NOTHING_SENT);
        return entry;
    }
    // ... a value put into an entry - F3 or typing into its row, the cell's -, prepared; a slot's entry left with no
    // condition - an empty cell's NULL, not allowed - taken off as the slot's clearing does (edit)
    public void prepareValue(GUserFilter entry, PValue value) {
        Slot slot = getSlot(entry);
        entry.condition.value.setValue(value);
        if (entry.isActive())
            entry.placeholderOf = null;
        else if (slot != null && entry.placeholderOf == null)
            clearSlotEntry(slot, entry);
        normalize();
        changed(NOTHING_SENT);
    }
    // ... a row's edit - its value committed (confirmed), its comparison, negation, NULL allowed, junction; another
    // property chosen, prepared (write false) -: the row's own condition taken into the entry. A slot's entry with no
    // condition left is taken off as the slot's clearing does
    public void edit(GUserFilter entry, GPropertyFilter row, boolean allowNull, boolean confirmed, boolean write) {
        Slot slot = getSlot(entry);
        copy(row, entry, true);
        entry.allowNull = allowNull;
        entry.confirmed |= confirmed;
        if (slot != null && !entry.isActive()) {
            if (entry.placeholderOf == null)
                clearSlotEntry(slot, entry);
        } else if (entry.isActive())
            entry.placeholderOf = null;
        normalize();
        if (write)
            written(Collections.singletonList(entry));
        else
            changed(NOTHING_SENT);
    }
    // ... a row of the box's deleted - its button, Escape in an editor no value was committed in -: sent at once
    public void remove(GUserFilter entry) {
        if (getSlot(entry) != null) // a FILTER is never removed
            return;
        entries.remove(entry);
        normalize();
        sent(Collections.emptyList());
    }

    // THE COMMANDS, sent at once whatever the mode: the reset - every entry of the box's off, every slot's entry its
    // placeholder, the ones that had a value marked loading -, and the apply of what waits
    public void reset() {
        if (groups.isEmpty())
            return;
        List<GUserFilter> cleared = new ArrayList<>();
        for (GUserFilter entry : new ArrayList<>(entries))
            if (getSlot(entry) == null)
                entries.remove(entry);
        for (Slot slot : slots) {
            GUserFilter shown = getFirstActive(slot.column);
            if (shown != null) {
                if (!shown.condition.nullValue())
                    cleared.add(shown);
                clearSlotEntry(slot, shown);
            }
        }
        normalize();
        sent(cleared);
    }
    public void apply() {
        if (groups.isEmpty())
            return;
        sent(Collections.emptyList());
    }

    // ===== THE MODE AND THE SLOTS' COLUMNS
    // the apply button shown or hidden: what draws the model told - what waits stays
    public void modeChanged() {
        changed(NOTHING_SENT);
    }
    // a FILTER's column, where the platform draws it on a property grouped in columns: the one the table is on as the
    // platform builds its row (GPlatformFilterController.build)
    public void setSlotColumn(GFilter filter, GGroupObjectValue columnKey) {
        Slot slot = getSlot(filter);
        if (slot == null)
            return;
        GUserFilter placeholder = getPlaceholder(slot);
        slot.column = new Column(filter.property, columnKey);
        if (placeholder != null)
            placeholder.condition.column = slot.column;
    }

    // ===== HOW
    private void written(Collection<GUserFilter> changed) {
        if (isManual())
            changed(NOTHING_SENT);
        else
            sent(changed);
    }
    private void sent(Collection<GUserFilter> changed) {
        for (GUserFilter entry : entries) // one of no group of the table's is not sent (add)
            if (entry.isActive() && !groups.contains(entry.condition.groupObject))
                sayLeftOut(entry.condition);
        long requestIndex = -1;
        if (isPending()) {
            requestIndex = transport.changeFilters(groups, getConditions());
            for (GUserFilter entry : entries) {
                entry.applied = entry.isActive();
                if (entry.applied)
                    entry.confirmed = true;
            }
        }
        changed(requestIndex >= 0 ? new Change(requestIndex, changed) : NOTHING_SENT);
    }
    private void changed(Change change) {
        for (View view : new ArrayList<>(views))
            view.changed(change);
    }

    // after every change: a slot with an active entry on its column has no placeholder, remembering that entry's
    // condition; one with none has its placeholder - one made appended -, remembering what it showed
    private void normalize() {
        for (Slot slot : slots) {
            GUserFilter active = getFirstActive(slot.column);
            GUserFilter placeholder = getPlaceholder(slot);
            if (active != null) {
                if (placeholder != null)
                    entries.remove(placeholder);
                slot.remember(active.condition);
            } else if (placeholder != null)
                slot.remember(placeholder.condition);
            else {
                GPropertyFilter condition = new GPropertyFilter(new GFilter(slot.column.property), slot.column.property.groupObject,
                        slot.column.columnKey, null, slot.negation, slot.compare, slot.junction);
                GUserFilter made = new GUserFilter(condition, false);
                made.placeholderOf = slot.filter;
                entries.add(made);
            }
        }
    }
    // a slot's entry with no condition: the next active one on its column is shown then, this one going; else it
    // is the slot's placeholder, in its place, remembering its condition
    private void clearSlotEntry(Slot slot, GUserFilter entry) {
        entry.condition.value.setValue(null);
        entry.allowNull = false;
        entry.applied = false;
        if (getFirstActive(slot.column) != null)
            entries.remove(entry);
        else {
            GUserFilter placeholder = getPlaceholder(slot);
            if (placeholder != null && placeholder != entry)
                entries.remove(placeholder);
            entry.placeholderOf = slot.filter;
        }
    }

    // the entries replaced by a list's conditions, in its order (contract 1): matched first to the unchanged ones in
    // order, then to the rest on the same column in order - a matched entry keeps its key, so what draws it keeps its
    // row and its editor -, the others new, the ones left over gone. The places of the replaced ones are filled in the
    // list's order, its extra conditions put after the last of them, so the entries not replaced keep theirs; the
    // placeholders of the groups' slots after them all, a value given an empty FILTER following the list. A condition
    // of no group of the table's is left out, and said on the console
    private void replace(List<GUserFilter> replaced, List<GPropertyFilter> conditions, boolean reported) {
        List<GPropertyFilter> stated = new ArrayList<>();
        for (GPropertyFilter condition : conditions)
            if (isOfGroups(condition))
                stated.add(condition);

        GUserFilter[] matched = new GUserFilter[stated.size()];
        Set<GUserFilter> used = new HashSet<>();
        for (int i = 0; i < matched.length; i++)
            for (GUserFilter entry : replaced)
                if (!used.contains(entry) && same(entry.condition, stated.get(i))) {
                    matched[i] = entry;
                    used.add(entry);
                    break;
                }
        for (int i = 0; i < matched.length; i++)
            if (matched[i] == null)
                for (GUserFilter entry : replaced)
                    if (!used.contains(entry) && entry.condition.column.equals(stated.get(i).column)) {
                        matched[i] = entry;
                        used.add(entry);
                        break;
                    }
        for (int i = 0; i < matched.length; i++) {
            GPropertyFilter condition = stated.get(i);
            GUserFilter entry = matched[i];
            if (entry == null)
                entry = newEntry(condition);
            else {
                copy(condition, entry, true);
                if (condition.nullValue())
                    entry.allowNull = true;
            }
            entry.placeholderOf = null;
            entry.confirmed = true;
            if (reported)
                entry.applied = entry.isActive();
            matched[i] = entry;
        }

        Set<GUserFilter> places = new HashSet<>(replaced);
        ArrayList<GUserFilter> result = new ArrayList<>();
        int next = 0, after = -1;
        for (GUserFilter entry : entries)
            if (places.contains(entry)) {
                if (next < matched.length) {
                    result.add(matched[next++]);
                    after = result.size();
                }
            } else
                result.add(entry);
        if (after < 0)
            after = result.size();
        for (int i = next; i < matched.length; i++)
            result.add(after++, matched[i]);
        Set<GGroupObject> replacedGroups = new HashSet<>();
        for (GUserFilter entry : replaced)
            replacedGroups.add(entry.condition.groupObject);
        for (GPropertyFilter condition : stated)
            replacedGroups.add(condition.groupObject);
        List<GUserFilter> placeholders = new ArrayList<>();
        for (GUserFilter entry : result)
            if (entry.placeholderOf != null && replacedGroups.contains(entry.condition.groupObject))
                placeholders.add(entry);
        result.removeAll(placeholders);
        result.addAll(placeholders);
        entries.clear();
        entries.addAll(result);
    }
    private static boolean same(GPropertyFilter entry, GPropertyFilter condition) {
        return entry.column.equals(condition.column) && entry.compare == condition.compare && entry.negation == condition.negation
                && entry.junction == condition.junction && Objects.equals(entry.value.value, condition.value.value);
    }
    // a new entry of a condition stated: the NULL allowed where it compares with NULL, or where the box shows it - a
    // free row's own default -, not where a slot shows it (a FILTER's default)
    private GUserFilter newEntry(GPropertyFilter condition) {
        return new GUserFilter(copyOf(condition), condition.nullValue() || getSlot(condition.column) == null);
    }
    private static GPropertyFilter copyOf(GPropertyFilter condition) {
        return new GPropertyFilter(new GFilter(condition.column.property), condition.groupObject, condition.column.columnKey,
                condition.value.value, condition.negation, condition.compare, condition.junction);
    }
    // a condition's fields taken into an entry's - its column too, but where a slot's value is given its column stays
    private static void copy(GPropertyFilter condition, GUserFilter entry, boolean column) {
        if (column) {
            entry.condition.column = condition.column;
            entry.condition.groupObject = condition.groupObject;
        }
        entry.condition.compare = condition.compare;
        entry.condition.negation = condition.negation;
        entry.condition.junction = condition.junction;
        entry.condition.value.setValue(condition.value.value);
    }
    private boolean isOfGroups(GPropertyFilter condition) {
        if (condition.groupObject != null && groups.contains(condition.groupObject))
            return true;
        sayLeftOut(condition);
        return false;
    }
    private void sayLeftOut(GPropertyFilter condition) {
        GwtClientUtils.consoleError("the filters of " + GPropertyFilter.getNames(groups) + ": the condition on '"
                + condition.column.property.sID + "' is on a property "
                + (condition.groupObject != null ? "of group '" + condition.groupObject.getSID() + "'" : "of no group")
                + ", which is none of theirs, so it is not sent");
    }

    private GUserFilter getFirstActive(Column column) {
        for (GUserFilter entry : entries)
            if (entry.condition.column.equals(column) && entry.isActive())
                return entry;
        return null;
    }
    private Slot getSlot(GFilter filter) {
        for (Slot slot : slots)
            if (slot.filter == filter)
                return slot;
        return null;
    }
    private Slot getSlot(Column column) {
        for (Slot slot : slots)
            if (slot.column.equals(column))
                return slot;
        return null;
    }
    // the slot an entry is of: the one whose placeholder it is, or the one that shows it - it is the first active entry
    // on the slot's column -; null for an entry the box shows
    private Slot getSlot(GUserFilter entry) {
        if (entry.placeholderOf != null)
            return getSlot(entry.placeholderOf);
        Slot slot = getSlot(entry.condition.column);
        return slot != null && getFirstActive(slot.column) == entry ? slot : null;
    }
    private GUserFilter getPlaceholder(Slot slot) {
        for (GUserFilter entry : entries)
            if (entry.placeholderOf == slot.filter)
                return entry;
        return null;
    }
}
