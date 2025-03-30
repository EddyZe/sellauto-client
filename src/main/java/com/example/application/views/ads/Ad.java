package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.PhotoBasePayload;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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
        setWidthFull();
        setAlignItems(Alignment.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setHorizontalComponentAlignment(Alignment.CENTER);
    }


    @Override
    public void setParameter(BeforeEvent beforeEvent, String param) {
        try {
            var adLay = new VerticalLayout();
            adLay.setWidth("80%");
            adLay.setClassName("custom-block");
            adLay.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

            var id =  Long.parseLong(param);
            var ad = sellAutoRestClient.getAdId(id);

            var title = new H3(ad.getTitle());
            title.setWidth("60%");

            adLay.add(title, generatePhotoLayout(ad));

            add(adLay);
        } catch (NumberFormatException e) {
            log.error("Error: {}", e.getMessage());
            Notification.show("Произошла ошибка...", 5000, Notification.Position.TOP_CENTER);
        }
    }

    private VerticalLayout generatePhotoLayout(AdPayload ad) {
        var photos = ad.getCar().getPhotos()
                .stream()
                .map(this::createImage)
                .toList();

        var mainPhoto = photos.getFirst();
        mainPhoto.setMaxWidth("500px");
        mainPhoto.setMaxHeight("300px");
        var photosLay = new HorizontalLayout();
        photosLay.setWidth("90%");
        for (int i = 1; i < photos.size(); i++) {
            var img = photos.get(i);
            img.setMaxHeight("90px");
            img.setMaxWidth("150px");
            photosLay.add(photos.get(i));
        }
        var adPhotos = new VerticalLayout(mainPhoto, photosLay);
        adPhotos.setWidthFull();
        adPhotos.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        return adPhotos;
    }

    private Image createImage(PhotoBasePayload p) {
        try {
            var res = sellAutoRestClient.getPhoto(p.getPhotoId());
            var img = ComponentRenders.createImage(res, p, "100%", "100%");
            img.addClickListener(e -> {
                var dialog = new Dialog();
                dialog.add(ComponentRenders.createImage(res, p, "900px", "450px"));
                Button closeButton = new Button(new Icon("lumo", "cross"),
                        (ev) -> dialog.close());
                closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                dialog.getHeader().add(closeButton);
                dialog.open();
            });
            img.setClassName("custom-card");

            return img;
        } catch (Exception e) {
            log.error("error get image: {}", e.getMessage());
            return null;
        }
    }

}
