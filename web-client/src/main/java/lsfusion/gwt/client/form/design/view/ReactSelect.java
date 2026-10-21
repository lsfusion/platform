package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import lsfusion.gwt.client.form.object.table.grid.view.GSimpleStateTableView;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.GSelect;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RendererType;

import java.util.ArrayList;

import static lsfusion.gwt.client.base.GwtClientUtils.*;

// A SELECTION A REACT VIEW DRAWS (GReactFormData.ReactPropertyEntry): its cell holds the list of options the server
// sends (PropertyDrawEntity.getSelectProperty), not the value shown. A pick changes it one option at a time, as the
// platform's element does (select.js). `value` is the view of the option picked, of several values the views of the
// options checked joined by the separator - the property's own value where its GROUP CONCAT joins them so
final class ReactSelect {

    // ===== the read: a cell's options, as a view is given them
    static void emit(JavaScriptObject target, GPropertyDraw property, PValue cell) {
        JavaScriptObject options = toSelect(getList(property, cell)), select = newObject();
        setField(select, "list", options);
        setFlag(select, "multi", property.select.multi);
        setFlag(select, "notNull", property.select.notNull);
        if (property.select.multi) // its GROUP CONCAT's, or a comma
            setField(select, "separator", property.select.separator);
        setField(target, "select", select);
        setField(target, "value", property.select.multi ? getCheckedValue(options, property.select.separator) : getPickedValue(options));
    }

    // ===== the write: what a call picks - against the options the cell holds, a pick not answered yet included -, added
    // to the changes, and the options the cell shows with it, returned
    static PValue change(String errorPrefix, GPropertyDraw property, PValue cell, JavaScriptObject value, ArrayList<PValue> changes) {
        JavaScriptObject list = getList(property, cell);
        boolean[] checked = getChecked(errorPrefix, property, list, value);
        addToggles(property, list, checked, changes);
        return getShown(property, list, checked);
    }
    // ... the options that change, one at a time, the checks first; of one value the server unpicks the option picked as
    // it writes the new one (select.js sends the click alone), so an uncheck goes only to clear - with none picked from
    // now on: picked again, an option of a view several options are picked by (an explicit change taking the view) is no
    // uncheck of the others
    private static void addToggles(GPropertyDraw property, JavaScriptObject list, boolean[] checked, ArrayList<PValue> changes) {
        boolean unchecks = property.select.multi || !isAnyChecked(checked);
        ArrayList<PValue> unchecked = new ArrayList<>();
        for (int index = 0, size = jsArrayLength(list); index < size; index++) {
            JavaScriptObject option = jsArrayGet(list, index);
            boolean check = checked[index];
            if (check != hasProperty(option, "selected") && (check || unchecks))
                (check ? changes : unchecked).add(getPick(property, getField(option, "objects"), getField(option, "name"), check));
        }
        changes.addAll(unchecked);
    }
    private static boolean isAnyChecked(boolean[] checked) {
        for (boolean check : checked)
            if (check)
                return true;
        return false;
    }
    // ... the options, each picked as `checked` says of its index
    private static PValue getShown(GPropertyDraw property, JavaScriptObject list, boolean[] checked) {
        JavaScriptObject shownList = emptyArray();
        for (int index = 0, size = jsArrayLength(list); index < size; index++) {
            JavaScriptObject shownOption = copyObject(jsArrayGet(list, index));
            setFlag(shownOption, "selected", checked[index]);
            push(shownList, shownOption);
        }
        return GSimpleStateTableView.convertFromJSValue(property.getRenderType(RendererType.SIMPLE), shownList);
    }
    // the cell's options as the server sends them, none where it holds nothing
    private static JavaScriptObject getList(GPropertyDraw property, PValue value) {
        JavaScriptObject list = GSimpleStateTableView.convertToJSValue(property, value, RendererType.SIMPLE, true);
        return isUndefinedOrNull(list) ? emptyArray() : list;
    }
    // the options checked from now on, by index: of one value the option named, none for null; of several those of the
    // array, or of their value split by the separator, none for null or '', or with an option alone checked or unchecked
    // - the opposite of what the cell shows. Anything else is refused before a send
    private static boolean[] getChecked(String errorPrefix, GPropertyDraw property, JavaScriptObject list, JavaScriptObject value) {
        GSelect select = property.select;
        boolean[] checked = new boolean[jsArrayLength(list)];
        if (select.multi && isOption(value)) {
            for (int index = 0; index < checked.length; index++)
                checked[index] = hasProperty(jsArrayGet(list, index), "selected");
            int toggled = findOption(errorPrefix, property, list, value);
            checked[toggled] = !checked[toggled];
            return checked;
        }
        JavaScriptObject named;
        if (!select.multi) {
            if (select.notNull && isUndefinedOrNull(value))
                throw refusal(errorPrefix + "'" + property.integrationSID + "' has to have a value: pick one of its options");
            named = toArray(value);
        } else if (isJSArray(value))
            named = value;
        else if (isUndefinedOrNull(value) || isJSString(value)) {
            named = getNamedValues(value, select.separator);
            if (jsArrayLength(named) > 1 && isSplitAmbiguous(list, select.separator)) // split, it could name other options
                throw refusal(errorPrefix + "the views of the options of '" + property.integrationSID + "' do not split back"
                        + " from its value by its separator '" + select.separator + "' one way only: pass the array of its"
                        + " options checked from now on");
        } else
            throw refusal(errorPrefix + "'" + property.integrationSID + "' is a selection of several values: pass their value"
                    + " - the values of its options joined by '" + select.separator + "' -, the array of its options checked"
                    + " from now on, or one of them, to check or uncheck");
        for (int i = 0, size = jsArrayLength(named); i < size; i++)
            checked[findOption(errorPrefix, property, list, jsArrayGet(named, i))] = true;
        return checked;
    }
    // the index of the option named: an option - an element of `select.list` - by its key, as a row is named by its key;
    // a value by its view, which several options may share (SELECT does not make views unique)
    private static int findOption(String errorPrefix, GPropertyDraw property, JavaScriptObject list, JavaScriptObject named) {
        int index;
        if (isOption(named))
            index = getKeyIndex(list, getOwnField(named, "key"));
        else { // a value
            index = getValueIndex(list, named);
            if (index == -2)
                throw refusal(errorPrefix + "several options of '" + property.integrationSID + "' have the value "
                        + jsonStringify(named) + ": pass the option itself, an element of its select.list");
        }
        if (index == -1)
            throw refusal(errorPrefix + "'" + property.integrationSID + "' has no option " + jsonStringify(named));
        return index;
    }
    // ... an option: an element of `select.list`, which has its key
    private static boolean isOption(JavaScriptObject value) {
        return isJSObject(value) && hasOwnField(value, "key");
    }
    // one option picked or no longer, as select.js sends it: the change of the cell by its objects - or, where an
    // explicit change takes the option's view, that view, none for no longer (select.js _changeSingleProperty)
    private static PValue getPick(GPropertyDraw property, JavaScriptObject objects, JavaScriptObject name, boolean picked) {
        return GSimpleStateTableView.convertFromJSUndefValue(property, property.select.changeValue
                ? (picked ? getOptionName(name) : null) : getSelectedChange(objects, picked));
    }

