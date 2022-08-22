function chart_line() {
    return chart('line', {});
}

function chart_bar() {
    return chart('bar', {});
}

function chart_pie() {
    return chart('pie', {});
}

function chart_radar() {
    return chart('radar', {});
}

function chart(type, options) {
    return {
        render: function (element) {
            element.style.setProperty("position", "relative");
            element.style.setProperty("height", "100%");
            element.style.setProperty("width", "100%");

            const canvas = document.createElement("canvas");
            element.appendChild(canvas);

            element.chart = new Chart(canvas.getContext('2d'), {
                                    type: type,
                                    options : { ...options, ...{ maintainAspectRatio: false } } });
        },
        update: function (element, controller, list) {
            let chart = element.chart;

            chart.data.labels.pop();
            chart.data.datasets.forEach((dataset) => {
                dataset.data.pop();
            });

            if (list.length > 0) {
                chart.data.labels = Array.from(list, object => object.label);

                let datasets = [];
                for (const [key, value] of Object.entries(list[0])) {
                    if (!(key === "#__key" || key === "label"))
                        datasets.push({ label: controller.getCaption(key),
                                        backgroundColor : Array.from(list, object => ('' + controller.getBackground(key, object) || '')),
                                        borderColor : Array.from(list, object => ('' + controller.getForeground(key, object) || '')),
                                        data: Array.from(list, object => object[key]) });
                }
                chart.data.datasets = datasets;
            }
            chart.update();
        }
    }
}
