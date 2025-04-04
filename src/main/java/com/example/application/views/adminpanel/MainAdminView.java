package com.example.application.views.adminpanel;


import com.example.application.clients.sellauto.client.SellAutoRestClient;
import com.example.application.clients.sellauto.payloads.*;
import com.example.application.exceptions.SellAutoApiException;
import com.example.application.views.MainLayout;
import com.example.application.views.profile.UserProfileView;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;


@Slf4j
@Route(value = "/admin", layout = MainLayout.class)
@PageTitle("Админ панель")
public class MainAdminView extends HorizontalLayout {

    private final String downloadDir;

    private final SellAutoRestClient sellAutoRestClient;

    private final VerticalLayout colorsView;
    private final VerticalLayout brandsView;

    private final VerticalLayout usersView;
    private final VerticalLayout backupView;

    public MainAdminView(@Value("${sell-auto.data.backup}") String downloadDir, SellAutoRestClient sellAutoRestClient) {
        this.downloadDir = downloadDir;
        this.sellAutoRestClient = sellAutoRestClient;

        setSizeFull();

        var firstBlock = new VerticalLayout();
        firstBlock.setWidth("45%");
        firstBlock.setHeight("1300px");
        firstBlock.setAlignItems(Alignment.CENTER);

        var secondBlock = new VerticalLayout();
        secondBlock.setWidth("45%");
        secondBlock.setHeight("1300px");
        secondBlock.setAlignItems(Alignment.CENTER);
        colorsView = new VerticalLayout();
        brandsView = new VerticalLayout();
        usersView = new VerticalLayout();
        backupView = new VerticalLayout();

        try {
            this.sellAutoRestClient.getAdminBrands();

            colorsView.setSizeFull();
            colorsView.setClassName("custom-block");

            brandsView.setClassName("custom-block");
            brandsView.setSizeFull();

            firstBlock.add(new H1("Цвета авто"), colorsView);
            firstBlock.add(new H1("Бренды авто"), brandsView);

            createColorBlock();
            createBrandsBlock();

            usersView.setClassName("custom-block");
            usersView.setSizeFull();

            backupView.setClassName("custom-block");
            backupView.setSizeFull();

            secondBlock.add(new H1("Пользователи"), usersView, new H1("Бэкапы"), backupView);
            createUserList();
            createBackUpBlock();

            add(firstBlock, secondBlock);
        } catch (SellAutoApiException e) {
            log.warn(e.getMessage());
        }
    }

    private void createBackUpBlock() {
        Path backupDir = Path.of(downloadDir);

        Select<Path> backupSelect = new Select<>();
        backupSelect.setLabel("Загруженные бэкапы");
        var createBackUp = createDownloadBackupButton(backupDir, backupSelect);
        backupSelect.setWidthFull();
        backupSelect.setEmptySelectionAllowed(true);
        backupSelect.setEmptySelectionCaption("Выберите бэкап");
        backupSelect.setRenderer(new ComponentRenderer<Component, Path>(
                path -> new Span(path.toFile().getName())

        ));

        var removeBtn = createRemoveBackupButton(backupSelect, backupDir);
        removeBtn.setWidth("48%");
        createBackUp.setWidth("48%");

        refreshBackupList(backupDir, backupSelect);
        var btns = new HorizontalLayout(createBackUp, removeBtn);
        btns.setWidthFull();

        var downloadUrl = new Anchor();
        downloadUrl.setVisible(false);

        backupSelect.addValueChangeListener(createChangeBackupListener(downloadUrl));

        Upload upload = createUploadField();

        backupView.add(
                backupSelect,
                btns,
                downloadUrl,
                new Span("Загруженный файл с бэкапом будет применен"),
                upload
        );
    }

