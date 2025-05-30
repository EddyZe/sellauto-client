package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.BrandDetailPayload;
import com.example.application.clients.sellauto.payloads.ColorBasePayload;
import com.example.application.clients.sellauto.payloads.ModelBasePayload;
import com.example.application.enums.*;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.util.ComponentRenders;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@PageTitle("Объявления")
@Route(value = "", layout = MainLayout.class)
@Menu(order = 0, icon = LineAwesomeIconUrl.LIST_ALT)
public class AdListView extends HorizontalLayout {

    private final SellAutoRestClient sellAutoRestClient;

    public AdListView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        try {
            var ads = createAdList();
            ads.setHeight("1050px");

            var titleFilter = new H3("Фильтр");
            titleFilter.setWidthFull();
            var filterBlock = new VerticalLayout(titleFilter);
            filterBlock.setClassName("filter-block");
            filterBlock.setWidth("35.5%");

            var colors = new Select<ColorBasePayload>();
            colors.setRenderer(new ComponentRenderer<Component, ColorBasePayload>(c -> new Span(c.getTitle())));
            colors.setItems(sellAutoRestClient.getColors().getColors());
            colors.setLabel("Цвет");
            colors.setEmptySelectionAllowed(true);
            colors.setEmptySelectionCaption("Выберите цвет");
            colors.setWidthFull();

            var brands = new Select<BrandDetailPayload>();
            brands.setLabel("Бренд");
            brands.setEmptySelectionCaption("Выберете бренд");
            brands.setEmptySelectionAllowed(true);
            brands.setWidthFull();
            var models = new Select<ModelBasePayload>();
            models.setWidthFull();
            models.setEmptySelectionAllowed(true);
            models.setEmptySelectionCaption("Выберете модель");
            models.setLabel("Модель");
            models.setRenderer(new ComponentRenderer<Component, ModelBasePayload>(m ->
                    new Span(m.getTitle())));
            brands.setItems(sellAutoRestClient.getBrands().getBrands());
            brands.setRenderer(new ComponentRenderer<Component, BrandDetailPayload>(b ->
                    new Span(b.getTitle())));
            brands.addValueChangeListener(e -> {
                        if (e.getValue() != null) {
                            models.setItems(e.getValue().getModel());
                        } else
                            models.setItems(new ArrayList<>());
                    }
            );

            var brandsLay = new HorizontalLayout(brands, models);
            brandsLay.setWidthFull();

            var yearFrom = new NumberField("От");
            yearFrom.setPlaceholder("Введите год...");
            yearFrom.setWidthFull();
            var yearTo = new NumberField("До");
            yearTo.setPlaceholder("Введите год...");
            yearTo.setWidthFull();

            var yearLay = new HorizontalLayout(yearFrom, yearTo);
            yearLay.setWidthFull();

            var sortYear = new Select<Sort>();
            sortYear.setLabel("Сортировка по годам");
            sortYear.setItems(Sort.values());
            sortYear.setRenderer(new ComponentRenderer<Component, Sort>(s -> new Span(s.toString())));
            sortYear.setEmptySelectionAllowed(true);
            sortYear.setWidthFull();
            sortYear.setEmptySelectionCaption("Выбрать тип сортировки");

            var sortPrice = new Select<Sort>();
            sortPrice.setLabel("Сортировка по цене");
            sortPrice.setWidthFull();
            sortPrice.setItems(Sort.values());
            sortPrice.setRenderer(new ComponentRenderer<Component, Sort>(s -> new Span(s.toString())));
            sortPrice.setEmptySelectionAllowed(true);
            sortPrice.setWidthFull();
            sortPrice.setEmptySelectionCaption("Выбрать тип сортировки");

            var priceFrom = new NumberField();
            priceFrom.setPlaceholder("Цена от...");
            priceFrom.setWidth("50%");
            var priceTo = new NumberField();
            priceTo.setPlaceholder("Цена до...");
            priceTo.setWidth("50%");
            var filterPrices = new HorizontalLayout(priceFrom, priceTo);

            var mileageFrom = new NumberField();
            mileageFrom.setPlaceholder("Пробег от...");
            mileageFrom.setWidth("50%");
            var mileageTo = new NumberField();
            mileageTo.setPlaceholder("Пробег до...");
            mileageTo.setWidth("50%");
            var mileageFilter = new HorizontalLayout(mileageFrom, mileageTo);

            var drive = new Select<DriveMode>();
            drive.setLabel("Тип привода");
            drive.setWidthFull();
            drive.setItems(DriveMode.values());
            drive.setEmptySelectionAllowed(true);
            drive.setEmptySelectionCaption("Выбрать тип привода");
            drive.setRenderer(new ComponentRenderer<Component, DriveMode>(s -> new Span(s.toString())));

            var body = new Select<BodyType>();
            body.setLabel("Тип Кузова");
            body.setWidthFull();
            body.setItems(BodyType.values());
            body.setEmptySelectionAllowed(true);
            body.setEmptySelectionCaption("Выбрать тип Кузова");
            body.setRenderer(new ComponentRenderer<Component, BodyType>(s -> new Span(s.toString())));

            var engine = new Select<EngineType>();
            engine.setLabel("Тип двигателя");
            engine.setWidthFull();
            engine.setItems(EngineType.values());
            engine.setEmptySelectionAllowed(true);
            engine.setEmptySelectionCaption("Выбрать тип двигателя");
            engine.setRenderer(new ComponentRenderer<Component, EngineType>(s -> new Span(s.toString())));

            var transmission = new Select<TransmissionType>();
            transmission.setLabel("Тип КПП");
            transmission.setWidthFull();
            transmission.setItems(TransmissionType.values());
            transmission.setEmptySelectionAllowed(true);
            transmission.setEmptySelectionCaption("Выбрать тип КПП");
            transmission.setRenderer(new ComponentRenderer<Component, TransmissionType>(s -> new Span(s.toString())));

            var btn = new Button("Найти", e -> {
                if (yearFrom.getValue() != null && !checkYear(yearFrom.getValue().intValue())) {
                    Notification.show("Год должен быть не менее 1900 и не более текущего...", 5000, Notification.Position.TOP_CENTER);
                    return;
                }

                if (yearTo.getValue() != null && !checkYear(yearTo.getValue().intValue())) {
                    Notification.show("Год должен быть не менее 1900 и не более текущего...", 5000, Notification.Position.TOP_CENTER);
                    return;
                }

                Map<String, String> params = new HashMap<>();

                if (colors.getValue() != null) {
                    params.put("color", colors.getValue().getTitle());
                }

                if (brands.getValue() != null) {
                    params.put("brand", brands.getValue().getTitle());
                }

                if (models.getValue() != null) {
                    params.put("model", models.getValue().getTitle());
                }

                if (yearFrom.getValue() != null) {
                    params.put("year-from", String.valueOf(yearFrom.getValue().intValue()));
                }

                if (yearTo.getValue() != null) {
                    params.put("year-to", String.valueOf(yearTo.getValue().intValue()));
                }

                if (sortYear.getValue() != null) {
                    params.put("sort-year", sortYear.getValue().name());
                }

                if (sortPrice.getValue() != null) {
                    params.put("sort-price", sortPrice.getValue().name());
                }

                if (priceFrom.getValue() != null) {
                    params.put("price-from", priceFrom.getValue().toString());
                }

                if (priceTo.getValue() != null) {
                    params.put("price-to", priceTo.getValue().toString());
                }

                if (mileageFrom.getValue() != null) {
                    params.put("mileage-from", String.valueOf(mileageFrom.getValue().intValue()));
                }

                if (mileageTo.getValue() != null) {
                    params.put("mileage-to", String.valueOf(mileageTo.getValue().intValue()));
                }

                if (drive.getValue() != null) {
                    params.put("drive", drive.getValue().toString());
                }

                if (body.getValue() != null) {
                    params.put("body", body.getValue().toString());
                }

                if (engine.getValue() != null) {
                    params.put("engine", engine.getValue().toString());
                }

                if (transmission.getValue() != null) {
                    params.put("transmission", engine.getValue().toString());
                }

                ads.setItems(sellAutoRestClient.getAds(params).getAds());
            });

            btn.setWidthFull();
            filterBlock.add(
                    colors,
                    brandsLay,
                    yearLay,
                    sortYear,
                    sortPrice,
                    new Span("Искать по цене"),
                    filterPrices,
                    new Span("Пробег"),
                    mileageFilter,
                    drive,
                    engine,
                    transmission,
                    btn);

            add(ads, filterBlock);
        } catch (SellAutoApiException e) {
            Notification.show("Ошибка загрузки объявлений.", 5000, Notification.Position.TOP_CENTER);
            log.error(e.getMessage());
        }

    }

    private boolean checkYear(Integer y) {
        return y != null && y >= 1900 && y <= LocalDateTime.now().getYear();
    }

    private VirtualList<AdPayload> createAdList() {
        var ads = new VirtualList<AdPayload>();
        ads.setItems(sellAutoRestClient.getAds().getAds());
        ads.setRenderer(
                new ComponentRenderer<>(ad -> {
                    var photoId = ad.getCar().getPhotos().getFirst().getPhotoId();
                    var photoRes = sellAutoRestClient.getPhoto(photoId);
                    return ComponentRenders.generateComponentAdList(ad, photoRes, "290px", "175px");
                })
        );
        ads.setClassName("custom-block");
        return ads;
    }
}
