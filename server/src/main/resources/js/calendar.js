function calendar(element, listJsObject) {

    var script= document.createElement('script');
    script.src= 'https://cdn.jsdelivr.net/npm/fullcalendar@5.3.0/main.js';

    var css = document.createElement('link');
    css.href = 'https://cdn.jsdelivr.net/npm/fullcalendar@5.3.0/main.css';
    css.rel = 'stylesheet';


    script.onload = function() {
        setTimeout(function() {
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
    };

    document.head.appendChild(script);
    document.head.appendChild(css);
}