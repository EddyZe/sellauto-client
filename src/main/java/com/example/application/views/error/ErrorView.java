package com.example.application.views.error;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.*;
import jakarta.servlet.http.HttpServletResponse;



@Route("error")
@PageTitle("Ошибка")
public class ErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    private final TextArea errorMessage;

    public ErrorView() {
        setSizeFull();
        errorMessage = new TextArea();
        errorMessage.setSizeFull();
        errorMessage.setReadOnly(true);
        add(errorMessage);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        errorMessage.setValue(parameter.getCustomMessage());
        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
