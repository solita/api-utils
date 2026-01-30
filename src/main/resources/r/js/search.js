var search = function(map, searchUrlFunction, olstuff, select) {
    var searchStyles = [[0,255,0,0.4], [0,0,255,0.4], [255,0,0,0.4], [255,255,0,0.4], [0,255,255,0.4], [255,0,255,0.4]].map(function(c) {
        var stroke = [...c];
        stroke[3] = 1;
        return olstuff.styles.defaultWithColor(c,stroke);
    });

    return function(hash) {
        hash.split('#')
            .map(function(x) { return x.trim(); })
            .filter(function(x) { return x != ''; })
            .map(function(h) { return decodeURIComponent(h); })
            .forEach(function(h,index) {
                let s = searchUrlFunction(h);
                if (s && map.getLayers().getArray().filter(function(x) {return x.getProperties().title == olstuff.mkLayerTitle(h,h);}).length == 0) {
                    let layer = s instanceof Array ?
                         new ol.layer.Vector({source: new ol.source.Vector({features: [new ol.format.WKT().readFeature(s[0])] })}) :
                         olstuff.newVectorLayerNoTile(s, h, h, h, undefined, undefined, searchStyles[index % searchStyles.length]);
                    layer.setVisible(true);
                    map.addLayer(layer);
                    layer.once('change', function(e) {
                        if (layer.getSource().getFeatures().length == 1) {
                            layer.getSource().getFeatures().forEach(f => select(f, true));
                            var extent = layer.getSource().getExtent();
                            if (!ol.extent.isEmpty(extent)) {
                                map.getView().fit(extent, {'maxZoom': 10, 'padding': [50, 50, 50, 50], 'duration': 1000});
                            }
                        }
                    });
                }
            });
    }
};
