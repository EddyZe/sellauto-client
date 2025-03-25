package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.vaadin.lineawesome.LineAwesomeIconUrl;


@PageTitle("Объявления")
@Route(value = "/ads",  layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.LIST_ALT)
public class AdListView extends VerticalLayout {

    private final SellAutoRestClient sellAutoRestClient;

    public AdListView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        try {
            ProfilePayload profile = sellAutoRestClient.getProfile();
            add(new H1("Welcome, " + profile.getFirstName() + " " + profile.getLastName()));
        } catch (SellAutoApiException e) {
            Notification.show("Error: " + e.getMessage());
        }

    }
}
