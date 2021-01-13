package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.base.view.ColorUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static lsfusion.gwt.client.base.view.grid.AbstractDataGridBuilder.COLUMN_CLASS;

public class GCalendar extends GTippySimpleStateTableView implements ColorThemeChangeListener {

    private final String calendarDateType;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;

        MainFrame.addColorThemeChangeListener(this);
    }

    @Override
    public int getDefaultPageSize() {
        return 10;
    }

    @Override
    protected void render(Element element, JsArray<JavaScriptObject> list) {
        if (calendar == null) {
            //fullcalendar bug - https://github.com/fullcalendar/fullcalendar/issues/5863
            //to prevent this when calendar-element height less then ~350px
            element.getParentElement().getStyle().setProperty("overflow", "auto");
            element.getStyle().setProperty("minHeight", "400px");
            element.getStyle().setProperty("cursor", "default");
            String locale = LocaleInfo.getCurrentLocale().getLocaleName();

            calendar = createCalendar(element, controller, calendarDateType, locale);
        }
        updateEvents(list);
    }

    @Override
    public void onResize() {
        if (calendar != null)
            resize(calendar);
    }

    protected native void resize(JavaScriptObject calendar)/*-{
        calendar.updateSize();
    }-*/;

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject controller, String calendarDateType, String locale)/*-{
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth',
            height: 'parent',
            timeZone: 'UTC',
            locale: locale,
            firstDay: 1,
            initialDate: controller.getCurrentDay(calendarDateType),
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: calendarDateType.includes('dateTime') ? 'dayGridMonth,dayGridWeek,timeGridDay' : 'dayGridMonth,dayGridWeek'
            },
            dayMaxEvents: true,
            //to prevent the expand of a single event without "end"-param to the next day "nextDayThreshold" should be equal to "defaultTimedEventDuration", which by default is 01:00:00
            nextDayThreshold: '01:00:00',
            eventDisplay: 'block', // no dots and correct work of backgroundColor/borderColor
            eventOrder: 'start,index',
            eventChange: function (info) {
                if (!@GCalendar::isUpdating) {
                    changeProperty(info, 'start');
                    changeProperty(info, 'end');
                }
            },
            datesSet: function () {
                var filterLeftBorder = parseCalendarDateElement(calendar.view.activeStart);
                var filterRightBorder = parseCalendarDateElement(calendar.view.activeEnd);
                controller.setViewFilter(filterLeftBorder.year, filterLeftBorder.month, filterLeftBorder.day, filterRightBorder.year,
                    filterRightBorder.month, filterRightBorder.day, calendarDateType, calendarDateType.toLowerCase().includes('time'), 1000);
            },
            eventClick: function (info) {
                changeCurrentEvent(info.event, info.el);
            }
        });
        calendar.render();
        return calendar;

        function changeCurrentEvent(newEvent, elementClicked) {
            controller.changeSimpleGroupObject(newEvent.extendedProps.object, true, elementClicked); // we're rerendering current event below
            @GCalendar::highlightEvent(*)(calendar, newEvent.extendedProps.key);
        }

        function changeProperty(info, position) {
            var currentEvent = info.event;
            var oldEvent = info.oldEvent;
            if (getTime(currentEvent[position]) !== getTime(oldEvent[position])) {
                var propertyName = currentEvent.extendedProps[position + 'FieldName'];
                var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
                var eventElement = parseCalendarDateElement(currentEvent[position]);
                controller[controllerFunction](propertyName, info.event.extendedProps.object, eventElement.year,
                    eventElement.month, eventElement.day, eventElement.hour, eventElement.minute, eventElement.second);
            }
        }

        function getTime(eventTime) {
            //eventTime can be null due to property dateTo(dateTimeTo) can be null
            return eventTime != null ? eventTime.getTime() : null;
        }

        function parseCalendarDateElement(element) {
            return {
                year: element.getFullYear(),
                month: element.getMonth() + 1,
                day: element.getUTCDate(),
                hour: element.getUTCHours(),
                minute: element.getUTCMinutes(),
                second: element.getUTCSeconds()
            }
        }
    }-*/;

    private final NativeHashMap<GGroupObjectValue, Event> events = new NativeHashMap<>();
    private class Event {

        public final String title;
        public final String start;
        public final String end;
        public final boolean editable;
        public final boolean durationEditable;
        public final boolean allDay;
        public final String startFieldName;
        public final String endFieldName;
        public final int index;
        public final GGroupObjectValue key;
        public final JavaScriptObject object;
        public final String backgroundColor;
        public final String foregroundColor;

        public Event(JavaScriptObject object, int index) {
            String endEventFieldName = calendarDateType.contains("From") ? calendarDateType.replace("From", "To") : null;

            title = getTitle(object, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()));
            start = getStart(object, calendarDateType);
            end = endEventFieldName != null ? getEnd(object, endEventFieldName): null;
            editable = isEditable(object, controller, calendarDateType, endEventFieldName);
            durationEditable = isDurationEditable(object, controller, endEventFieldName);
            allDay = calendarDateType.equals("date") || calendarDateType.equals("dateFrom");
            startFieldName = calendarDateType;
            endFieldName = endEventFieldName;
            this.index = index;
            this.key = getKey(object);
            this.object = object;
            backgroundColor = getBackgroundColor(object, controller);
            foregroundColor = getForegroundColor(object, controller);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private static boolean isUpdating = false;

    private void updateEvents(JsArray<JavaScriptObject> list) {
        NativeHashMap<GGroupObjectValue, Event> oldEvents = new NativeHashMap<>();
        oldEvents.putAll(events);

        int fullUpdateLimit = 10;
        List<Runnable> eventsToAdd = new ArrayList<>();
        NativeHashMap<GGroupObjectValue, Consumer<JavaScriptObject>> eventsToUpdate = new NativeHashMap<>();

        for (int i = 0; i < list.length(); i++) {
            if (eventsToAdd.size() + eventsToUpdate.size() > fullUpdateLimit)
                break;

            JavaScriptObject object = list.get(i);
            GGroupObjectValue key = getKey(object);

            Event event = new Event(object, i);
            events.put(key, event);

            Event oldEvent = oldEvents.remove(key);
            if (oldEvent == null)
                eventsToAdd.add(() -> addSingleCalendarEvent(calendar, createJsEvent(event, false)));
            else
                updateActionMap(eventsToUpdate, key, event, oldEvent);
        }
        oldEvents.foreachEntry((key, event) -> {
            events.remove(key);
            eventsToUpdate.put(key, this::removeSingleCalendarEvent);
        });

        GGroupObjectValue currentKey = getCurrentKey();

        if (eventsToAdd.size() + eventsToUpdate.size() > fullUpdateLimit) {
            events.clear();
            for (int i = 0; i < list.length(); i++) {
                JavaScriptObject object = list.get(i);
                events.put(getKey(object), new Event(object, i));
            }
            setCalendarEvents(calendar, createCalendarEventsObject(events));
        } else {
            isUpdating = true;
            eventsToAdd.forEach(Runnable::run);
            processUpdateEvents(eventsToUpdate);
            highlightEvent(calendar, currentKey);
            isUpdating = false;
        }

        setCalendarCurrentEventId(calendar, currentKey);
    }

    private void processUpdateEvents(NativeHashMap<GGroupObjectValue, Consumer<JavaScriptObject>> eventsToUpdate) {
        JsArray<JavaScriptObject> calendarEvents = getCalendarEvents(calendar);
        for (int i = 0; i < calendarEvents.length(); i++) {
            JavaScriptObject eventToUpdate = calendarEvents.get(i);
            Consumer<JavaScriptObject> updateAction = eventsToUpdate.remove(getCalendarEventKey(eventToUpdate));
            if (updateAction != null)
                updateAction.accept(eventToUpdate);
        }
    }

    private void updateActionMap(NativeHashMap<GGroupObjectValue, Consumer<JavaScriptObject>> eventsToUpdate, GGroupObjectValue key, Event event, Event oldEvent) {
        Consumer<JavaScriptObject> updateAction = null;

        if (oldEvent.title == null || !oldEvent.title.equals(event.title))
            updateAction = addUpdateAction(calendarEvent -> updateCalendarProperty("title", event.title, calendarEvent), null);

        if (oldEvent.start != null && !oldEvent.start.equals(event.start))
            updateAction = addUpdateAction(calendarEvent -> updateStart(event.start, calendarEvent), updateAction);

        if (oldEvent.end != null && !oldEvent.end.equals(event.end))
            updateAction = addUpdateAction(calendarEvent -> updateEnd(event.end, calendarEvent), updateAction);

        if (oldEvent.editable != event.editable)
            updateAction = addUpdateAction(calendarEvent -> updateCalendarProperty("editable", event.editable, calendarEvent), updateAction);

        if (oldEvent.durationEditable != event.durationEditable)
            updateAction = addUpdateAction(calendarEvent -> updateCalendarProperty("durationEditable", event.durationEditable, calendarEvent), updateAction);

        if (oldEvent.index != event.index)
            updateAction = addUpdateAction(calendarEvent -> updateCalendarExtendedProperty("index", event.index, calendarEvent), updateAction);

        if (!GwtClientUtils.nullEquals(oldEvent.backgroundColor, event.backgroundColor))
            updateAction = addUpdateAction(calendarEvent -> {
                updateCalendarProperty("backgroundColor", getDisplayBackgroundColor(event.backgroundColor, false), calendarEvent);
                updateCalendarExtendedProperty("sourceBackgroundColor", event.backgroundColor, calendarEvent);
            }, updateAction);

        if (!GwtClientUtils.nullEquals(oldEvent.foregroundColor, event.foregroundColor))
            updateAction = addUpdateAction(calendarEvent -> updateCalendarExtendedProperty("foregroundColor", ColorUtils.getDisplayColor(event.foregroundColor), calendarEvent), updateAction);

        if (updateAction != null)
            eventsToUpdate.put(key, updateAction);
    }

    private Consumer<JavaScriptObject> addUpdateAction(Consumer<JavaScriptObject> action, Consumer<JavaScriptObject> updateAction) {
        return updateAction == null ? action : updateAction.andThen(action);
    }

    private JsArray<JavaScriptObject> createCalendarEventsObject(NativeHashMap<GGroupObjectValue, Event> events){
        JsArray<JavaScriptObject> calendarEvents = JavaScriptObject.createArray().cast();
        events.foreachValue(event -> calendarEvents.push(createJsEvent(event, isCurrentObjectKey(event.object))));
        return calendarEvents;
    }

    protected native void setCalendarCurrentEventId(JavaScriptObject calendar, GGroupObjectValue currentEventId)/*-{
        calendar.currentEventId = currentEventId;
    }-*/;

    protected native JsArray<JavaScriptObject> getCalendarEvents(JavaScriptObject calendar)/*-{
        return calendar.getEvents();
    }-*/;

    protected native GGroupObjectValue getCalendarEventKey(JavaScriptObject event)/*-{
        return event.extendedProps.key;
    }-*/;

    protected native void removeSingleCalendarEvent(JavaScriptObject event)/*-{
        event.remove();
    }-*/;

    protected native void addSingleCalendarEvent(JavaScriptObject calendar, JavaScriptObject event)/*-{
        calendar.addEvent(event, true); //true, assign event to the first event source
    }-*/;

    protected static native void highlightEvent(JavaScriptObject calendar, GGroupObjectValue id)/*-{
        @GCalendar::isUpdating = true;
        var calendarEvents = calendar.getEvents();

        for (var i = 0; i < calendarEvents.length; i++) {
            var calendarEvent = calendarEvents[i];
            var key = calendarEvent.extendedProps.key;
            if (calendar.currentEventId != null && @lsfusion.gwt.client.base.GwtClientUtils::nullEquals(*)(key, calendar.currentEventId)) {
                calendarEvent.setProp('classNames', @GCalendar::getClassNames(*)(false));
                calendarEvent.setProp('backgroundColor', @GSimpleStateTableView::getDisplayBackgroundColor(*)(calendarEvent.extendedProps.sourceBackgroundColor, false))
                calendarEvent.setProp('borderColor', null);
            }

            if (@lsfusion.gwt.client.base.GwtClientUtils::nullEquals(*)(key, id)) {
                calendarEvent.setProp('classNames', @GCalendar::getClassNames(*)(true));
                calendarEvent.setProp('backgroundColor', @GSimpleStateTableView::getDisplayBackgroundColor(*)(calendarEvent.extendedProps.sourceBackgroundColor, true))
                calendarEvent.setProp('borderColor', @lsfusion.gwt.client.view.StyleDefaults::getFocusedCellBorderColor()());
            }
        }
        calendar.currentEventId = id;
        @GCalendar::isUpdating = false;
    }-*/;

    protected native void setCalendarEvents(JavaScriptObject calendar, JsArray<JavaScriptObject> events)/*-{
        calendar.setOption('events', events);
    }-*/;

    protected native void updateCalendarProperty(String propertyName, Object property, JavaScriptObject event)/*-{
        event.setProp(propertyName, property);
    }-*/;

    protected native void updateCalendarExtendedProperty(String propertyName, Object property, JavaScriptObject event)/*-{
        event.setExtendedProp(propertyName, property);
    }-*/;

    protected native void updateStart(String start, JavaScriptObject event)/*-{
        event.setStart(start, {
            maintainDuration: true  //if set to true, the event’s end will also be adjusted in order to keep the same duration
        });
    }-*/;

    protected native void updateEnd(String end, JavaScriptObject event)/*-{
        event.setEnd(end);
    }-*/;

    private JavaScriptObject createJsEvent(Event event, boolean isCurrentKey){
        return createEventAsJs(event.title, event.start, event.end, event.editable, event.durationEditable,
                event.allDay, event.key, event.startFieldName, event.endFieldName, event.index, event.object, isCurrentKey, event.backgroundColor, event.foregroundColor);
    }

    protected Element getCellParent(Element target) {
        return GwtClientUtils.getParentWithClass(target, COLUMN_CLASS);
    }

    private static String getClassNames(boolean isCurrentKey) {
        return COLUMN_CLASS + (isCurrentKey ? " event-highlight" : "");
    }

    protected native JavaScriptObject createEventAsJs(String title, String start, String end, boolean editable, boolean durationEditable, boolean allDay, GGroupObjectValue key,
                                                      String startFieldName, String endFieldName, int index, JavaScriptObject object, boolean  isCurrentKey,
                                                      String backgroundColor, String foregroundColor)/*-{
        return {
            title: title,
            start: start,
            end: end,
            editable: editable,
            durationEditable: durationEditable,
            allDay: allDay,
            startFieldName: startFieldName,
            endFieldName: endFieldName,
            index: index,
            key: key,
            object: object,
            sourceBackgroundColor: backgroundColor,
            sourceTextColor: foregroundColor,

            classNames: @GCalendar::getClassNames(*)(isCurrentKey),
            backgroundColor: @GSimpleStateTableView::getDisplayBackgroundColor(*)(backgroundColor, isCurrentKey),
            textColor: @lsfusion.gwt.client.base.view.ColorUtils::getDisplayColor(Ljava/lang/String;)(foregroundColor),
            borderColor: isCurrentKey ? @lsfusion.gwt.client.view.StyleDefaults::getFocusedCellBorderColor()() : null
        };
    }-*/;

    protected native String getTitle(JavaScriptObject object, JsArray<JavaScriptObject> columns)/*-{
        var title = '';
        for (var i = 0; i < columns.length; i++) {
            if (title !== '')
                continue;
            title = columns[i] === 'name' ? object[columns[i]] : '';
        }
        if (title === '' && columns.length >= 2) {
            for (var k = 0; k <= 2; k++) {
                var value = object[columns[k]];
                if (value != null)
                    title = title !== '' ? title + ' - ' + value : value;
            }
        }
        return title;
    }-*/;

    protected native String getStart(JavaScriptObject object, String startEventFieldName)/*-{
        return object[startEventFieldName];
    }-*/;

    protected native String getEnd(JavaScriptObject object, String endEventFieldName)/*-{
        return object[endEventFieldName];
    }-*/;

    protected native boolean isEditable(JavaScriptObject object, JavaScriptObject controller, String startEventFieldName, String endEventFieldName)/*-{
        return !controller.isPropertyReadOnly(startEventFieldName, object) && (endEventFieldName == null || !controller.isPropertyReadOnly(endEventFieldName, object));
    }-*/;

    protected native boolean isDurationEditable(JavaScriptObject object, JavaScriptObject controller, String endEventFieldName)/*-{
        return endEventFieldName !== null && !controller.isPropertyReadOnly(endEventFieldName, object);
    }-*/;

    protected native String getBackgroundColor(JavaScriptObject object, JavaScriptObject controller)/*-{
        return controller.getGroupObjectBackgroundColor(object);
    }-*/;

    protected native String getForegroundColor(JavaScriptObject object, JavaScriptObject controller)/*-{
        return controller.getGroupObjectForegroundColor(object);
    }-*/;

    @Override
    public void colorThemeChanged() {
        updateEventThemeColors(calendar);
    }

    protected native void updateEventThemeColors(JavaScriptObject calendar)/*-{
        var events = calendar.getEvents();
        for (var i = 0; i < events.length; i++) {
            var event = events[i];
            var isCurrentKey = @lsfusion.gwt.client.base.GwtClientUtils::nullEquals(*)(calendar.currentEventId, event.extendedProps.id);
            var displayBackgroundColor = @GSimpleStateTableView::getDisplayBackgroundColor(*)(event.extendedProps.sourceBackgroundColor, isCurrentKey);
            if (displayBackgroundColor) {
                event.setProp('backgroundColor', displayBackgroundColor)
            }
            if (event.extendedProps.sourceTextColor) {
                event.setProp('textColor', @lsfusion.gwt.client.base.view.ColorUtils::getDisplayColor(Ljava/lang/String;)(event.extendedProps.sourceTextColor))
            }
            if (isCurrentKey) {
                event.setProp('borderColor', @lsfusion.gwt.client.view.StyleDefaults::getFocusedCellBorderColor()());
            }
        }
    }-*/;
}
