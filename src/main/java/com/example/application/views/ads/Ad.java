package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Route("/ads")
public class Ad extends VerticalLayout implements HasUrlParameter<String> {

    private final SellAutoRestClient sellAutoRestClient;

    public Ad(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();
        setClassName("custom-card");
    }


    @Override
    public void setParameter(BeforeEvent beforeEvent, String param) {
        try {
            long id =  Long.parseLong(param);
            var ad = sellAutoRestClient.getAdId(id);



        } catch (NumberFormatException e) {
            log.error("Error: {}", e.getMessage());
            Notification.show("Произошла ошибка...", 5000, Notification.Position.TOP_CENTER);
        }
    }

}
