function calendar(element, listJsObject) {
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
            events: listJsObject
        });
        calendar.render();
    }, 0);
}