var time = function(timeInput, timeInputForce) {
    timeInput.placeholder = new Date().toISOString().slice(0,11) + '00:00:00Z';
    var params = new URLSearchParams(window.location.search);
    if (params.get('time')) {
        timeInput.value = params.get('time');
        var start = timeInput.value.indexOf('/') < 0 ? new Date(timeInput.value) : new Date(timeInput.value.split('/')[0]);
        var end = timeInput.value.indexOf('/') < 0 ? new Date() : new Date(timeInput.value.split('/')[1]);
        timeInput.setAttribute('data-duration', durationFns.durationFns.toString(durationFns.durationFns.between(start, end)));
    }
    if (params.get('time-infra')) {
        timeInputForce.checked = true;
    }
    timeInput.onchange = function() {
        if (timeInput.validity.valid) {
            let params = new URLSearchParams(window.location.search);
            if (timeInput.value) {
                params.set('time', timeInput.value);
                var start = timeInput.value.indexOf('/') < 0 ? new Date(timeInput.value) : new Date(timeInput.value.split('/')[0]);
                var end = timeInput.value.indexOf('/') < 0 ? new Date() : new Date(timeInput.value.split('/')[1]);
                timeInput.setAttribute('data-duration', durationFns.durationFns.toString(durationFns.durationFns.between(start, end)));
            } else {
                params.delete('time');
                timeInput.removeAttribute('data-duration');
            }
            window.location.search = decodeURIComponent(params);
        }
    }
    timeInputForce.onchange = function() {
        let params = new URLSearchParams(window.location.search);
        if (timeInputForce.checked && timeInput.value && timeInput.validity.valid) {
            params.set('time-infra', timeInput.value + (timeInput.value.indexOf('/') < 0 ? "/" + timeInput.value : ''));
        } else {
            params.delete('time-infra');
        }
        window.location.search = decodeURIComponent(params);
    }
};
