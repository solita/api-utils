var util = function() {
    var ret = {
        toISOStringNoMillis: function(d) {
            function pad(n) {
                return n < 10 ? '0' + n : n;
            }
            return d.getUTCFullYear() + '-' + pad(d.getUTCMonth() + 1) + '-' + pad(d.getUTCDate()) + 'T' + pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes()) + ':' + pad(d.getUTCSeconds()) + 'Z';
        },

        withoutProp: function(obj, unwantedProp) {
            var ret = {};
            for (var key in obj) {
                if (key !== unwantedProp) {
                    ret[key] = obj[key];
                }
            }
            return ret;
        },

        prettyPrint: function(obj) {
            if (obj instanceof Array && obj.length > 0) {
                return '<span class="array">' + obj.map(function(val) {
                    var printedVal = (val && typeof val === 'object') ? ret.prettyPrint(val) : val;
                    return printedVal;
                }).join('') + '</span>';
            } else {
                var r = '';
                for (var key in obj) {
                    if (obj.hasOwnProperty(key)) {
                        if (r === '') {
                            r = '<ul>';
                        }
                        var val = obj[key];
                        if (val instanceof Array && val.length === 0) {
                            // skip empty arrays
                        } else {
                            var printedVal = (val && typeof val === 'object') ? ret.prettyPrint(val) : val;
                            r += '<li>' + '<span class="key">' + key + '</span><span class="value">' + printedVal + '</span></li>';
                        }
                    }
                }
                return r === '' ? '' : r + '</ul>';
            }
        },

        now: function() {
            var d = new Date();
            d.setUTCHours(0);
            d.setUTCMinutes(0);
            d.setUTCSeconds(0);
            d.setUTCMilliseconds(0);
            return ret.toISOStringNoMillis(d);
        }
    };
    
    return ret;
};