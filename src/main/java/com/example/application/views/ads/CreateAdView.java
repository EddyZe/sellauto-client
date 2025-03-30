package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.BrandDetailPayload;
import com.example.application.clients.sellauto.payloads.ColorBasePayload;
import com.example.application.clients.sellauto.payloads.CreateNewAdPayload;
import com.example.application.clients.sellauto.payloads.ModelBasePayload;
import com.example.application.enums.BodyType;
import com.example.application.enums.DriveMode;
import com.example.application.enums.EngineType;
import com.example.application.enums.TransmissionType;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;

@PageTitle("Создание объявления")
@Route(value = "ads/create", layout = MainLayout.class)
@Slf4j
public class CreateAdView extends VerticalLayout {

    private final SellAutoRestClient sellAutoRestClient;
    private final String downloadDir;

    private final Binder<CreateNewAdPayload> binder = new BeanValidationBinder<>(CreateNewAdPayload.class);

    public CreateAdView(SellAutoRestClient sellAutoRestClient, @Value("${sell-auto.data.files}") String downloadDir) {
        this.sellAutoRestClient = sellAutoRestClient;
        this.downloadDir = downloadDir;
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        setWidth("50%");
        setClassName("custom-block");

        try {
            init();
        } catch (SellAutoApiException e) {
            log.error("error", e);
        }
    }

