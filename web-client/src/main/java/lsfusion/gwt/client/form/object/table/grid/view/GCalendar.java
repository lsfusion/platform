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
            calendar = createCalendar(element, list, calendarDateType, controller);
        }
        updateEvents(calendar, remapList(list, calendarDateType, getCaptions(new NativeHashMap<>(), gPropertyDraw -> !gPropertyDraw.baseType.isId()), controller));
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject objects, String calendarDateType, JavaScriptObject controller)/*-{
        var calendar = new $wnd.FullCalendar.Calendar(element, {
            initialView: calendarDateType === 'date' ? 'dayGridMonth' : 'dayGridWeek',
            height: 'parent',
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,dayGridWeek,timeGridDay'
            },
            dayMaxEvents: 4,
            nextDayThreshold: '01:00:00',
            eventChange: function (info) {
                controller.changeDateTimeProperty(info.event.extendedProps.startFieldName, objects[info.event.extendedProps.index], info.event.start.getFullYear(),
                    info.event.start.getMonth() + 1, info.event.start.getUTCDate(), info.event.start.getUTCHours(),
                    info.event.start.getUTCMinutes(), info.event.start.getUTCSeconds());
                if (info.event.extendedProps.endFieldName != null) {
                    controller.changeDateTimeProperty(info.event.extendedProps.endFieldName, objects[info.event.extendedProps.index], info.event.end.getFullYear(),
                        info.event.end.getMonth() + 1, info.event.end.getUTCDate(), info.event.end.getUTCHours(),
                        info.event.end.getUTCMinutes(), info.event.end.getUTCSeconds());
                }
            }
        });
        calendar.render();
        return calendar;
    }-*/;

    protected native JavaScriptObject remapList(JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns, JavaScriptObject controller)/*-{
        var events = [];
        for (var i = 0; i < objects.length; i++) {
            var startEventFieldName = getEventFieldName(objects[i], 'start');
            var endEventFieldName = getEventFieldName(objects[i], 'end');
            var event = {
                'title': getTitle(objects[i]),
                'start': objects[i][startEventFieldName],
                'end': endEventFieldName != null ? objects[i][endEventFieldName] : '',
                'editable': !controller.isPropertyReadOnly(startEventFieldName, objects[i]),
                'durationEditable': endEventFieldName !== null,
                'allDay': startEventFieldName === 'date' || startEventFieldName === 'dateFrom',
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

        function getEventFieldName(object, type) {
            if (type === 'start') {
                if (object[calendarDateType] != null && typeof object[calendarDateType] != 'undefined') {
                    return calendarDateType;
                } else if (object[calendarDateType + 'From'] !== null && typeof object[calendarDateType + 'From'] !== 'undefined') {
                    return calendarDateType + 'From';
                }
            } else if (type === 'end') {
                if (object[calendarDateType + 'To'] !== null && typeof object[calendarDateType + 'To'] !== 'undefined') {
                    return calendarDateType + 'To';
                }  else {
                    return null;
                }
            }
        }
    }-*/;

    protected native void updateEvents(JavaScriptObject calendar, JavaScriptObject events)/*-{
        calendar.setOption('events', events);
    }-*/;
}
