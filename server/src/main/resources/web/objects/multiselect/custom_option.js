function option() {
    return {
        render: function (element, controller) {
            let options = document.createElement('options');
            options.style.display = 'block';
            options.style.background = 'var(--component-background-color)';
            options.style.borderRadius = 'var(--table-border-radius)';
            options.style.border = '1px solid var(--grid-separator-border-color)';
            options.style.padding = 'var(--border-padding) 4px';

            element.appendChild(options);
            element.options = options;
        },
        update: function (element, controller, list) {
            let options = element.options;
            let currentOptions = Array.from(options.children);

            let diff = controller.getDiff(list);

            for (let option of diff.add) {
                add(option);
            }

            for (let option of diff.update) {
                update(option);
            }

            for (let option of diff.remove) {
                remove(option);
            }

            //set Current
            Array.from(options.children).forEach(o =>
                o.style.border = 'var(--border-width) solid var(' + (controller.isCurrent(o.key) ? '--focused-cell-border-color)' : '--grid-separator-border-color)'));

            function remove(rawOption) {
                currentOptions.forEach(o => {
                    if (controller.getKey(o.key).toString() === controller.getKey(rawOption).toString())
                        options.removeChild(o);
                })
            }

            function update(rawOption) {
                currentOptions.forEach(o => {
                    if (controller.getKey(o.key).toString() === controller.getKey(rawOption).toString()) {
                        o.innerText = rawOption.name;
                        o.selected = rawOption.selected;
                        o.style.backgroundColor = rawOption.selected ? 'var(--selection-color)' : 'unset';
                    }
                });
            }

            function add(rawOption) {
                let option = document.createElement('div');
                option.innerText = rawOption.name;
                option.key = rawOption;
                option.selected = rawOption.selected;

                option.style.backgroundColor = rawOption.selected ? 'var(--selection-color)' : 'unset';
                option.style.display = 'inline-block';
                option.style.padding = '5px';
                option.style.border = 'var(--border-width) solid var(--grid-separator-border-color)';
                option.style.borderRadius = 'var(--table-border-radius)';
                option.style.margin = '3px 3px 3px 0';

                option.addEventListener('mouseover', function () {
                    this.style.backgroundColor = 'var(--button-hover-background-color)';
                    this.style.cursor = 'pointer';
                });

                option.addEventListener('mouseout', function () {
                    this.style.backgroundColor = this.selected ? 'var(--selection-color)' : 'unset';
                });

                option.addEventListener('click', function () {
                    this.selected = !this.selected;
                    controller.changeProperty('selected', this.key, this.selected === false ? null : this.selected);
                    controller.changeSimpleGroupObject(this.key, false, null);
                });

                options.appendChild(option);
            }
        }
    }
}