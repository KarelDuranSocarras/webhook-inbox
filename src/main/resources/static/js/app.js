document.addEventListener("click", function (event) {
    var trigger = event.target.closest("[data-copy]");
    if (!trigger) {
        return;
    }
    var target = document.querySelector(trigger.getAttribute("data-copy"));
    if (!target) {
        return;
    }
    var value = "value" in target ? target.value : target.textContent;
    var done = function () {
        var original = trigger.textContent;
        trigger.textContent = "Copied!";
        setTimeout(function () {
            trigger.textContent = original;
        }, 1200);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(value).then(done, function () {
            fallbackCopy(value, done);
        });
    } else {
        fallbackCopy(value, done);
    }
});

function fallbackCopy(value, done) {
    var area = document.createElement("textarea");
    area.value = value;
    area.setAttribute("readonly", "");
    area.style.position = "absolute";
    area.style.left = "-9999px";
    document.body.appendChild(area);
    area.select();
    try {
        document.execCommand("copy");
        done();
    } finally {
        document.body.removeChild(area);
    }
}
