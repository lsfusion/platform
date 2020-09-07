function calendar(element, listJsObject, customChangeProperty) {
    setTimeout(function () {
        element.style.position = 'absolute';
        element.style.top = '0px';
        element.style.left = '0px';
        element.style.bottom = '0px';
        element.style.right = '0px';

        var calendar = new FullCalendar.Calendar(element, {
            timeZone: 'UTC',
            firstDay: 1,
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
            editable: true,
            dayMaxEvents: true,
            events: listJsObject,
            eventChange: function(info) {
                let object = null;
                for (let o in listJsObject) {
                    if (listJsObject[o]["#__key"] === info.event.extendedProps["#__key"]) {
                        object = listJsObject[o];
                    }
                }
                //getMonth()+1 because "getMonth()" is zero based
                customChangeProperty.changeDateTimeProperty('start', object, info.event.start.getFullYear(), info.event.start.getMonth() + 1, info.event.start.getUTCDate(), info.event.start.getUTCHours(), info.event.start.getUTCMinutes(), info.event.start.getUTCSeconds());
            }
        });
        calendar.render();
    }, 0);
}