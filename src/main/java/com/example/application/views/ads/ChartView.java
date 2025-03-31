package com.example.application.views.ads;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;

import java.util.ArrayList;
import java.util.List;


public class ChartView extends Div {
    public ChartView(List<Double> prices) {
        UI.getCurrent().getPage().addJavaScript("https://cdn.jsdelivr.net/npm/chart.js");

        Div container = new Div();
        container.setWidth("600px");
        container.setHeight("400px");
        add(container);

        Element canvas = new Element("canvas");
        canvas.setAttribute("width", "600");
        canvas.setAttribute("height", "400");
        container.getElement().appendChild(canvas);

        UI.getCurrent().getPage().executeJs("""
                    window.waitForChart = new Promise((resolve) => {
                        const check = () => {
                            if(typeof Chart !== 'undefined') {
                                resolve(true);
                                return;
                            }
                            setTimeout(check, 50);
                        }
                        check();
                    });
                """).then(r -> initChart(canvas, prices));
    }

    private void initChart(Element canvas, List<Double> prices) {
        List<Integer> integers = new ArrayList<>();

        for (int i = 0; i < prices.size(); i++) {
            integers.add(i);
        }
        var code = """
                    const ctx = this;
                    new Chart(ctx, {
                        type: 'line',
                        data: {
                            labels: %s,
                            datasets: [{
                                label: 'Изменение цен',
                                data: %s,
                                backgroundColor: 'rgba(54, 162, 235, 0.5)'
                            }]
                        },
                        options: {
                            responsive: false,
                            maintainAspectRatio: false
                        }
                    });
                """.formatted(integers, prices);
        canvas.executeJs(code);
    }
}
