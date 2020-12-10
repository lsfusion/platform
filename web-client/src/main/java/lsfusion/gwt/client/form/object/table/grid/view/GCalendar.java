package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

import java.util.ArrayList;
import java.util.List;

public class GCalendar extends GTippySimpleStateTableView {

    private final String calendarDateType;
    private JavaScriptObject calendar;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
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
            eventOrder: 'start,id',
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
            var newEventId= newEvent.id;
            var newObject = calendar.objects.get(newEventId);

            controller.changeSimpleGroupObject(newObject, true, elementClicked); // we're rerendering current event below

            controller.highlightEvent(calendar, newEventId);
        }

        function changeProperty(info, position, objects) {
            var currentEvent = info.event;
            var oldEvent = info.oldEvent;
            if (currentEvent[position] !== null && oldEvent[position] !== null && currentEvent[position].getTime() !== oldEvent[position].getTime()) {
                var propertyName = currentEvent.extendedProps[position + 'FieldName'];
                var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
                var eventElement = parseCalendarDateElement(currentEvent[position]);
                controller[controllerFunction](propertyName, objects.get(currentEvent.id), eventElement.year,
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

    private final NativeHashMap<GGroupObjectValue, Event> events = new NativeHashMap<>();
    private class Event {

        public String title;
        public String start;
        public String end;
        public boolean editable;
        public boolean durationEditable;
        public final boolean allDay;
        public final GGroupObjectValue id;
        public final String startFieldName;
        public final String endFieldName;

        public Event(JavaScriptObject object) {
            String endEventFieldName = calendarDateType.contains("From") ? calendarDateType.replace("From", "To") : null;

            title = getTitle(object, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()));
            start = getStart(object, calendarDateType);
            end = endEventFieldName != null ? getEnd(object, endEventFieldName): null;
            editable = isEditable(object, controller, calendarDateType, endEventFieldName);
            durationEditable = isDurationEditable(object, controller, endEventFieldName);
            allDay = calendarDateType.equals("date") || calendarDateType.equals("dateFrom");
            id = getKey(object);
            startFieldName = calendarDateType;
            endFieldName = endEventFieldName;
        }
    }

    private void updateEvents(JsArray<JavaScriptObject> list) {
        NativeHashMap<GGroupObjectValue, Event> oldEvents = new NativeHashMap<>();
        oldEvents.putAll(events);
        boolean eventsAdded = false;
        List<Event> eventsToAdd = new ArrayList<>();
        int updateCol = 0;
        boolean needFullUpdate = false;
        for (int i = 0; i < list.length(); i++) {
            JavaScriptObject object = list.get(i);
            GGroupObjectValue key = getKey(object);

            Event event = new Event(object);
            Event oldEvent = oldEvents.remove(key);
            if (oldEvent == null) {
                events.put(key, event);
                eventsToAdd.add(event);
                eventsAdded = true;
            } else {

                if (updateCol > 10)
                    needFullUpdate = true;

                if (!oldEvent.title.equals(event.title)) {
                    if (!needFullUpdate)
                        updateCalendarProperty(calendar, "title", event.title, oldEvent.id);
                    events.get(oldEvent.id).title = event.title;
                    updateCol ++;
                }

                if (!oldEvent.start.equals(event.start)) {
                    if (!needFullUpdate)
                        updateStart(calendar, event.start, oldEvent.id);
                    events.get(oldEvent.id).start = event.start;
                    updateCol ++;
                }

                if (oldEvent.end != null && !oldEvent.end.equals(event.end)) {
                    if (!needFullUpdate)
                        updateEnd(calendar, event.end, oldEvent.id);
                    events.get(oldEvent.id).end = event.end;
                    updateCol ++;
                }

                if (oldEvent.editable != event.editable) {
                    if (!needFullUpdate)
                        updateCalendarProperty(calendar, "editable", event.editable, oldEvent.id);
                    events.get(oldEvent.id).editable = event.editable;
                    updateCol ++;
                }

                if (oldEvent.durationEditable != event.durationEditable) {
                    if (!needFullUpdate)
                        updateCalendarProperty(calendar, "durationEditable", event.durationEditable, oldEvent.id);
                    events.get(oldEvent.id).durationEditable = event.durationEditable;
                    updateCol ++;
                }
            }
        }

        //удалять пакетом события из календаря не выйдет. либо все сразу либо по одному
        // поэтому при удалении малого количества событий будет работать этот if,
        // а при удалении большого объема (например при перелистывании страницы) быстрее добавить заново все события
        oldEvents.foreachEntry((key, event) -> {
            if (oldEvents.size() <= 2 )
                removeSingleCalendarEvent(calendar, key);
            events.remove(key);
        });

        //добавление по одному элементу
        if (!eventsToAdd.isEmpty() && eventsToAdd.size() <= 2)
            eventsToAdd.forEach(e -> addSingleCalendarEvent(calendar, getJsEvent(e)));
        //обновление всех событий календаря(при добавлении/обновлении большого объема событий, при перелистываниии страниц, при нажатии "отмена",
        // при включении корректировки на форме)
        else if ((eventsAdded && !events.isEmpty() && eventsToAdd.size() > 2) || oldEvents.size() > 2 || list.length() == 0 || needFullUpdate)
            setCalendarEvents(calendar, createCalendarEventsObject(events));

        setExtendedProp(calendar, "objects", list);
        highlightEvent(calendar, getCurrentKey());
    }

    private JsArray<JavaScriptObject> createCalendarEventsObject(NativeHashMap<GGroupObjectValue, Event> events){
        JsArray<JavaScriptObject> calendarEvents = JavaScriptObject.createArray().cast();
        events.foreachValue(event -> calendarEvents.push(getJsEvent(event)));
        return calendarEvents;
    }

    protected native void removeSingleCalendarEvent(JavaScriptObject calendar, GGroupObjectValue eventId)/*-{
        var event = calendar.getEventById(eventId);
        if (event != null)
            event.remove();
    }-*/;

    protected native void addSingleCalendarEvent(JavaScriptObject calendar, JavaScriptObject event)/*-{
        calendar.addEvent(event, true);
    }-*/;

    protected native void highlightEvent(JavaScriptObject calendar, GGroupObjectValue id)/*-{
        var oldEvent = calendar.currentEventId != null ? calendar.getEventById(calendar.currentEventId) : null;
        if (oldEvent != null)
            oldEvent.setProp('classNames', '');
        var newEvent = calendar.getEventById(id);
        if (newEvent != null)
            newEvent.setProp('classNames', 'event-highlight');
        calendar.currentEventId = id;
    }-*/;

    protected native void setCalendarEvents(JavaScriptObject calendar, JsArray<JavaScriptObject> events)/*-{
        calendar.setOption('events', events);
    }-*/;

    protected native void updateCalendarProperty(JavaScriptObject calendar, String propertyName, Object property, GGroupObjectValue id)/*-{
        calendar.getEventById(id).setProp(propertyName, property);
    }-*/;

    protected native void updateStart(JavaScriptObject calendar, String start, GGroupObjectValue id)/*-{
        calendar.getEventById(id).setStart(start);
    }-*/;

    protected native void updateEnd(JavaScriptObject calendar, String end, GGroupObjectValue id)/*-{
        calendar.getEventById(id).setEnd(end);
    }-*/;

    protected native void setExtendedProp(JavaScriptObject calendar, String propertyName, JsArray<JavaScriptObject> list)/*-{
        var map = new Map();
        for (var i = 0; i < list.length; i++) {
            map.set(this.@GSimpleStateTableView::getKey(*)(list[i]).toString(), list[i]);
        }
        calendar.objects = map;
    }-*/;

    private JavaScriptObject getJsEvent(Event event){
        return getEventAsJs(event.title, event.start, event.end, event.editable, event.durationEditable,
                event.allDay, event.id, event.startFieldName, event.endFieldName);
    }

    protected native JavaScriptObject getEventAsJs(String title, String start, String end, boolean editable, boolean durationEditable,
                                                 boolean allDay, GGroupObjectValue id, String startFieldName, String endFieldName)/*-{
        return {
            title: title,
            start: start,
            end: end,
            editable: editable,
            durationEditable: durationEditable,
            allDay: allDay,
            id: id,
            startFieldName: startFieldName,
            endFieldName: endFieldName
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
}