    private Upload createUploadField() {
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setMaxFiles(1);
        upload.setDropLabelIcon(VaadinIcon.UPLOAD.create());
        upload.setUploadButton(new Button("Upload zip..."));
        upload.setAcceptedFileTypes("application/zip", ".zip");
        upload.setWidthFull();

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            InputStream inputStream = buffer.getInputStream(fileName);
            Path tempFile = Paths.get(downloadDir, fileName);
            try (BufferedInputStream bis = new BufferedInputStream(inputStream);
                 BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tempFile.toFile()))) {
                bos.write(bis.readAllBytes());
            } catch (IOException e) {
                log.error("error writing  file", e);
            }

            try {
                sellAutoRestClient.uploadBackup(new FileSystemResource(tempFile));
                UI.getCurrent().getPage().reload();
                Notification.show("Бэкап успешно установлен!", 5000, Notification.Position.TOP_CENTER);
            } catch (SellAutoApiException e) {
                Notification.show("Ошибка загрузки файла на сервер!");
                log.error(e.getMessage());
            }
        });
        return upload;
    }

    private HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<Select<Path>, Path>> createChangeBackupListener(Anchor downloadUrl) {
        return e -> {
            var currentPath = e.getValue();

            if (currentPath == null || Files.notExists(currentPath)) {
                downloadUrl.setVisible(false);
                return;
            }

            StreamResource resource = new StreamResource(
                    currentPath.toFile().getName(),
                    (InputStreamFactory) () -> {
                        try {
                            return new BufferedInputStream(Files.newInputStream(currentPath));
                        } catch (IOException ex) {
                            log.error("error creating downloadUrl", ex);
                            return null;
                        }
                    });

            downloadUrl.setVisible(true);
            downloadUrl.setText("Скачать: %s".formatted(currentPath.toFile().getName()));
            downloadUrl.setHref(resource);
        };
    }

    private Button createRemoveBackupButton(Select<Path> backupSelect, Path backupDir) {
        return new Button("Удалить", click -> {
            if (backupSelect.getValue() == null) {
                Notification.show("Выберите бэкап", 5000, Notification.Position.TOP_CENTER);
                return;
            }
            try {
                Files.delete(backupSelect.getValue());
                refreshBackupList(backupDir, backupSelect);
            } catch (IOException ex) {
                Notification.show("Ошибка при удалении бэкапа", 5000, Notification.Position.TOP_CENTER);
                log.error("error delete files: ", ex);
            }
        });
    }

    private void refreshBackupList(Path backupDir, Select<Path> backupSelect) {
        if (Files.exists(backupDir)) {
            try (var backups = Files.walk(backupDir).filter(Files::isRegularFile)
                    .sorted((o1, o2) ->
                            o2.getFileName().toString()
                                    .compareTo(o1.getFileName().toString()))) {
                backupSelect.setItems(backups.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Button createDownloadBackupButton(Path backupDir, Select<Path> backupSelect) {
        return new Button("Создать бэкап", e -> {
            try {
                var res = sellAutoRestClient.downloadBackup();
                Path downDir = Path.of(downloadDir);

                createDowndir(downDir);

                Path backup = Paths.get(downloadDir, res.getFilename());

                downloadBackup(res, backup);
                refreshBackupList(backupDir, backupSelect);
            } catch (SellAutoApiException ex) {
                log.error("error download backup", ex);
                Notification.show("Ошибка создания бэкапа", 5000, Notification.Position.TOP_CENTER);
            }
        });
    }

    private void createDowndir(Path downDir) {
        if (Files.notExists(downDir)) {
            try {
                Files.createDirectory(downDir);
            } catch (IOException ex) {
                log.error("Error creating download dir", ex);
                Notification.show("Ошибка при создании директории для бэкапа, попробуйте повторить попытку", 5000,
                        Notification.Position.TOP_CENTER);
            }
        }
    }

    private void downloadBackup(Resource res, Path backup) {
        try (BufferedInputStream bis = new BufferedInputStream(res.getInputStream());
             BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(backup))) {

            byte[] buffer = new byte[1024];
            int read;

            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }

            Notification.show("Бэкап создан", 5000, Notification.Position.TOP_CENTER);
        } catch (IOException ex) {
            log.error("error download backup", ex);
        }
    }

    private void createBrandsBlock() {
        try {
            createSelectBlock();
        } catch (Exception e) {
            log.error("Error get brand {}", e.getMessage());
        }
    }

    private void createUserList() {
        try {
            var currentUser = sellAutoRestClient.getProfile();
            VirtualList<ProfilePayload> profiles = new VirtualList<>();
            profiles.setItems(sellAutoRestClient.getProfiles().getProfiles());
            profiles.setRenderer(new ComponentRenderer<>(profile -> {
                var userLayout = new HorizontalLayout();
                userLayout.setWidth("95%");
                userLayout.setClassName("custom-card");
                if (profile.getAccount() != null) {
                    var link = new RouterLink(profile.getAccount().getEmail(), UserProfileView.class,
                            profile.getUserId().toString());
                    var banButton = createBanButton(profile, currentUser);

                    var linkLay = new HorizontalLayout(link);
                    linkLay.setWidthFull();

                    userLayout.add(linkLay, banButton);
                } else
                    userLayout.add(new Span("Неизвестный пользователь"));

                return userLayout;
            }));
            usersView.add(profiles);
        } catch (SellAutoApiException e) {
            log.error("error get users {} ", e.getMessage());
        }
    }

    private Button createBanButton(ProfilePayload profile, ProfilePayload currentUser) {
        var btn = new Button(profile.getAccount().isBlocked() ? "Разблокировать" : "Заблокировать");
        btn.addClickListener(e -> {
            try {
                if (profile.getUserId().equals(currentUser.getUserId())) {
                    Notification.show("Нельзя заблокировать/разблокировать себя!", 5000, Notification.Position.TOP_CENTER);
                    return;
                }
                if (profile.getAccount().isBlocked()) {
                    profile.getAccount().setBlocked(false);
                    sellAutoRestClient.unBanAccount(profile.getAccount().getAccountId());
                    Notification.show("Пользователь разблокирован", 5000, Notification.Position.TOP_CENTER);
                    btn.setText("Заблокировать");
                    return;
                }

                sellAutoRestClient.banAccount(profile.getAccount().getAccountId());
                profile.getAccount().setBlocked(true);
                Notification.show("Пользователь заблокирован", 5000, Notification.Position.TOP_CENTER);
                btn.setText("Разблокировать");
            } catch (SellAutoApiException ex) {
                log.error("Error banned/unban user", ex);
                Notification.show("Произошла ошибка...", 5000, Notification.Position.TOP_CENTER);
            }

        });

        return btn;
    }

    private void createSelectBlock() {
        var brands = sellAutoRestClient.getAdminBrands()
                .getBrands();
        var brandSelect = new Select<BrandDetailPayload>();
        brandSelect.setWidthFull();
        var models = new Select<ModelBasePayload>();
        models.setWidthFull();
        var selectingLayout = new HorizontalLayout(brandSelect, models);
        selectingLayout.setWidthFull();

        brandSelect.setEmptySelectionAllowed(true);
        brandSelect.setEmptySelectionCaption("Выберете бренд");
        brandSelect.setItems(brands);
        brandSelect.setRenderer(new ComponentRenderer<>(brandDetailPayload ->
                new Span(brandDetailPayload.getTitle())
        ));

        models.setEmptySelectionCaption("Модель");
        models.setEmptySelectionAllowed(true);

        brandSelect.addValueChangeListener(createChangeListenerModel(brandSelect, models));

        var deleteBrand = createDeleteButtonBrand(brandSelect, models);
        var deleteModel = createDeleteButtonModel(models, brandSelect);

        var addBrandField = new TextField();
        addBrandField.setWidthFull();
        addBrandField.setPlaceholder("Введите название бренда");

        var addBrandButton = createAddBrandButton(addBrandField, brandSelect);

        var buttonLayout = new HorizontalLayout(deleteBrand, deleteModel);
        buttonLayout.setWidthFull();

        var addBrandLayout = new HorizontalLayout(addBrandField, addBrandButton);
        addBrandLayout.setWidthFull();

        var addModelField = new TextField();
        addModelField.setWidthFull();
        addModelField.setPlaceholder("Введите название модели");

        var addModelButton = createAddButtonModel(addModelField, brandSelect, models);
        var addModelLayout = new HorizontalLayout(addModelField, addModelButton);
        addModelLayout.setWidthFull();

        var titleCreatingBrand = new H3("Создание бренда");
        titleCreatingBrand.setWidthFull();

        var titleCreatingModel = new H3("Создание модели");
        titleCreatingModel.setWidthFull();
        brandsView.add(
                selectingLayout,
                buttonLayout,
                titleCreatingBrand,
                addBrandLayout,
                titleCreatingModel,
                addModelLayout);
    }

    private Button createAddButtonModel(TextField addModelField, Select<BrandDetailPayload> brandSelect, Select<ModelBasePayload> models) {
        return new Button("Создать", e -> {
            if (addModelField.getValue() == null || addModelField.getValue().isEmpty()) {
                Notification.show("Введите название модели!", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            if (brandSelect.getValue() == null) {
                Notification.show("Выберите бренд, к которому хотите добавить модель!", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            try {
                sellAutoRestClient.createModel(ModelBasePayload.builder()
                        .title(addModelField.getValue())
                        .build(), brandSelect.getValue().getTitle());
                addModelField.setValue("");
                Notification.show("Модель создана", 5000, Notification.Position.TOP_CENTER);
                models.setItems(sellAutoRestClient.getModelsFromBrandTitle(
                        brandSelect.getValue().getTitle()).getModels()
                );
            } catch (SellAutoApiException ex) {
                if (ex.getMessage().contains("already exists")) {
                    Notification.show("Такая модель уже существует!", 5000, Notification.Position.TOP_CENTER);
                }
            }
        });
    }

    private Button createAddBrandButton(TextField addBrandField, Select<BrandDetailPayload> brandSelect) {
        return new Button("Создать", e -> {
            if (addBrandField.getValue() == null || addBrandField.getValue().isEmpty()) {
                Notification.show("Введите название бренда!", 5000, Notification.Position.TOP_CENTER);
                return;
            }

            try {
                sellAutoRestClient.createBrand(BrandBasePayload.builder()
                        .title(addBrandField.getValue())
                        .build());
                addBrandField.setValue("");

                Notification.show("Бренд создан", 5000, Notification.Position.TOP_CENTER);
                brandSelect.setItems(sellAutoRestClient.getAdminBrands().getBrands());
            } catch (SellAutoApiException ex) {
                if (ex.getMessage().contains("already exists")) {
                    Notification.show("Такой бренд уже существует!", 5000, Notification.Position.TOP_CENTER);
                }
            }
        });
    }

    private HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<Select<BrandDetailPayload>, BrandDetailPayload>> createChangeListenerModel(Select<BrandDetailPayload> brandSelect, Select<ModelBasePayload> models) {
        return event -> {
            if (brandSelect.getValue() != null) {
                models.setItems(event.getValue().getModel());
                models.setRenderer(new ComponentRenderer<>(model ->
                        new Span(model.getTitle())));
            }
        };
    }

    private Button createDeleteButtonModel(Select<ModelBasePayload> models, Select<BrandDetailPayload> brandSelect) {
        var deleteModel = new Button("Удалить модель", clickEvent -> {
            if (models.getValue() != null) {
                try {
                    sellAutoRestClient.deleteModel(models.getValue().getModelId());
                    models.setItems(sellAutoRestClient
                            .getModelsFromBrandTitle(brandSelect.getValue().getTitle())
                            .getModels());
                    Notification.show("Модель удалена!", 5000, Notification.Position.TOP_CENTER);
                } catch (SellAutoApiException e) {
                    Notification.show("Ошибка удаления модели!", 5000, Notification.Position.TOP_CENTER);
                }
            } else {
                Notification.show("Выберете модель, которую хотите удалить!", 5000, Notification.Position.TOP_CENTER);
            }
        });
        deleteModel.setWidth("48%");
        return deleteModel;
    }

    private Button createDeleteButtonBrand(Select<BrandDetailPayload> brandSelect, Select<ModelBasePayload> models) {
        var deleteBrand = new Button("Удалить бренд", (e) -> {
            if (brandSelect.getValue() != null) {
                try {
                    sellAutoRestClient.deleteBrand(brandSelect.getValue().getBrandId());
                    brandSelect.setItems(sellAutoRestClient.getAdminBrands().getBrands());
                    models.setItems(new ArrayList<>());
                    Notification.show("Бренд удален", 5000, Notification.Position.TOP_CENTER);
                } catch (SellAutoApiException ex) {
                    log.error(ex.getMessage(), ex);
                    Notification.show("Ошибка удаления бренда!", 5000, Notification.Position.TOP_CENTER);
                }
            } else {
                Notification.show("Выберите бренд, который хотите удалить.", 5000, Notification.Position.TOP_CENTER);
            }
        });
        deleteBrand.setWidth("48%");
        return deleteBrand;
    }

    private void createColorBlock() {
        try {
            var colorsList = new VirtualList<ColorBasePayload>();
            colorsList.setSizeFull();
            colorsList.setItems(sellAutoRestClient.getAdminColors().getColors());
            colorsList.setRenderer(new ComponentRenderer<>(color -> {
                var ver = new HorizontalLayout();
                ver.setClassName("custom-card");
                var title = new TextField();
                title.setValue(color.getTitle());
                title.setReadOnly(true);
                title.setWidthFull();
                ver.add(title);
                ver.add(new Button("Удалить", e -> {
                    try {
                        sellAutoRestClient.deleteColor(color.getColorId());
                        colorsList.setItems(sellAutoRestClient.getAdminColors().getColors());
                        Notification.show("Цвет удален", 5000, Notification.Position.TOP_CENTER);
                    } catch (SellAutoApiException ex) {
                        Notification.show("Ошибка удаления.", 5000, Notification.Position.TOP_CENTER);
                    }
                }));
                ver.setWidth("95%");
                return ver;
            }));

            var form = createFormCreatingForm(colorsList);

            colorsView.add(colorsList, form);
        } catch (SellAutoApiException e) {
            log.warn("error get colors: {}", e.getMessage());
        }
    }

    private FormLayout createFormCreatingForm(VirtualList<ColorBasePayload> colorsList) {
        var titleColor = new TextField();
        titleColor.setWidthFull();
        titleColor.setPlaceholder("Введите название цвета...");
        var btn = new Button("Добавить", e -> {
            try {
                sellAutoRestClient.createColor(ColorBasePayload.builder()
                        .title(titleColor.getValue())
                        .build());
                colorsList.setItems(sellAutoRestClient.getAdminColors().getColors());
                titleColor.setValue("");
                Notification.show("Цвет создан", 5000, Notification.Position.TOP_CENTER);
            } catch (SellAutoApiException ex) {
                Notification.show("Ошибка при добавлении цвета! Возможно такой цвет уже существует!", 5000, Notification.Position.TOP_CENTER);
            }
        });

        var form = new FormLayout(titleColor, btn);
        form.setColspan(titleColor, 1);
        form.setColspan(btn, 1);
        form.setWidthFull();
        return form;
    }
}
