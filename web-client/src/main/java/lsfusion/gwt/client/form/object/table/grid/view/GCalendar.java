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
            calendar = createCalendar(element, list, controller);
        }
        updateEvents(calendar, remapList(list, calendarDateType, getCaptions(new NativeHashMap<>(), gPropertyDraw -> !gPropertyDraw.baseType.isId()), controller));
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject objects, JavaScriptObject controller)/*-{
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: 'dayGridMonth',
            height: 'parent',
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,dayGridWeek,timeGridDay'
            },
            dayMaxEvents: 4,
            //to prevent the expand of a single event without "end"-param to the next day "nextDayThreshold" should be equal to "defaultTimedEventDuration", which by default is 01:00:00
            nextDayThreshold: '01:00:00',
            eventChange: function (info) {
                changeProperty(info, 'start');
                if (info.event.extendedProps.endFieldName != null) {
                    changeProperty(info, 'end');
                }
            }
        });
        calendar.render();
        return calendar;

        function changeProperty(info, position) {
            var propertyName = info.event.extendedProps[position + 'FieldName'];
            var controllerFunction = propertyName.includes('dateTime') ? 'changeDateTimeProperty' : 'changeDateProperty';
            var eventElement = info.event[position];
            controller[controllerFunction](propertyName, objects[info.event.extendedProps.index], eventElement.getFullYear(),
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
            for (var x in columns) {
                if (title !== '')
                    continue;
                title = x === 'name' ? object[x] : '';
            }
            if (title === '' && columns.length >= 2) {
                for (var k = 0; k <= 2; k++) {
                    title = title + ' ' + (object[columns[k]] != null ? object[columns[k]] : '');
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

    protected native void updateEvents(JavaScriptObject calendar, JavaScriptObject events)/*-{
        calendar.setOption('events', events);
    }-*/;
}