    // ===== the JS of the read
    private static native JavaScriptObject toSelect(JavaScriptObject list) /*-{
        return list.map(function (option) {
            var choice = {
                key: @lsfusion.gwt.client.form.design.view.ReactSelect::getOptionKey(*)(option.objects),
                value: @lsfusion.gwt.client.form.design.view.ReactSelect::getOptionValue(*)(option)
            };
            if (option.selected)
                choice.selected = true;
            return choice;
        });
    }-*/;
    // an option's key: the id of its one object, or the JSON of the ids of several (a selection over a group by several)
    private static native JavaScriptObject getOptionKey(JavaScriptObject objects) /*-{
        var ids = Object.keys(objects).map(function (object) { return objects[object]; });
        return ids.length === 1 ? ids[0] : JSON.stringify(ids);
    }-*/;
    // ... its value: its view as text, as select.js shows it (_getName), null where it has none
    private static native JavaScriptObject getOptionValue(JavaScriptObject option) /*-{
        return option.name == null ? null : String(option.name);
    }-*/;
    // the value of the option picked, the first where several are - an explicit change taking the option's view picks
    // every option of that view (Property.getSelectProperty) -, null where none is
    private static native JavaScriptObject getPickedValue(JavaScriptObject options) /*-{
        var picked = options.find(function (option) { return option.selected; });
        return picked ? picked.value : null;
    }-*/;
    // ... and of several values the values of the options checked joined by the separator, as their GROUP CONCAT joins
    // them, one with no view left out; null where none is
    private static native JavaScriptObject getCheckedValue(JavaScriptObject options, String separator) /*-{
        var values = options.filter(function (option) { return option.selected && option.value !== null; })
            .map(function (option) { return option.value; });
        return values.length ? values.join(separator) : null;
    }-*/;

    // ===== ... and of the write: the option of a key, -1 where none is
    private static native int getKeyIndex(JavaScriptObject list, JavaScriptObject key) /*-{
        return list.findIndex(function (option) {
            return String(@lsfusion.gwt.client.form.design.view.ReactSelect::getOptionKey(*)(option.objects)) === String(key);
        });
    }-*/;
    // ... the option of a view, -1 where none is, -2 where several are
    private static native int getValueIndex(JavaScriptObject list, JavaScriptObject value) /*-{
        var found = -1;
        for (var i = 0; i < list.length; i++)
            if (@lsfusion.gwt.client.form.design.view.ReactSelect::getOptionValue(*)(list[i]) === value) {
                if (found >= 0)
                    return -2;
                found = i;
            }
        return found;
    }-*/;
    // whether views joined by the separator could split back more ways than one: an empty separator, or a view with it
    // inside, or one ending with its start or starting with its end
    private static native boolean isSplitAmbiguous(JavaScriptObject list, String separator) /*-{
        if (separator === '')
            return true;
        return list.some(function (option) {
            var view = @lsfusion.gwt.client.form.design.view.ReactSelect::getOptionValue(*)(option);
            if (view === null)
                return false;
            if (view.indexOf(separator) >= 0)
                return true;
            for (var i = 1; i < separator.length; i++)
                if (view.endsWith(separator.substring(0, i)) || view.startsWith(separator.substring(i)))
                    return true;
            return false;
        });
    }-*/;
    // the views a value of several values names, split by the separator: none of null or ''
    private static native JavaScriptObject getNamedValues(JavaScriptObject value, String separator) /*-{
        return value === undefined || value === null || value === '' ? [] : value.split(separator);
    }-*/;
    // the change of the cell picking an option or no longer (select.js changeProperty('selected', ...))
    private static native JavaScriptObject getSelectedChange(JavaScriptObject objects, boolean picked) /*-{
        return { property: 'selected', objects: objects, value: picked ? true : null };
    }-*/;
    // ... where an explicit change takes the option's view: that view as text, '' for none (select.js _getName)
    private static native JavaScriptObject getOptionName(JavaScriptObject name) /*-{
        return name != null ? String(name) : '';
    }-*/;
}
