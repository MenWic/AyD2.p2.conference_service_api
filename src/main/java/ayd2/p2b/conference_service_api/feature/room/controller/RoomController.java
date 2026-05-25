package ayd2.p2b.conference_service_api.feature.room.controller;

import ayd2.p2b.conference_service_api.common.response.ApiResponse;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.core.openapi.OpenApiExamples;
import ayd2.p2b.conference_service_api.core.security.AuthenticatedUser;
import ayd2.p2b.conference_service_api.feature.room.application.create.CreateRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.delete.DeleteRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.exception.RoomExceptions;
import ayd2.p2b.conference_service_api.feature.room.application.get.GetRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.list.ListRoomsUseCase;
import ayd2.p2b.conference_service_api.feature.room.application.update.UpdateRoomUseCase;
import ayd2.p2b.conference_service_api.feature.room.dto.internal.RoomRequesterContext;
import ayd2.p2b.conference_service_api.feature.room.dto.request.CreateRoomRequest;
import ayd2.p2b.conference_service_api.feature.room.dto.request.UpdateRoomRequest;
import ayd2.p2b.conference_service_api.feature.room.dto.response.RoomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
@Tag(name = "Rooms", description = "Room management endpoints")
public class RoomController {

    private static final String ROLE_CONGRESS_ADMIN = "ROLE_CONGRESS_ADMIN";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CreateRoomUseCase createRoomUseCase;
    private final ListRoomsUseCase listRoomsUseCase;
    private final GetRoomUseCase getRoomUseCase;
    private final UpdateRoomUseCase updateRoomUseCase;
    private final DeleteRoomUseCase deleteRoomUseCase;

    public RoomController(
            CreateRoomUseCase createRoomUseCase,
            ListRoomsUseCase listRoomsUseCase,
            GetRoomUseCase getRoomUseCase,
            UpdateRoomUseCase updateRoomUseCase,
            DeleteRoomUseCase deleteRoomUseCase
    ) {
        this.createRoomUseCase = createRoomUseCase;
        this.listRoomsUseCase = listRoomsUseCase;
        this.getRoomUseCase = getRoomUseCase;
        this.updateRoomUseCase = updateRoomUseCase;
        this.deleteRoomUseCase = deleteRoomUseCase;
    }

    @PostMapping("/congresses/{id}/rooms")
    @Operation(
            summary = "Create room in congress",
            description = "Creates a room within a congress. Room name must be unique per congress. Capacity is optional but must be greater than zero when present.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Room created"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.VALIDATION_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.TOKEN_INVALID_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.FORBIDDEN_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.NOT_FOUND_ERROR))),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Room name conflict", content = @Content(mediaType = "application/problem+json", examples = @ExampleObject(value = OpenApiExamples.CONFLICT_ERROR)))
            }
    )
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @PathVariable("id") UUID congressId,
            @Valid @RequestBody CreateRoomRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        RoomRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        RoomResponse response = createRoomUseCase.execute(congressId, request, requester);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, URI.create("/rooms/" + response.getId()).toString())
                .body(ApiResponse.of(response, "Room created"));
    }

    @GetMapping("/congresses/{id}/rooms")
    @Operation(
            summary = "List rooms by congress",
            description = "Public paginated list of rooms for a congress.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rooms listed"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Congress not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<PageResponse<RoomResponse>>> listRoomsByCongress(
            @PathVariable("id") UUID congressId,
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = normalizePageable(page, size);
        PageResponse<RoomResponse> response = listRoomsUseCase.execute(congressId, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping("/rooms/{id}")
    @Operation(
            summary = "Get room by id",
            description = "Public room detail endpoint.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<RoomResponse>> getRoom(@PathVariable("id") UUID roomId) {
        RoomResponse response = getRoomUseCase.execute(roomId);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/rooms/{id}")
    @Operation(
            summary = "Update room",
            description = "Updates a room. Requires CONGRESS_ADMIN owner/scoped access.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room updated"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable("id") UUID roomId,
            @Valid @RequestBody UpdateRoomRequest request,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        RoomRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        RoomResponse response = updateRoomUseCase.execute(roomId, request, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Room updated"));
    }

    @DeleteMapping("/rooms/{id}")
    @Operation(
            summary = "Delete room",
            description = "Deletes a room only when no activities are associated. Requires CONGRESS_ADMIN owner/scoped access.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Room deleted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token invalid", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found", content = @Content),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict", content = @Content)
            }
    )
    public ResponseEntity<ApiResponse<RoomResponse>> deleteRoom(
            @PathVariable("id") UUID roomId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            Authentication authentication
    ) {
        RoomRequesterContext requester = requireCongressAdminRequester(authentication, authorization);
        RoomResponse response = deleteRoomUseCase.execute(roomId, requester);
        return ResponseEntity.ok(ApiResponse.of(response, "Room deleted"));
    }

    private RoomRequesterContext requireCongressAdminRequester(Authentication authentication, String authorization) {
        if (authentication == null) {
            throw RoomExceptions.forbidden("Congress admin role is required");
        }

        boolean congressAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLE_CONGRESS_ADMIN::equals);
        if (!congressAdmin) {
            throw RoomExceptions.forbidden("Congress admin role is required");
        }

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw RoomExceptions.forbidden("Authenticated user context is required");
        }
        if (user.getUserId() == null) {
            throw RoomExceptions.forbidden("Authenticated user context is required");
        }

        return RoomRequesterContext.builder()
                .userId(user.getUserId())
                .roles(user.getRoles() == null ? Set.of() : user.getRoles())
                .accessToken(extractBearerToken(authorization))
                .build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw RoomExceptions.forbidden("Bearer token is required");
        }
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) {
            throw RoomExceptions.forbidden("Bearer token is required");
        }
        return token;
    }

    private Pageable normalizePageable(int page, int size) {
        int normalizedPage = Math.max(DEFAULT_PAGE, page);
        int normalizedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(normalizedPage, normalizedSize, Sort.by(Sort.Direction.ASC, "name"));
    }
}
