package ayd2.p2b.conference_service_api.unit.common.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.exception.GlobalExceptionHandler;
import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressDomainException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapApiExceptionWithCode() {
        ApiException ex = new ApiException(HttpStatus.CONFLICT, "resource.conflict", "conflict");

        ProblemDetail detail = handler.handleApiException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getProperties()).containsEntry("code", "resource.conflict");
    }

    @Test
    void shouldMapMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("createCongressRequest", "name", "name is required")
        ));

        ProblemDetail detail = handler.handleValidation(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getProperties()).containsEntry("code", "validation.failed");
        assertThat(detail.getProperties().get("errors")).isEqualTo(Map.of("name", "name is required"));
    }

    @Test
    void shouldMapConstraintViolationException() {
        ConstraintViolationException ex = new ConstraintViolationException(Set.of());

        ProblemDetail detail = handler.handleConstraintViolation(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getProperties()).containsEntry("code", "validation.failed");
    }

    @Test
    void shouldMapCongressDomainException() {
        CongressDomainException ex = new CongressDomainException("price must be >= 35.00");

        ProblemDetail detail = handler.handleCongressDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getDetail()).isEqualTo("price must be >= 35.00");
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapUnexpectedException() {
        ProblemDetail detail = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getProperties()).containsEntry("code", "system.internal_error");
    }
}
