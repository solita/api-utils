var olstuff = function(constants, util) {
    proj4.defs("EPSG:3067", "+proj=utm +zone=35 +ellps=GRS80 +units=m +no_defs");
    
    var instant = new URLSearchParams(window.location.search).get('time') || util.now();
    
    var ret = {
        instant: instant,
        
        projection: new ol.proj.Projection({
            code: 'EPSG:3067',
            extent: constants.world
        }),
        
        geojsonProjection: new ol.proj.Projection({
            code: 'EPSG:4326'
        }),
        
        tileGrid: new ol.tilegrid.TileGrid({
            resolutions: constants.resolutions,
            tileSizes: constants.tileSizes,
            extent: constants.dataExtent
        }),
        
        tileGridAll: new ol.tilegrid.TileGrid({
            resolutions: constants.resolutions,
            extent: constants.dataExtent
        }),
        
        format: new ol.format.GeoJSON(),
        
        overlay: function (element) {
            return new ol.Overlay(({
                element: element,
                offset: [50,-50]
            }));
        },
        
        toWKT: function(feature) {
            return new ol.format.WKT().writeFeature(feature).replace(/[.]\d+/g, '');
        },
        
        createPolygonInteraction: function(map, callback) {
            var vector = new ol.layer.Vector({
                source: new ol.source.Vector(),
                style: new ol.style.Style({
                    fill: new ol.style.Fill({
                      color: 'rgba(255, 255, 255, 0.2)'
                    }),
                    stroke: new ol.style.Stroke({
                      color: '#ffcc33',
                      width: 2
                    }),
                    image: new ol.style.Circle({
                      radius: 7,
                      fill: new ol.style.Fill({
                        color: '#ffcc33'
                      })
                    })
                })
            });
            map.addLayer(vector);
            var draw = new ol.interaction.Draw({
                source: new ol.source.Vector({wrapX: false}),
                type: 'Polygon'
            });
            draw.on('drawend', function(e) {
                return callback(e, map.getCoordinateFromPixel([50,100]));
            });
            return draw;
        },
        
        createContextMenu: function(map, callback) {
            map.getViewport().addEventListener('contextmenu', function (evt) {
                evt.preventDefault();
                var coords = map.getEventCoordinate(evt);
                callback(evt, coords);
            });
        },
        
        actualLayers: function(mapOrGroup) {
            var results = [];
            if (mapOrGroup.getLayers) {
                mapOrGroup.getLayers().forEach(function(groupOrLayer) {
                    if (groupOrLayer.getLayers) {
                        results = results.concat(ret.actualLayers(groupOrLayer));
                    } else {
                        results.push(groupOrLayer);
                    }
                });
            }
            return results;
        },
        
        actualVisibleLayers: function(map) {
            return ret.actualLayers(map).filter(function(l) {
                return l.getVisible();
            });
        },
        
        featuresOnScreen: function(map) {
            var mapExtent = map.getView().calculateExtent(map.getSize());
            var results = {};
            var onlyUnique = function(value, index, self) { 
                return !value.tunniste || self.findIndex(function(v) { return v.tunniste == value.tunniste; }) === index;
            };
            ret.actualVisibleLayers(map).forEach(function(layer) {
                if (layer.getSource && layer.getSource().getFeaturesInExtent) {
                    var props = layer.getProperties();
                    results[props.title] = layer.getSource().getFeaturesInExtent(mapExtent).map(function(x) {
                        return util.withoutProp(x.getProperties(), 'geometry');
                    }).filter(onlyUnique);
                }
            });
            return results;
        },
        
        getFeatureByTunniste: function(map, tunniste) {
            var layers = ret.actualVisibleLayers(map);
            for (var i = 0; i < layers.length; i++) {
                var layer = layers[i];
                if (layer.getSource && layer.getSource().getFeatures) {
                    var features = layer.getSource().getFeatures(tunniste);
                    for (var j = 0; j < features.length; ++j) {
                        if (features[j].getProperties().tunniste == tunniste) {
                            return features[j];
                        }
                    }
                }
            }
            return null;
        },
        
        registerListView: function(map, elem, selectInteraction, select, unselect, click) {
            if (!map.mystate) {
                map.mystate = {};
                map.mystate.closed = {};
            }
            document.onkeydown = function() {
                if (!document.querySelector('input:not(#rajoita):focus')) {
                    document.getElementById('rajoita').focus();
                }
            };
            var f = function(evt) {
                elem.innerHTML = '<input id="rajoita" autofocus type="text" placeholder="rajoita/restrict..." /><br />' + util.prettyPrint(ret.featuresOnScreen(map));
                var input = elem.querySelector('input');
                input.onkeyup = function() {
                    [...elem.querySelectorAll('ul > li > span > span > ul')].filter(function(x) { return x.textContent.indexOf(input.value) >= 0; }).forEach(function(x) { x.style.display = 'block'; });
                    [...elem.querySelectorAll('ul > li > span > span > ul')].filter(function(x) { return x.textContent.indexOf(input.value) < 0;  }).forEach(function(x) { x.style.display = 'none'; });
                };
                elem.querySelectorAll('* > * > .key').forEach(function(x) {
                    if (map.mystate.closed[x.textContent]) {
                        util.getSiblings(x).forEach(function(y) { y.style.display = 'none'; });
                    }
                });
                elem.querySelectorAll('* > * > .key').forEach(function(x) {
                    x.onclick = function() {
                        if (map.mystate.closed[x.textContent]) {
                            map.mystate.closed[x.textContent] = undefined;
                            util.getSiblings(x).forEach(function(y) { y.style.display = 'block'; });
                        } else {
                            map.mystate.closed[x.textContent] = true;
                            util.getSiblings(x).forEach(function(y) { y.style.display = 'none'; });
                        }
                    };
                });
                [...elem.querySelectorAll('ul > li > span > span > ul')].filter(function(x) { return !x.querySelector('ul');}).forEach(function(x) {
                    x.onmouseover = function() {
                        [...x.querySelectorAll('.key')].filter(function(y) { return y.textContent.indexOf("tunniste") >= 0; }).forEach(function(tunnisteElem) {
                            var tunniste = util.getSiblings(tunnisteElem).map(function(y) { return y.textContent; }).join();
                            var feature = ret.getFeatureByTunniste(map, tunniste);
                            try {
                                selectInteraction.getFeatures().clear();
                            } catch (e) {
                                console.log(e);
                            }
                            if (feature) {
                                selectInteraction.getFeatures().push(feature);
                                select(feature);
                            }
                        });
                    };
                    x.onmouseout = function() {
                        [...x.querySelectorAll('.key')].filter(function(y) { return y.textContent.indexOf("tunniste") >= 0; }).forEach(function(tunnisteElem) {
                            var tunniste = util.getSiblings(tunnisteElem).map(function(y) { return y.textContent; }).join();
                            var feature = ret.getFeatureByTunniste(map, tunniste);
                            if (feature) {
                                unselect([feature]);
                            }
                        });
                    };
                    x.onclick = function() {
                        [...x.querySelectorAll('.key')].filter(function(y) { return y.textContent.indexOf("tunniste") >= 0; }).forEach(function(tunnisteElem) {
                            var tunniste = util.getSiblings(tunnisteElem).map(function(y) { return y.textContent; }).join();
                            var feature = ret.getFeatureByTunniste(map, tunniste);
                            if (feature) {
                                if (click) {
                                    click(feature);
                                }
                                map.getView().fit(feature.getGeometry().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
                            }
                        });
                    };
                });
                
                elem.querySelectorAll('ul > li > span > span > ul a.oid').forEach(function(x) {
                    x.onmouseover = function() {
                        var tunniste = x.textContent;
                        var feature = ret.getFeatureByTunniste(map, tunniste);
                        try {
                            selectInteraction.getFeatures().clear();
                        } catch (e) {
                            console.log(e);
                        }
                        if (feature) {
                            selectInteraction.getFeatures().push(feature);
                        }
                        select(feature, null, tunniste);
                    };
                    x.onmouseout = function() {
                        var tunniste = x.textContent;
                        var feature = ret.getFeatureByTunniste(map, tunniste);
                        if (feature) {
                            unselect([feature]);
                        } else {
                            unselect([]);
                        }
                    };
                    x.onclick = function() {
                        var tunniste = x.textContent;
                        var feature = ret.getFeatureByTunniste(map, tunniste);
                        if (feature) {
                            if (click) {
                                click(feature);
                            }
                            map.getView().fit(feature.getGeometry().getExtent(), {'maxZoom': 10, 'padding': [50,50,50,50], 'duration': 1000});
                        }
                    };
                });
            };
            map.on("moveend", f);
            f();
        },
        
        createPopup: function(elem) {
            var popupcontent = elem.querySelector('#popup-content');
            popupcontent.onmouseover = function() {
                popupcontent.style.display = 'block';
                popupcontent.style.opacity = '100';
            };
            popupcontent.onmouseout = function() {
                popupcontent.style.display = 'none';
            };
            return popupcontent;
        },
        
        styles: {
            // from ol source
            defaultWithColor: function(fillColor, strokeColor) {
                return new ol.style.Style({
                    image: new ol.style.Circle({
                        fill: new ol.style.Fill({
                            color: fillColor
                            }),
                        stroke: new ol.style.Stroke({
                            color: strokeColor,
                            width: 1.25
                            }),
                        radius: 5
                    }),
                    fill: new ol.style.Fill({
                        color: fillColor
                    }),
                    stroke: new ol.style.Stroke({
                        color: strokeColor,
                        width: 1.25
                    })
                });
            },
            
            circle: function(radius, color) {
                var vcolor = ol.color.asArray(color);
                vcolor[3] = 1.0;
                return new ol.style.Style({
                    image: new ol.style.Circle({
                        radius: radius,
                        fill: new ol.style.Fill({
                            color: color
                        }),
                        stroke: new ol.style.Stroke({
                            color: vcolor,
                            width: 1.0
                        })
                    })
                });
            },
            
            line: function(width, color) {
                return new ol.style.Style({
                    stroke: new ol.style.Stroke({
                        color: color,
                        width: width
                    })
                });
            },
            
            onLoad: function(func) {
                return func;
            },
            
            icon: function(url, flipped, rotation, anchor, scale, opacity) {
                return new ol.style.Style({
                    image: new ol.style.Icon({
                        src: url,
                        scale: scale ? scale : 1,
                        rotateWithView: true,
                        anchor: anchor ? anchor : [0.5, 0.5],
                        rotation: rotation ? 2*Math.PI*rotation/360 : 0,
                        opacity: opacity ? opacity : 1.0
                    })
                });
            },
            
            text: function(color, txt, offsetY) {
                return new ol.style.Style({
                    text: new ol.style.Text({
                        text: txt,
                        stroke: new ol.style.Stroke({
                            color: color
                        }),
                        fill: new ol.style.Fill({
                            color: color
                        }),
                        offsetY: offsetY || 0
                    })
                });
            }
        },
        
        newVectorLayerNoTile: function(url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames) {
            return ret.newVectorLayerImpl(false, url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames);
        },
        
        newVectorLayer: function(url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames) {
            return ret.newVectorLayerImpl(true, url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames);
        },
        
        newVectorLayerImpl: function(tiling, url, shortName, title_fi, title_en, opacity, propertyName, styleOrHandler, typeNames) {
            var u1 = url + (url.indexOf('?') < 0 ? '?' : '');
            u1 = u1.indexOf('.geojson') < 0 ? u1.replace('?', '.geojson?') : u1; 
            var u2 = (window.location.search.indexOf('profile') == -1 ? '' : '&profile=true') +
                     (!propertyName ? '' : '&propertyName=' + propertyName) +
                     (url.indexOf('time=') >= 0 ? '' : '&time=' + instant + '/' + instant) +
                     (!typeNames ? '' : '&typeNames=' + typeNames);

            var source = new ol.source.Vector({
                format: ret.format,
                projection: ret.projection,
                strategy: tiling ? ol.loadingstrategy.tile(ret.tileGrid) : ol.loadingstrategy.all,
                loader: function(extent, resolution, projection) {
                    if (tiling && (extent[0] < constants.dataExtent[0] ||
                                   extent[1] < constants.dataExtent[1] ||
                                   extent[2] > constants.dataExtent[2] ||
                                   extent[3] > constants.dataExtent[3])) {
                        return;
                    }
                    var kaavio = document.getElementById('kaavio');
                    fetch((u1 + (tiling ? '&bbox=' + extent.join(',') : '') + (kaavio && kaavio.checked ? '&presentation=diagram' : '') + u2).replace('?&','?'))
                        .then(function(response) { return response.json(); })
                        .then(function(response) {
                          var features = ret.format.readFeatures(response);
                          if (styleOrHandler instanceof Function) {
                              if (styleOrHandler.length == 1) {
                                  // on feature load
                                  features.forEach(styleOrHandler);
                              } else {
                                  // dynamic style
                                  features.forEach(function(f) { f.setStyle(styleOrHandler); });
                              }
                          }
                          source.addFeatures(features);
                        });
                }
            });
            var layer = new ol.layer.Vector({
                title: ret.mkLayerTitle(title_fi, title_en),
                shortName: shortName,
                source: source,
                opacity: opacity || 1.0,
                style: styleOrHandler instanceof Function ? undefined : styleOrHandler,
                extent: constants.dataExtent,
                renderBuffer: constants.renderBuffer,
                updateWhileInteracting: true,
                updateWhileAnimating: true
            });
            layer.setVisible(false);
            return layer;
        },
        
        mkLayerTitle: function(title_fi, title_en) {
            return '<span class="fi">' + title_fi + '</span><span class="en">' + title_en + '</span>';
        },
        
        newPngLayer: function(url, title, opacity, propertyName, typeNames) {
            var u1 = url + '.png?';
            var u2 = (window.location.search.indexOf('profile') == -1 ? '' : '&profile=true') + (propertyName === null ? '' : '&propertyName=' + propertyName) + '&time=' + instant + '/' + instant;
            u2 += (typeNames ? '&typeNames=' + typeNames : '');

            var source = new ol.source.TileImage({
                format: ret.format,
                projection: ret.projection,
                tileGrid: ret.tileGridAll,
                tileUrlFunction: function(tileCoord, resolution, projection) {
                    var extent = source.tileGrid.getTileCoordExtent(tileCoord);
                    if (extent[0] < constants.dataExtent[0] ||
                        extent[1] < constants.dataExtent[1] ||
                        extent[2] > constants.dataExtent[2] ||
                        extent[3] > constants.dataExtent[3]) {
                        return;
                    }
                    return u1 + 'bbox=' + extent.join(',') + u2;
                }
            });
            var layer = new ol.layer.Tile({
                title: title,
                source: source,
                opacity: opacity || 1.0,
                extent: constants.dataExtent
            });
            layer.setVisible(false);
            return layer;
        },
        
        newImageLayer: function(url, title, opacity) {
            var layer = new ol.layer.Image({
                title: title,
                opacity: opacity || 1.0,
                extent: constants.dataExtent,
                source: new ol.source.ImageStatic({
                  url: url,
                  projection: ret.projection,
                  imageExtent: constants.dataExtent,
                })
              });
            layer.setVisible(false);
            return layer;
        },
        
        newWMTSGroup: function(title, url, matrix) {
            var parser = new ol.format.WMTSCapabilities();
            var group = new ol.layer.Group({
                title: title,
                layers: []
            });
            
            var createLayer = function(matrix) { return function(response) {
                var result = parser.read(response);
                result.Contents.Layer.map(function(l) {
                    var id = l.Identifier;
                    var tms = l.TileMatrixSetLink.map(function(t) { return t.TileMatrixSet; });
                    if (tms.indexOf(matrix) > -1) {
                        var options = ol.source.WMTS.optionsFromCapabilities(result, {layer: id, matrixSet: matrix});
                        group.getLayers().push(ret.tileLayer(id, new ol.source.WMTS(options), 1.0));
                    }
                });
              };
            };
            
            fetch(url).then(function(x) { return x.text(); }).then(createLayer(matrix));
            
            return group;
        },
        
        taustaGroup: function(opacity) {
            var parser = new ol.format.WMTSCapabilities();
            var osm = ret.tileLayer('OpenStreetMap', new ol.source.OSM());
            osm.setVisible(true);
            var group = new ol.layer.Group({
                title: '<span class="fi">Taustat<span><span class="en">Backgrounds</span>',
                layers: [ret.newImageLayer('https://placehold.it/256/eeffee?text=+', 'Green', opacity),
                         ret.tileLayer('Grid', new ol.source.TileImage({projection: ret.projection, tileGrid: ret.tileGrid, tileUrlFunction: function(extent, resolution, projection) {var size = ret.tileGrid.getTileSize(extent[0]); return 'https://placehold.it/' + size + '?text=' + extent + ' (' + size + 'x' + size + ')&w=' + size + '&h=' + size + '';} }), opacity),
                         ret.tileLayer('Debug', new ol.source.TileDebug({projection: ret.projection, tileGrid: ret.tileGrid}, opacity)),
                         osm
                        ]
            });
            
            var createLayer = function(matrix, host) { return function(response) {
                var result = parser.read(response);
                result.Contents.Layer.map(function(l) {
                    var id = l.Identifier;
                    var tms = l.TileMatrixSetLink.map(function(t) { return t.TileMatrixSet; });
                    if (tms.indexOf(matrix) > -1) {
                        var options = ol.source.WMTS.optionsFromCapabilities(result, {layer: id, matrixSet: matrix});
                        if (host !== null) {
                            options.urls[0] = options.urls[0].replace(/[-a-z]+\.[a-z]+\.local/, host)
                                                             .replace(/\d+\.\d+\.\d+\.\d+/, host)
                                                             .replace('oag.vayla.fi', host);
                        }
                        group.getLayers().push(ret.tileLayer(id, new ol.source.WMTS(options), opacity));
                    }
                });
              };
            };
            
            if (window.location.hostname != 'localhost' && window.location.hostname != '127.0.0.1' && window.location.hostname.indexOf('digitraffic.fi') == -1) {
                var host = window.location.hostname;
                var baseurl = window.location.protocol + '//' + host;
                var maasto = baseurl + '/rasteripalvelu-mml/wmts/maasto/1.0.0/WMTSCapabilities.xml';
                var teema = baseurl + '/rasteripalvelu-mml/wmts/teema/1.0.0/WMTSCapabilities.xml';
                var kiinteisto = baseurl + '/rasteripalvelu-mml/wmts/kiinteisto/1.0.0/WMTSCapabilities.xml';
                var basic = baseurl + '/rasteripalvelu/service/wmts?request=getcapabilities';
                
                fetch(maasto)    .then(function(x) { return x.text(); }).then(createLayer('ETRS-TM35FIN', host));
                fetch(teema)     .then(function(x) { return x.text(); }).then(createLayer('ETRS-TM35FIN', host));
                fetch(kiinteisto).then(function(x) { return x.text(); }).then(createLayer('ETRS-TM35FIN', host));
                fetch(basic)     .then(function(x) { return x.text(); }).then(createLayer('EPSG:3067_PTP', host));
            }
            return group;
        },
        
        tileLayer: function(title, source, opacity) {
            var layer = new ol.layer.Tile({
                title: title,
                tileGrid: ret.tileGrid,
                opacity: opacity || 0.3,
                source: source,
                renderBuffer: constants.renderBuffer,
                updateWhileInteracting: true,
                updateWhileAnimating: true
            });
            layer.setVisible(false);
            return layer;
        },
        
        map: function(overlays, layers, taustaGroup, renderer) {
            return new ol.Map({
                target: 'kartta',
                overlays: overlays,
                layers: layers.concat(taustaGroup),
                renderer: renderer || 'canvas',
                view: new ol.View({
                    center: [342900, 6820390],
                    resolution: 2048,
                    resolutions: constants.resolutions,
                    projection: ret.projection
                }),
                controls: [
                    new ol.control.Attribution({collapsible: false}),
                    new ol.control.Zoom(),
                    new ol.control.ZoomSlider(),
                    new ol.control.ScaleLine(),
                    new ol.control.MousePosition({
                        coordinateFormat: function(c) {
                            return Math.round(c[0]) + "," + Math.round(c[1]);
                        }
                    }),
                    new ol.control.LayerSwitcher()
                ]
            });
        }
    };
    
    ol.proj.addProjection(ret.projection);
    return ret;
};