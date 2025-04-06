package com.example.application.views.profile;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.EditProfilePayload;
import com.example.application.clients.sellauto.payloads.FeedBackPayload;
import com.example.application.clients.sellauto.payloads.ProfilePayload;
import com.example.application.components.FeedBackComment;
import com.example.application.enums.Role;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.adminpanel.MainAdminView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.lineawesome.LineAwesomeIconUrl;


@PageTitle("Профиль")
@Route(value = "/profile", layout = MainLayout.class)
@Menu(order = 1, icon = LineAwesomeIconUrl.PERSON_BOOTH_SOLID)
@Slf4j
public class ProfileView extends VerticalLayout {

    private final SellAutoRestClient sellAutoRestClient;


    private final Binder<EditProfilePayload> editProfileBinder = new BeanValidationBinder<>(EditProfilePayload.class);

    private boolean isEditMode = false;

    public ProfileView(SellAutoRestClient sellAutoRestClient) {
        this.sellAutoRestClient = sellAutoRestClient;
        setSizeFull();
        setClassName("custom-card");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        try {
            ProfilePayload profile = sellAutoRestClient.getProfile();
            var headerLayout = new HorizontalLayout();
            headerLayout.setWidthFull();
            var helloMessage = new H1("Привет, " + profile.getFirstName() + " " + profile.getLastName());
            helloMessage.setWidthFull();
            headerLayout.add(helloMessage);
            addAdminPanelButton(profile, headerLayout);

            var dataLayout = new VerticalLayout();
            dataLayout.setSizeFull();

            var editLayout = new HorizontalLayout();
            editLayout.setSizeFull();
            editLayout.setClassName("custom-card");

            var firstName = new TextField("Имя");
            firstName.setValue(profile.getFirstName());
            firstName.setWidthFull();
            firstName.setReadOnly(true);
            var lastName = new TextField("Фамилия");
            lastName.setWidthFull();
            lastName.setValue(profile.getLastName());
            lastName.setReadOnly(true);

            var phoneNumber = new TextField("Номер телефона");
            phoneNumber.setWidthFull();
            phoneNumber.setValue(profile.getAccount().getPhoneNumber());
            phoneNumber.setReadOnly(true);

            var email = new TextField("Email");
            email.setWidthFull();
            email.setValue(profile.getAccount().getEmail());
            email.setReadOnly(true);

            var editButtonLayout = createEditMenu(phoneNumber, firstName, lastName);


            dataLayout.add(firstName, lastName, phoneNumber, email, editButtonLayout);
            editLayout.add(dataLayout);

            var infoLayout = new VerticalLayout();
            infoLayout.setSizeFull();

            var userIds = new Span("Ваш ID: %d".formatted(profile.getUserId()));
            var adsSize = new Span("Опубликовано объявлений: %d".formatted(profile.getAds().size()));
            var rating = new Span("Ваш рейтинг: %.1f ⭐".formatted(profile.getRating()));


            infoLayout.add(userIds, adsSize, rating);
            var titleFeedbacks = new H3("Ваши отзывы");
            titleFeedbacks.setWidthFull();
            var myFeedback = new VirtualList<FeedBackPayload>();
            myFeedback.setItems(sellAutoRestClient.getUserFeedBack(profile.getUserId()).getFeedbacks());
            myFeedback.setSizeFull();
            myFeedback.setRenderer(new ComponentRenderer<>(f ->
                    new FeedBackComment(f, profile, sellAutoRestClient)));
            infoLayout.add(titleFeedbacks, myFeedback);

            editLayout.add(infoLayout);

            add(headerLayout, editLayout);
        } catch (SellAutoApiException e) {
            log.error("SellAutoApiException", e);
            Notification.show("Ошибка загрузки профиля", 5000, Notification.Position.TOP_CENTER);
        }
    }

    private HorizontalLayout createEditMenu(TextField phoneNumber, TextField firstName, TextField lastName) {
        var edit = new Button("Редактировать");
        var approvedEdit = new Button("Принять изменения");
        approvedEdit.setVisible(false);
        configureEditBinder(phoneNumber, firstName, lastName);
        approvedEdit.addClickListener(e -> UI.getCurrent().access(() -> {
            try {
                sellAutoRestClient.editProfile(EditProfilePayload.builder()
                        .firstName(firstName.getValue())
                        .phoneNumber(phoneNumber.getValue())
                        .lastName(lastName.getValue())
                        .build());
                approvedEdit.setVisible(false);
                setEditMode(edit, approvedEdit, phoneNumber, firstName, lastName);
                Notification.show("Изменения сохранены...", 5000, Notification.Position.TOP_CENTER);
            } catch (SellAutoApiException ex) {
                if (ex.getMessage().toLowerCase().contains("phone number already exist")) {
                    Notification.show("Номер телефона уже занят!", 5000, Notification.Position.TOP_CENTER);
                    return;
                }

                Notification.show("Произошла ошибка! Попробуйте повторить попытку!", 5000, Notification.Position.TOP_CENTER);
                log.error("SellAutoApiException {}", ex.getMessage());
            }
        }));

        edit.addClickListener(event ->
                setEditMode(edit, approvedEdit, phoneNumber, firstName, lastName));

        return new HorizontalLayout(approvedEdit, edit);
    }

    private void configureEditBinder(TextField phone, TextField firstName, TextField lastName) {

        editProfileBinder.forField(phone)
                .withValidator(new RegexpValidator(
                        "Некорректный формат телефона",
                        "^\\+?[0-9]{10,15}$"
                ))
                .bind(EditProfilePayload::getPhoneNumber, EditProfilePayload::setPhoneNumber);

        editProfileBinder.forField(firstName)
                .withValidator(
                        new StringLengthValidator("Имя должно быть 2-30 символов", 2, 30)
                )
                .bind(EditProfilePayload::getFirstName, EditProfilePayload::setFirstName);

        editProfileBinder.forField(lastName)
                .withValidator(
                        new StringLengthValidator("Фамилия должна быть 2-30 символов", 2, 30)
                )
                .bind(EditProfilePayload::getLastName, EditProfilePayload::setLastName);
    }

    private void setEditMode(Button edit, Button approvedEdit, TextField phoneNumber, TextField firstName, TextField lastName) {
        isEditMode = !isEditMode;
        if (isEditMode) {
            edit.setText("Отмена");
        } else {
            edit.setText("Редактировать");
        }
        approvedEdit.setVisible(isEditMode);
        phoneNumber.setReadOnly(!isEditMode);
        firstName.setReadOnly(!isEditMode);
        lastName.setReadOnly(!isEditMode);
    }

    private void addAdminPanelButton(ProfilePayload profile, HorizontalLayout headerLayout) {
        if (profile.getAccount().getRole() == Role.ROLE_ADMIN) {
            var routerLink = new RouterLink("Панель администратора", MainAdminView.class);
            var routerLayout = new VerticalLayout(routerLink);
            routerLayout.setWidthFull();
            routerLayout.setDefaultHorizontalComponentAlignment(Alignment.END);
            headerLayout.add(routerLayout);
        }
    }
}
