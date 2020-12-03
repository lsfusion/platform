package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

import java.util.HashMap;
import java.util.Map;

public class GCalendar extends GTippySimpleStateTableView {

    private final String calendarDateType;
    private final String endEventFieldName;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
        this.endEventFieldName = calendarDateType.contains("From") ? calendarDateType.replace("From", "To") : null;
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
            eventOrder: 'start,index',
            eventChange: function (info) {
                changeProperty(info, 'start', this.objects);
                changeProperty(info, 'end', this.objects);
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
            var newEventIndex = newEvent.id;
            var newObject = calendar.objects[newEventIndex];

            controller.changeSimpleGroupObject(newObject, true, elementClicked); // we're rerendering current event below

            var oldEvent = calendar.currentEventIndex != null ? calendar.getEvents()[calendar.currentEventIndex] : null;
            if (oldEvent !== null)
                oldEvent.setProp('classNames', '');
            newEvent.setProp('classNames', 'event-highlight');
            calendar.currentEventIndex = newEventIndex;
        }

        function changeProperty(info, position, objects) {
            var currentEvent = info.event;
            var oldEvent = info.oldEvent;
            if (currentEvent[position] !== null && oldEvent[position] !== null && currentEvent[position].getTime() !== oldEvent[position].getTime()) {
                var propertyName = currentEvent.extendedProps[position + 'FieldName'];
                var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
                var eventElement = parseCalendarDateElement(currentEvent[position]);
                controller[controllerFunction](propertyName, objects[currentEvent.id], eventElement.year,
                    eventElement.month, eventElement.day, eventElement.hour, eventElement.minute, eventElement.second);
            }
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

    private final Map<GGroupObjectValue, Event> events = new HashMap<>();

    private class Event {

        public final String title;
        public final String start;
        public final String end;
        public final boolean editable;
        public final boolean durationEditable;
        public final boolean allDay;
        public final int id;
        public final String startFieldName;
        public final String endFieldName;
        public final boolean isCurrentKey;
        public final String classNames;

        public Event(JavaScriptObject object, int id) {
            title = getTitle(object, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()));
            start = getStart(object, calendarDateType);
            end = endEventFieldName != null ? getEnd(object, endEventFieldName): null;
            editable = isEditable(object, controller, calendarDateType, endEventFieldName);
            durationEditable = isDurationEditable(object, controller, endEventFieldName);
            allDay = calendarDateType.equals("date") || calendarDateType.equals("dateFrom");
            this.id = id;
            startFieldName = calendarDateType;
            endFieldName = endEventFieldName;
            isCurrentKey = isCurrentObjectKey(object);
            classNames = isCurrentKey ? "event-highlight" : "";
        }
    }

    private void updateEvents(JsArray<JavaScriptObject> list) {
        Map<GGroupObjectValue, Event> oldEvents = new HashMap<>(events);
        for (int i = 0; i < list.length(); i++) {
            JavaScriptObject object = list.get(i);
            GGroupObjectValue key = getKey(object);

            Event event = new Event(object, i);

            Event oldEvent = oldEvents.remove(key);

            if (oldEvent == null) {
                events.put(key, event);
                addEvent(calendar, event.title, event.start, event.end, event.editable, event.durationEditable,
                        event.allDay, event.id, event.startFieldName, event.endFieldName, event.isCurrentKey, event.classNames);
                continue;
            }

            if (!oldEvent.title.equals(event.title))
                updateCalendarProperty(calendar, "title", event.title, oldEvent.id);

            if (!oldEvent.start.equals(event.start))
                updateStart(calendar, event.start, oldEvent.id);

            if (oldEvent.end != null && !oldEvent.end.equals(event.end))
                updateEnd(calendar, event.end, oldEvent.id);

            if (oldEvent.editable != event.editable)
                updateCalendarProperty(calendar, "editable", event.editable, oldEvent.id);

            if (oldEvent.durationEditable != event.durationEditable)
                updateCalendarProperty(calendar, "durationEditable", event.durationEditable, oldEvent.id);

            if (!oldEvent.classNames.equals(event.classNames))
                updateCalendarProperty(calendar, "classNames", event.classNames, oldEvent.id);
        }
//        if (!oldEvents.isEmpty()) {
//            oldEvents.forEach((k, e) -> {
//                events.remove(k);
//                removeEvent(calendar, e.id);
//            });
//            updateEvents(list);
//        }
        setExtendedProp(calendar, "objects", list);
    }

    protected native void removeEvent(JavaScriptObject calendar, int id)/*-{
        calendar.getEventById(id).remove();
    }-*/;

    protected native void updateCalendarProperty(JavaScriptObject calendar, String propertyName, Object property, int id)/*-{
        calendar.getEventById(id).setProp(propertyName, property);
    }-*/;

    protected native void updateStart(JavaScriptObject calendar, String start, int id)/*-{
        calendar.getEventById(id).setStart(start);
    }-*/;

    protected native void updateEnd(JavaScriptObject calendar, String end, int id)/*-{
        calendar.getEventById(id).setEnd(end);
    }-*/;

    protected native void setExtendedProp(JavaScriptObject calendar, String propertyName, Object property)/*-{
        calendar[propertyName] = property;
    }-*/;

    protected native void addEvent(JavaScriptObject calendar, String title, String start, String end, boolean editable, boolean durationEditable,
                                   boolean allDay, int id, String startFieldName,  String endFieldName, boolean isCurrentKey, String classNames)/*-{
        var event = {
            title: title,
            start: start,
            end: end,
            editable: editable,
            durationEditable: durationEditable,
            allDay: allDay,
            id: id,
            startFieldName: startFieldName,
            endFieldName: endFieldName,
            classNames: classNames
        };
        if (isCurrentKey)
            calendar.currentEventIndex = id;
        calendar.addEvent(event);
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
}
