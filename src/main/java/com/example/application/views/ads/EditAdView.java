package com.example.application.views.ads;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.AdPayload;
import com.example.application.clients.sellauto.payloads.EditAdPayload;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PageTitle("Редактирование объявления")
@Route(value = "ads/edit", layout = MainLayout.class)
public class EditAdView extends VerticalLayout implements HasUrlParameter<String> {

    private final SellAutoRestClient sellAutoRestClient;
    private final Binder<EditAdPayload> binder = new BeanValidationBinder<>(EditAdPayload.class);

    public EditAdView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;

        setHorizontalComponentAlignment(Alignment.CENTER);
        setSizeFull();
    }


    @Override
    public void setParameter(BeforeEvent beforeEvent, String s) {
        try {
            var adId = Long.parseLong(s);
            var ad = sellAutoRestClient.getAdId(adId);

            var editLay = new VerticalLayout();
            editLay.setWidth("60%");
            editLay.setClassName("custom-block");

            var title = new TextField("Наименование");
            title.setValue(ad.getTitle());
            title.setWidthFull();
            var descr = new TextField("Описание");
            descr.setValue(ad.getDescription());
            descr.setWidthFull();
            var price = new NumberField("Цена");
            price.setWidthFull();
            price.setValue(ad.getPrices().getLast().getPrice());

            var okButton = createOkButton(title, descr, price, ad, adId);

            var cancel = new Button("Отмена", e ->
                    UI.getCurrent().navigate("ads/" + adId));

            configureBinder(title, descr, price);

            editLay.add(
                    title,
                    descr,
                    price,
                    new HorizontalLayout(okButton, cancel)
            );

            add(editLay);
        } catch (SellAutoApiException e) {
            log.error("error find ad", e);
            UI.getCurrent().navigate(UserAdsView.class);
            Notification.show("Что-то пошло не так...", 5000, Notification.Position.TOP_CENTER);
        }
    }

    private void configureBinder(TextField title, TextField descr, NumberField price) {
        binder.forField(title)
                .withValidator(
                        new StringLengthValidator("Наименование не может быть пустым и более 50 символов", 1, 50)
                )
                .bind(EditAdPayload::getTitle, EditAdPayload::setTitle);

        binder.forField(descr)
                .withValidator(
                        new StringLengthValidator("Описание не должно быть меньше чем 5 символов", 5, 4096)
                )
                .bind(EditAdPayload::getDescription, EditAdPayload::setDescription);

        binder.forField(price)
                .withValidator(
                        new DoubleRangeValidator("Цена не может быть меньше чем 1", 1., Double.MAX_VALUE)
                )
                .bind(EditAdPayload::getPrice, EditAdPayload::setPrice);
    }

    private Button createOkButton(TextField title, TextField descr, NumberField price, AdPayload ad, long adId) {
        return new Button("Применить", e -> {
            if (title.getValue() == null || descr.getValue() == null || price.getValue() == null) {
                Notification.show("Заполните все поля!", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            if (price.getValue() < 1) {
                Notification.show("Цена не должна быть меньше чем 1!", 5000, Notification.Position.TOP_CENTER);
                return;
            }
            try {
                sellAutoRestClient.editAd(EditAdPayload.builder()
                        .isActive(ad.getIsActive())
                        .description(descr.getValue())
                        .price(price.getValue())
                        .title(title.getValue())
                        .build(), adId);
                Notification.show("Изменения сохранены", 5000, Notification.Position.TOP_CENTER);
                UI.getCurrent().navigate("ads/" + adId);
            } catch (SellAutoApiException exe) {
                log.error("error edit", exe);
                Notification.show("Произошла ошибка при изменении", 5000, Notification.Position.BOTTOM_CENTER);
                UI.getCurrent().navigate("ads/" + adId);
            }
        });
    }
}
