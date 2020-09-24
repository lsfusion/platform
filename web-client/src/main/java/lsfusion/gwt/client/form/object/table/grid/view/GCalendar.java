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

    public GCalendar(GFormController form, GGridController grid, String calendarDateType) {
        super(form, grid);
        this.calendarDateType = calendarDateType;
    }

    @Override
    protected void render(Element element, Element recordElement, JsArray<JavaScriptObject> list) {
        if (calendar == null) {
            calendar = createCalendar(element, list, calendarDateType);
        }
        JavaScriptObject events = remapList(list, calendarDateType, getCaptions(new NativeHashMap<>(), true));
        updateEvents(calendar, events);
    }

    protected native JavaScriptObject createCalendar(Element element, JavaScriptObject objects, String calendarDateType)/*-{
        var thisObj = this;
        var controller = thisObj.@GCalendar::getController(*)();
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
            defaultTimedEventDuration: "00:01",
            allDayMaintainDuration: true,
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

    protected native JavaScriptObject remapList(JavaScriptObject objects, String calendarDateType, JsArray<JavaScriptObject> columns)/*-{
        var events = [];
        var thisObj = this;
        var controller = thisObj.@GCalendar::getController(*)();
        for (var i = 0; i < objects.length; i++) {
            var startEventFieldName = getStartEventFieldName(objects[i]);
            var endEventFieldName = getEndEventFieldName(objects[i]);
            var event = {
                'title': getTitle(objects[i]),
                'start': objects[i][startEventFieldName],
                'end': endEventFieldName != null ? objects[i][endEventFieldName] : '',
                'editable': !controller.isPropertyReadOnly(startEventFieldName, objects[i]),
                'allDay': endEventFieldName != null,
                'index': i,
                'startFieldName': getStartEventFieldName(objects[i]),
                'endFieldName': getEndEventFieldName(objects[i])
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

        function getStartEventFieldName(object) {
            if (object['dateFrom'] !== null && typeof object['dateFrom'] !== 'undefined') {
                return 'dateFrom';
            } else if (object['dateTimeFrom'] !== null && typeof object['dateTimeFrom'] !== 'undefined') {
                return 'dateTimeFrom';
            } else if (object['date'] !== null && typeof object['date'] !== 'undefined') {
                return 'date';
            } else if (object['dateTime'] !== null && typeof object['dateTime'] !== 'undefined') {
                return 'dateTime';
            }
            return null;
        }

        function getEndEventFieldName(object) {
            if (object['dateTo'] !== null && typeof object['dateTo'] !== 'undefined') {
                return 'dateTo';
            } else if (object['dateTimeTo'] !== null && typeof object['dateTimeTo'] !== 'undefined') {
                return 'dateTimeTo';
            } else {
                return null;
            }
        }
    }-*/;

    protected native void updateEvents(JavaScriptObject calendar, JavaScriptObject events)/*-{
        calendar.setOption('events', events);
    }-*/;
}
