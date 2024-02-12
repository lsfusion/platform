function carousel() {
    return {
        render: function (element) {
            let carousel = document.createElement("div");
            carousel.classList.add("carousel");

            let carouselInner = document.createElement("div");
            carouselInner.classList.add("carousel-inner");
            carousel.appendChild(carouselInner);

            let prevButton = document.createElement("button");
            prevButton.classList.add("carousel-control-prev");
            prevButton.type = "button";
            prevButton.setAttribute("data-bs-slide","prev");
            carousel.appendChild(prevButton);

            let prevSpan = document.createElement("span");
            prevSpan.classList.add("carousel-control-prev-icon");
            prevSpan.setAttribute("aria-hidden", "true");
            prevButton.appendChild(prevSpan);

            let nextButton = document.createElement("button");
            nextButton.classList.add("carousel-control-next");
            nextButton.type = "button";
            nextButton.setAttribute("data-bs-slide","next");
            carousel.appendChild(nextButton);

            let nextSpan = document.createElement("span");
            nextSpan.classList.add("carousel-control-next-icon");
            nextSpan.setAttribute("aria-hidden", "true");
            nextButton.appendChild(nextSpan);

            element.carousel = carousel;
            element.carouselInner = carouselInner;
            element.prevButton = prevButton;
            element.nextButton = nextButton;

            element.style.setProperty("min-height", "0px");
            element.style.setProperty("flex-shrink", "1");

            element.appendChild(carousel);
        },
        update: function (element, controller, list, options) {
            while (element.carouselInner.lastElementChild) {
                element.carouselInner.removeChild(element.carouselInner.lastElementChild);
            }

            let carouselId = options.id ? options.id : "carousel";
            element.carousel.id = carouselId;
            element.prevButton.setAttribute("data-bs-target","#" + carouselId);
            element.nextButton.setAttribute("data-bs-target","#" + carouselId);

            if (options.theme)
                element.carousel.setAttribute("data-bs-theme", options.theme);

            if (options.fitContain)
                element.carousel.classList.add("carousel-fit-contain");

            for (let object of list) {
                let carouselItem = document.createElement("div");
                carouselItem.classList.add("carousel-item");
                if (controller.isCurrent(object))
                    carouselItem.classList.add("active");
                carouselItem.object = object;

                let carouselObject = document.createElement( object.tag && object.tag !== "" ? object.tag : "img");
                carouselObject.classList.add("d-block");
                carouselObject.src = object.file;

                if (object.type)
                    carouselObject.type = object.type;

                carouselObject.style.cursor = "zoom-in";
                carouselObject.addEventListener("click", function() {
                    carouselObject.requestFullscreen();
                })

                carouselItem.appendChild(carouselObject);

                if (object.caption) {
                    let carouselCaption = document.createElement("div");
                    carouselCaption.classList.add("carousel-caption");
                    carouselCaption.classList.add("d-none");
                    carouselCaption.classList.add("d-md-block");
                    carouselCaption.innerHTML = object.caption;

                    carouselItem.appendChild(carouselCaption);
                }

                element.carouselInner.appendChild(carouselItem);
            }

            element.carousel.addEventListener('slide.bs.carousel', event => {
                if (event.relatedTarget) {
                    let object = event.relatedTarget.object;
                    if (!controller.isCurrent(object)) controller.changeObject(object);
                }
            })
        }
    }
}
