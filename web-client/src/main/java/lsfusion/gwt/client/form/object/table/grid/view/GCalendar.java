package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

import java.util.Objects;

public class GCalendar extends GSimpleStateTableView {

    private final String calendarDateType;
    private JavaScriptObject calendar;
    private final JavaScriptObject controller;

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
        this.controller = getController();
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        if (calendar == null) {
            //fullcalendar bug - https://github.com/fullcalendar/fullcalendar/issues/5863
            //to prevent this when calendar-element height less then ~350px
            element.getParentElement().getStyle().setProperty("overflow", "auto");
            element.getStyle().setProperty("minHeight", "400px");
            element.getStyle().setProperty("cursor", "default");

            calendar = createCalendar(element, controller, calendarDateType);
        }
        setRecordElement(calendar, recordElement);
        updateEvents(calendar, list, calendarDateType, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()), controller);
    }

    protected native void setRecordElement(JavaScriptObject calendar, Element recordElement)/*-{
        calendar.recordElement = recordElement;
    }-*/;

    @Override
    public void onResize() {
        if (calendar != null)
            resize(calendar);
    }

    protected native void resize(JavaScriptObject calendar)/*-{
        calendar.updateSize();
    }-*/;

    protected boolean isCurrentKey(JavaScriptObject object){
        return Objects.equals(getKey(object), getCurrentKey());
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject controller, String calendarDateType)/*-{
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth',
            height: 'parent',
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: calendarDateType === 'dateTime' ? 'dayGridMonth,dayGridWeek,timeGridDay' : 'dayGridMonth,dayGridWeek'
            },
            dayMaxEvents: true,
            //to prevent the expand of a single event without "end"-param to the next day "nextDayThreshold" should be equal to "defaultTimedEventDuration", which by default is 01:00:00
            nextDayThreshold: '01:00:00',
            eventOrder: 'start,index',
            eventChange: function (info) {
                changeProperty(info, 'start', this.objects);
                changeProperty(info, 'end', this.objects);
            },
            datesSet: function (dateInfo) {
                var oldEvent = calendar.getEvents()[calendar.currentEventIndex];
                if (oldEvent != null && (oldEvent.start < dateInfo.start || oldEvent.start > dateInfo.end)) {
                    var event = getEvent(dateInfo, oldEvent.start <= dateInfo.start);
                    if (event !== null)
                        changeCurrentEvent(event);
                }
            },
            eventClick: function (info) {
                changeCurrentEvent(info.event);
                setTooltip(info.el);
            }
        });
        calendar.render();
        return calendar;

        function setTooltip(el) {
            if (calendar.recordElement != null) {
                $wnd.tippy(el, {
                    content: calendar.recordElement,
                    showOnCreate: true,
                    trigger: 'click',
                    interactive: true,
                    allowHTML: true
                });
            }
        }

        function getEvent(dateInfo, getFirst) {
            var resultEvent = null;
            var events = calendar.getEvents();
            for (var i = 0; i < events.length; i++) {
                var event = events[i];
                if ((event.start >= dateInfo.start && event.start <= dateInfo.end) && (resultEvent == null || ((getFirst ? resultEvent.start > event.start : resultEvent.start < event.start))))
                    resultEvent = event;
            }
            return resultEvent;
        }

        function changeCurrentEvent(currentEvent) {
            var oldEvent = calendar.currentEventIndex != null ? calendar.getEvents()[calendar.currentEventIndex] : null;
            controller.changeSimpleGroupObject(calendar.objects[currentEvent.extendedProps.index], false);
            if (oldEvent !== null)
                oldEvent.setProp('classNames', '');
            currentEvent.setProp('classNames', 'event-highlight');
            calendar.currentEventIndex = currentEvent.extendedProps.index;
        }

        function changeProperty(info, position, objects) {
            var currentEvent = info.event;
            var oldEvent = info.oldEvent;
            if (currentEvent[position] !== null && currentEvent[position].getTime() !== oldEvent[position].getTime()) {
                var propertyName = currentEvent.extendedProps[position + 'FieldName'];
                var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
                var eventElement = currentEvent[position];
                controller[controllerFunction](propertyName, objects[currentEvent.extendedProps.index], eventElement.getFullYear(),
                    eventElement.getMonth() + 1, eventElement.getUTCDate(), eventElement.getUTCHours(),
                    eventElement.getUTCMinutes(), eventElement.getUTCSeconds());
            }
        }
    }-*/;

    protected native JavaScriptObject updateEvents(JavaScriptObject calendar, JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns, JavaScriptObject controller)/*-{
        var events = [];
        for (var i = 0; i < objects.length; i++) {
            var startEventFieldName = getEventName(objects[i], '') != null ? getEventName(objects[i], '') : getEventName(objects[i], 'From');
            var endEventFieldName = getEventName(objects[i], 'To');
            var isCurrentKey = this.@GCalendar::isCurrentKey(*)(objects[i]);
            var event = {
                'title': getTitle(objects[i]),
                'start': objects[i][startEventFieldName],
                'end': endEventFieldName != null ? objects[i][endEventFieldName] : null,
                'editable': !controller.isPropertyReadOnly(startEventFieldName, objects[i]) && (endEventFieldName == null || !controller.isPropertyReadOnly(endEventFieldName, objects[i])),
                'durationEditable': endEventFieldName !== null && !controller.isPropertyReadOnly(endEventFieldName, objects[i]),
                'allDay': calendarDateType === 'date',
                'index': i,
                'startFieldName': startEventFieldName,
                'endFieldName': endEventFieldName,
                'classNames': isCurrentKey ? 'event-highlight' : ''
            };
            events.push(event);
            if (isCurrentKey)
                calendar.currentEventIndex = i;
        }

        calendar.objects = objects;
        calendar.setOption('events', events);

        function getTitle(object) {
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
        }

        function getEventName(object, position) {
            if (object[calendarDateType + position] !== null && typeof object[calendarDateType + position] !== 'undefined') {
                return calendarDateType + position;
            } else {
                return null;
            }
        }
    }-*/;
}
