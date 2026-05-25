package ayd2.p2b.conference_service_api.unit.common.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.exception.GlobalExceptionHandler;
import ayd2.p2b.conference_service_api.feature.activity.domain.exception.ActivityDomainException;
import ayd2.p2b.conference_service_api.feature.attendance.domain.exception.AttendanceDomainException;
import ayd2.p2b.conference_service_api.feature.congress.domain.exception.CongressDomainException;
import ayd2.p2b.conference_service_api.feature.diploma.domain.exception.DiplomaDomainException;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.exception.EnrollmentDomainException;
import ayd2.p2b.conference_service_api.feature.proposal.domain.exception.ProposalDomainException;
import ayd2.p2b.conference_service_api.feature.reservation.domain.exception.ReservationDomainException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpInputMessage;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    void shouldMapActivityDomainException() {
        ActivityDomainException ex = new ActivityDomainException("startTime must be before endTime");

        ProblemDetail detail = handler.handleActivityDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapReservationDomainException() {
        ReservationDomainException ex = new ReservationDomainException("reservation invalid");

        ProblemDetail detail = handler.handleReservationDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapAttendanceDomainException() {
        AttendanceDomainException ex = new AttendanceDomainException("attendance invalid");

        ProblemDetail detail = handler.handleAttendanceDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapDiplomaDomainException() {
        DiplomaDomainException ex = new DiplomaDomainException("diploma invalid");

        ProblemDetail detail = handler.handleDiplomaDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapProposalDomainException() {
        ProposalDomainException ex = new ProposalDomainException("proposal invalid");

        ProblemDetail detail = handler.handleProposalDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapEnrollmentDomainException() {
        EnrollmentDomainException ex = new EnrollmentDomainException("enrollment invalid");

        ProblemDetail detail = handler.handleEnrollmentDomainException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(detail.getProperties()).containsEntry("code", "domain.invariant_violated");
    }

    @Test
    void shouldMapHttpMessageNotReadableException() {
        HttpInputMessage inputMessage = mock(HttpInputMessage.class);
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("body unreadable", inputMessage);

        ProblemDetail detail = handler.handleMessageNotReadable(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getProperties()).containsEntry("code", "validation.failed");
    }

    @Test
    void shouldMapMethodArgumentTypeMismatchException() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "bad-id",
                UUID.class,
                "id",
                null,
                null
        );

        ProblemDetail detail = handler.handleMethodArgumentTypeMismatch(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getProperties()).containsEntry("code", "validation.failed");
    }

    @Test
    void shouldMapUnexpectedException() {
        ProblemDetail detail = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getProperties()).containsEntry("code", "system.internal_error");
    }
}
