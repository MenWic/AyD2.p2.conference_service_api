package ayd2.p2b.conference_service_api.feature.enrollment.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.openapi.OpenApiExamples;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.enrollment.application.exception.EnrollmentExceptions;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListCongressEnrollmentsUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.list.ListUserEnrollmentsUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantResult;
import ayd2.p2b.conference_service_api.feature.enrollment.application.register.EnrollParticipantUseCase;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.internal.EnrollmentRequesterContext;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.request.CreateEnrollmentRequest;
import ayd2.p2b.conference_service_api.feature.enrollment.dto.response.EnrollmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@Tag(name = "Enrollments", description = "Congress enrollment management endpoints")
@RequiredArgsConstructor
public class EnrollmentController {

  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 100;
  private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 120;

  private final EnrollParticipantUseCase enrollParticipantUseCase;
  private final ListUserEnrollmentsUseCase listUserEnrollmentsUseCase;
  private final ListCongressEnrollmentsUseCase listCongressEnrollmentsUseCase;

  @PostMapping("/congresses/{id}/enrollments")
  @Operation(summary = "Enroll participant in congress",
      description = "PARTICIPANT self enrollment endpoint. Requires Idempotency-Key header. Conference derives amount from congress price and orchestrates Wallet payment before persisting enrollment.",
      security = @SecurityRequirement(name = "bearerAuth"), responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Enrolled successfully"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Idempotency replay"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Already enrolled or key conflict", content = @Content),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Insufficient funds or failed key", content = @Content)
  })
  public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
      @PathVariable UUID id,
      @Valid @RequestBody CreateEnrollmentRequest request,
      @Parameter(description = "Client/BFF-generated idempotency key. Max 120 chars. Reused by conference-service when invoking wallet-service.",
          required = true,
          example = "74ee4fce-8c03-4b18-8efc-3b53a4897fbe")
      @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      Authentication authentication) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw EnrollmentExceptions.validationFailed("Idempotency-Key header is required and must not be blank");
    }
    String normalizedKey = idempotencyKey.trim();
    if (normalizedKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
      throw EnrollmentExceptions.validationFailed("Idempotency-Key must not exceed 120 characters");
    }

    EnrollmentRequesterContext requester = buildRequesterContext(authentication, authorization);

    EnrollParticipantResult result = enrollParticipantUseCase.execute(id, request, normalizedKey, requester);

    if (result.isReplay()) {
      return ResponseEntity.ok(ApiResponse.of(result.getEnrollment(), "idempotency.replay"));
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.LOCATION,
            URI.create("/congresses/" + id + "/enrollments/" + result.getEnrollment().getId()).toString())
        .body(ApiResponse.of(result.getEnrollment(), "Enrolled successfully"));
  }

  @GetMapping("/congresses/{id}/enrollments")
  @Operation(summary = "List enrollments for a congress",
      description = "Returns paginated enrollments for a congress. Requires CONGRESS_ADMIN owner/scoped access.",
      security = @SecurityRequirement(name = "bearerAuth"), responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments listed"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
  })
  public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> listCongressEnrollments(
      @PathVariable UUID id,
      @Parameter(description = "Zero-based page index", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size (max 100)", example = "20")
      @RequestParam(defaultValue = "20") int size,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      Authentication authentication) {
    EnrollmentRequesterContext requester = buildRequesterContext(authentication, authorization);
    Pageable pageable = normalizePageable(page, size);
    PageResponse<EnrollmentResponse> response = listCongressEnrollmentsUseCase.execute(id, pageable, requester);
    return ResponseEntity.ok(ApiResponse.of(response));
  }

  @GetMapping("/users/{id}/enrollments")
  @Operation(summary = "List enrollments for a user",
      description = "Returns paginated enrollments for a user. Self-only endpoint for PARTICIPANT.",
      security = @SecurityRequirement(name = "bearerAuth"), responses = {
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollments listed"),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
  })
  public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> listUserEnrollments(
      @PathVariable UUID id,
      @Parameter(description = "Zero-based page index", example = "0")
      @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Page size (max 100)", example = "20")
      @RequestParam(defaultValue = "20") int size,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      Authentication authentication) {
    EnrollmentRequesterContext requester = buildRequesterContext(authentication, authorization);
    Pageable pageable = normalizePageable(page, size);
    PageResponse<EnrollmentResponse> response = listUserEnrollmentsUseCase.execute(id, pageable, requester);
    return ResponseEntity.ok(ApiResponse.of(response));
  }

  private EnrollmentRequesterContext buildRequesterContext(Authentication authentication, String authorization) {
    if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      throw EnrollmentExceptions.forbidden("Authentication is required");
    }
    String token = extractBearerToken(authorization);
    Set<Role> roles = user.getRoles() == null ? Set.of() : user.getRoles();
    return EnrollmentRequesterContext.builder()
        .userId(user.getUserId())
        .roles(roles)
        .accessToken(token)
        .build();
  }

  private String extractBearerToken(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw EnrollmentExceptions.forbidden("Bearer token is required");
    }
    String token = authorization.substring(7).trim();
    if (token.isEmpty()) {
      throw EnrollmentExceptions.forbidden("Bearer token is required");
    }
    return token;
  }

  private Pageable normalizePageable(int page, int size) {
    int normalizedPage = Math.max(DEFAULT_PAGE, page);
    int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    return PageRequest.of(normalizedPage, normalizedSize, Sort.by("enrolledAt").descending());
  }
}
