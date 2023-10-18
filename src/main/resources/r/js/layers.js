var initLayers = function(map, olstuff, path) {
    var ls = path.substring(path.lastIndexOf(';')+1).split(',');
    map.getLayers().getArray().map(olstuff.actualLayers).reduce(function(a,b) {return a.concat(b)}).forEach(function(l) {
        if (ls.includes(l.get('shortName'))) {
            l.setVisible(true);
        }
    });
};