    private void init() {
        var title = new TextField("Название объявления");
        title.setRequired(true);
        title.setWidthFull();
        var description = new TextArea("Описание");
        description.setRequired(true);
        description.setWidthFull();
        var price = new NumberField("Цена");
        price.setRequired(true);
        price.setWidthFull();
        var year = new NumberField("Год выпуска");
        year.setRequired(true);
        year.setWidthFull();
        var vin = new TextField("VIN");
        vin.setRequired(true);
        vin.setWidthFull();
        var mileage = new NumberField("Пробег");
        mileage.setWidthFull();
        mileage.setRequired(true);
        var engine = new Select<EngineType>();
        engine.setWidthFull();
        engine.setLabel("Тип двигателя");
        engine.setEmptySelectionCaption("Выберете тип двигателя");
        engine.setEmptySelectionAllowed(true);
        engine.setItems(EngineType.values());
        engine.setRenderer(new ComponentRenderer<Component, EngineType>(type ->
                new Span(type.toString())));
        var transmission = new Select<TransmissionType>();
        transmission.setWidthFull();
        transmission.setLabel("Коробка передач");
        transmission.setEmptySelectionAllowed(true);
        transmission.setEmptySelectionCaption("Выберете коробку передач");
        transmission.setItems(TransmissionType.values());
        transmission.setRenderer(new ComponentRenderer<Component, TransmissionType>(type ->
                new Span(type.toString())));
        var body = new Select<BodyType>();
        body.setWidthFull();
        body.setLabel("Тип кузова");
        body.setEmptySelectionCaption("Выберете тип кузова");
        body.setEmptySelectionAllowed(true);
        body.setItems(BodyType.values());
        body.setRenderer(new ComponentRenderer<Component, BodyType>(type ->
                new Span(type.toString())));
        var driveMode = new Select<DriveMode>();
        driveMode.setWidthFull();
        driveMode.setLabel("Привод");
        driveMode.setEmptySelectionAllowed(true);
        driveMode.setEmptySelectionCaption("Выберете привод");
        driveMode.setItems(DriveMode.values());
        driveMode.setRenderer(new ComponentRenderer<Component, DriveMode>(type ->
                new Span(type.toString())));

        var brands = this.sellAutoRestClient.getBrands().getBrands();
        var brand = new Select<BrandDetailPayload>();
        brand.setWidthFull();
        var model = new Select<ModelBasePayload>();
        model.setWidthFull();
        brand.setLabel("Марка");
        brand.setEmptySelectionCaption("Выберете марку");
        brand.setEmptySelectionAllowed(true);
        brand.setItems(brands);
        brand.setRenderer(new ComponentRenderer<Component, BrandDetailPayload>(b ->
                new Span(b.getTitle())));

        model.setLabel("Модель");
        model.setEmptySelectionAllowed(true);
        model.setEmptySelectionCaption("Выберете модель");
        model.setRenderer(new ComponentRenderer<Component, ModelBasePayload>(m ->
                new Span(m.getTitle())));

        brand.addValueChangeListener(e -> {
            if (brand.getValue() != null) {
                model.setItems(e.getValue().getModel());
            } else
                model.setItems(new ArrayList<>());
        });

        var colors = new Select<ColorBasePayload>();
        colors.setWidthFull();
        colors.setLabel("Цвет");
        colors.setEmptySelectionCaption("Выберете цвет");
        colors.setEmptySelectionAllowed(true);
        colors.setItems(this.sellAutoRestClient.getColors().getColors());
        colors.setRenderer(new ComponentRenderer<Component, ColorBasePayload>(color ->
                new Span(color.getTitle())));


        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setDropLabelIcon(VaadinIcon.UPLOAD.create());
        upload.setAcceptedFileTypes("image/jpeg", ".jpg", ".jpeg", "image/png", ".png");
        upload.setMaxFiles(5);
        upload.setWidthFull();

        var btn = new Button("Создать", e -> {
            if (colors.getValue() == null || model.getValue() == null || brand.getValue() == null ||
                driveMode.getValue() == null || body.getValue() == null || transmission.getValue() == null ||
                engine.getValue() == null) {
                Notification.show("Выберите все параметры авто. (Привод, тип коробки передач и т.д.)", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            if (buffer.getFiles().isEmpty()) {
                Notification.show("Загрузите фото.", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            try {
                var newAd = CreateNewAdPayload.builder()
                        .title(title.getValue())
                        .description(description.getValue())
                        .price(price.getValue())
                        .year(year.getValue().intValue())
                        .vin(vin.getValue())
                        .mileage(mileage.getValue().intValue())
                        .engineType(engine.getValue())
                        .transmissionType(transmission.getValue())
                        .bodyType(body.getValue())
                        .drive(driveMode.getValue())
                        .brandTitle(brand.getValue().getTitle())
                        .modelTitle(model.getValue().getTitle())
                        .colorTitle(colors.getValue().getTitle())
                        .build();

                var resp = sellAutoRestClient.createAd(newAd, new ArrayList<>(buffer.getFiles()
                        .stream()
                        .map(f -> {
                            Path fileName = Paths.get(downloadDir, f);
                            try (var is = buffer.getInputStream(f);
                                 var bos = new BufferedOutputStream(Files.newOutputStream(fileName))) {
                                bos.write(is.readAllBytes());
                            } catch (IOException ex) {
                                log.error("error read files", ex);
                                return null;
                            }
                            return new FileSystemResource(fileName);
                        })
                        .toList()));
                Notification.show("Объявление создано.", 5000, Notification.Position.TOP_CENTER);
                UI.getCurrent().navigate("/ads/%d".formatted(resp.getAdId()));
            } catch (Exception ex) {
                log.error(ex.getMessage(), ex);
                Notification.show("Ошибка создания объявления... Заполните все поля.", 5000, Notification.Position.TOP_CENTER);
            }
        });


        binder.forField(price)
                .withValidator(
                        p -> p != null && p > 1, "Цена должна быть больше чем 1"
                )
                .bind(CreateNewAdPayload::getPrice, CreateNewAdPayload::setPrice);

        binder.forField(title)
                .withValidator(
                        new StringLengthValidator("Наименование не должно быть пустым", 1, 250)
                )
                .bind(CreateNewAdPayload::getTitle, CreateNewAdPayload::setTitle);

        binder.forField(description)
                .withValidator(
                        new StringLengthValidator("Описание должно быть не менее 5 символов", 5, 4096)
                )
                .bind(CreateNewAdPayload::getDescription, CreateNewAdPayload::setDescription);

        binder.forField(year)
                .withValidator(
                        y -> y != null && y > 1900. && LocalDateTime.now().getYear() >= y,
                        "Год должен быть больше чем 1900 и меньше чем текущий..."
                )
                .bind(CreateNewAdPayload::getPrice, CreateNewAdPayload::setPrice);

        binder.forField(vin)
                .withValidator(
                        new StringLengthValidator("VIN не может быть пустым и больше 50 символов", 1, 50)
                )
                .bind(CreateNewAdPayload::getVin, CreateNewAdPayload::setVin);


        binder.forField(mileage)
                .withValidator(
                        m -> m != null && m > 1, "Пробег должен быть больше чем 1..."
                )
                .bind(CreateNewAdPayload::getPrice, CreateNewAdPayload::setPrice);

        add(title, description, price, year, vin, mileage, engine, transmission, body, driveMode, brand, model, colors, upload, btn);
    }
}
