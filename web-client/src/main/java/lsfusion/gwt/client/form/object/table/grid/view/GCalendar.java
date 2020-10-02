package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

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

            calendar = createCalendar(element, controller, calendarDateType);
        }
        updateEvents(calendar, list, remapList(list, calendarDateType, getCaptions(new NativeHashMap<>(), gPropertyDraw -> gPropertyDraw.baseType.isId()), controller));
    }

    @Override
    public void onResize() {
        if (calendar != null)
            resize(calendar);
    }

    protected native void resize(JavaScriptObject calendar)/*-{
        calendar.updateSize();
    }-*/;

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
                changeProperty(info, 'start', this.nonRemappedEvents);
                if (info.event.extendedProps.endFieldName != null) {
                    changeProperty(info, 'end');
                }
            }
        });
        calendar.render();
        return calendar;

        function changeProperty(info, position, nonRemappedEvents) {
            var propertyName = info.event.extendedProps[position + 'FieldName'];
            var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
            var eventElement = info.event[position];
            controller[controllerFunction](propertyName, nonRemappedEvents[info.event.extendedProps.index], eventElement.getFullYear(),
                eventElement.getMonth() + 1, eventElement.getUTCDate(), eventElement.getUTCHours(),
                eventElement.getUTCMinutes(), eventElement.getUTCSeconds());
        }
    }-*/;

    protected native JavaScriptObject remapList(JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns, JavaScriptObject controller)/*-{
        var events = [];
        for (var i = 0; i < objects.length; i++) {
            var startEventFieldName = getEventName(objects[i], '') != null ? getEventName(objects[i], '') : getEventName(objects[i], 'From');
            var endEventFieldName = getEventName(objects[i], 'To');
            var event = {
                'title': getTitle(objects[i]),
                'start': objects[i][startEventFieldName],
                'end': endEventFieldName != null ? objects[i][endEventFieldName] : '',
                'editable': !controller.isPropertyReadOnly(startEventFieldName, objects[i]) && (endEventFieldName == null || !controller.isPropertyReadOnly(endEventFieldName, objects[i])),
                'durationEditable': endEventFieldName !== null && !controller.isPropertyReadOnly(endEventFieldName, objects[i]),
                'allDay': calendarDateType === 'date',
                'index': i,
                'startFieldName': startEventFieldName,
                'endFieldName': endEventFieldName
            };
            events.push(event);
        }
        return events;

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
                    if (value != null) {
                        if (title !== ''){
                            title = title + ' - ' + value;
                        } else {
                          title = value;
                        }
                    }
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

    protected native void updateEvents(JavaScriptObject calendar, JavaScriptObject nonRemappedEvents, JavaScriptObject events)/*-{
        calendar.nonRemappedEvents = nonRemappedEvents;
        calendar.setOption('events', events);
    }-*/;
}
