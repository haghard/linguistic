function drawGraph(data) {

    nv.addGraph({

        generate: function () {

            var width = nv.utils.windowSize().width - 40,
                height = nv.utils.windowSize().height - 40;

            var d3Colors = d3.scale.category20();

            var chart = nv.models.forceDirectedGraph()
                .width(width)
                .height(height)
                .radius(20)
                .margin({top: 20, right: 20, bottom: 20, left: 20})
                .color(function (d) {
                    return d3Colors(d.group)
                })
                .nodeExtras(function (node) {
                    node
                        .append("text")
                        .attr("dx", 12)
                        .attr("dy", ".35em")
                        .text(function (d) {
                            return d.name
                        });
                });

            chart.dispatch.on('renderEnd', function () {
                console.log('render complete');
            });

            d3.select('#graph')
                .attr('width', width)
                .attr('height', height)
                .datum(data)  // all_data is set below
                .call(chart);
            return chart;
        },

        callback: function (graph) {
            window.onresize = function () {
                var width = nv.utils.windowSize().width - 40,
                    height = nv.utils.windowSize().height - 40,
                    margin = graph.margin();
                if (width < margin.left + margin.right + 20) {
                    width = margin.left + margin.right + 20;
                }

                if (height < margin.top + margin.bottom + 20)
                    height = margin.top + margin.bottom + 20;

                graph.width(width).height(height);

                d3.select('#graph')
                    .attr('width', width)
                    .attr('height', height)
                    .call(graph);
            };
        }
    });
}