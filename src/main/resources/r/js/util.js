var util = function(customPrettyPrinting, customInit) {
    var ret = {
        tooMassiveProperties: ['geometry', 'geometria', 'laskennallinenGeometria', 'labelPoint'],
        
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

        initPrettyPrinted: customInit,

        prettyPrint: function(obj) {
            var printer = customPrettyPrinting ? (x) => { var rr = customPrettyPrinting(x); return (rr === null || rr === undefined) ? ret.prettyPrint(x) : rr} : ret.prettyPrint;
            if (obj instanceof Array && obj.length > 0) {
                return '<span class="array">' + obj.map(function(val) {
                    var printedVal = (val && typeof val === 'object') ? printer(val) : val == null ? null : customPrettyPrinting ? customPrettyPrinting(val) : val;
                    return (printedVal ? printedVal : val) + ' ';
                }).join('') + '</span>';
            } else {
                var r = '';
                if (customPrettyPrinting) {
                    r = customPrettyPrinting(obj);
                }
                if (!r) {
                    r = '';
                    for (var key in obj) {
                        if (obj.hasOwnProperty(key) && !ret.tooMassiveProperties.includes(key)) {
                            if (r === '') {
                                r = '<ul>';
                            }
                            var val = obj[key];
                            if (val instanceof Array && val.length === 0) {
                                // skip empty arrays
                            } else {
                                var printedVal = (val && typeof val === 'object') ? printer(val) : val == null ? null : customPrettyPrinting ? customPrettyPrinting(val) : val;
                                r += '<li ' + (key.startsWith('_') ? 'hidden' : '') + '>' + '<span class="key">' + key + '</span><span class="value">' + (printedVal ? printedVal : val) + '</span></li>';
                            }
                        }
                    }
                    r = r === '' ? '' : r + '</ul>';
                }
                return r;
            }
        },

        now: function() {
            var d = new Date();
            d.setUTCHours(0);
            d.setUTCMinutes(0);
            d.setUTCSeconds(0);
            d.setUTCMilliseconds(0);
            return ret.toISOStringNoMillis(d);
        },
        
        limitInterval: function(intervalString) {
            var begin = new Date('2010-01-01T00:00:00Z');
            var end = new Date('2030-01-01T00:00:00Z');
            var instants = intervalString.split('/');
            if (new Date(instants[0]).getTime() < begin.getTime()) {
                instants[0] = ret.toISOStringNoMillis(begin);
            }
            if (new Date(instants[1]).getTime() > end.getTime()) {
                instants[1] = ret.toISOStringNoMillis(end);
            }
            return instants[0] + '/' + instants[1];
        },
        
        getSiblings: function (e) {
            let siblings = []; 
            if (!e.parentNode) {
                return siblings;
            }
            let sibling  = e.parentNode.firstChild;
            
            while (sibling) {
                if (sibling.nodeType === 1 && sibling !== e) {
                    siblings.push(sibling);
                }
                sibling = sibling.nextSibling;
            }
            return siblings;
        }
    };
    
    return ret;
};
