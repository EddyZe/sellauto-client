package com.example.application.views.util;

import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.PhotoBasePayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.ads.Ad;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;


@Slf4j
public class ComponentRenders {

    public static VerticalLayout generateComponentAdList(AdPayload ad, Resource photoRes, String photoMaxWidth, String photoMaxHeight) {
        var mainLay = new HorizontalLayout();
        var link = new RouterLink("%s %s рублей".formatted(
                ad.getTitle(),
                NumberFormat.getInstance(Locale.of("ru", "RU"))
                        .format(ad.getPrices().getLast().getPrice().intValue())),
                Ad.class,
                ad.getAdId().toString());

        var stats = new Span(ad.getIsActive() ? "Продается" : "Продано");
        stats.setHeight("20px");
        if (!ad.getIsActive()) {
            stats.setClassName("ad-sold");
        } else {
            stats.setClassName("ad-for-sale");
        }

        var linkAndStatus = new HorizontalLayout(link, stats);

        if (ad.getCar() != null && !ad.getCar().getPhotos().isEmpty()) {
            var firstPhoto = ad.getCar().getPhotos().getFirst();
            try {
                var photo = createImage(photoRes, firstPhoto, photoMaxWidth, photoMaxHeight);
                mainLay.add(photo);
            } catch (SellAutoApiException e) {
                log.error(e.getMessage());
            }
        }

        var car = ad.getCar();
        var carName = new Span("%s %s %s %d км".formatted(Objects.requireNonNull(car).getBrand().getTitle(),
                car.getModel().getTitle(),
                car.getTransmissionType().toString(),
                car.getMileage()));
        var vin = new Span("Vin: " + car.getVin());
        var drive = new Span("%s %s %d".formatted(car.getDrive().toString(), car.getColor().getTitle(), car.getYear()));


        mainLay.add(new VerticalLayout(linkAndStatus, carName, drive, vin));

        var ver = new VerticalLayout(mainLay);
        ver.setClassName("custom-card");
        return  ver;
    }

    public static Image createImage(Resource resourse, PhotoBasePayload firstPhoto,
                                        String maxWidth, String maxHeight) {
        var photo = new Image(new StreamResource(resourse.getFilename() == null ?
                "image" + firstPhoto.getPhotoId() : resourse.getFilename(),
                (InputStreamFactory) () -> {
                    try {
                        return resourse.getInputStream();
                    } catch (IOException e) {
                        log.error("error get ingupStream from resourses", e);
                        return null;
                    }
                }), "photo");
        photo.setMaxWidth(maxWidth);
        photo.setHeight(maxHeight);
        return photo;
    }
}
