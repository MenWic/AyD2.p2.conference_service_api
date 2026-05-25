package ayd2.p2b.conference_service_api.core.openapi;

public final class OpenApiExamples {

    private OpenApiExamples() {
    }

    public static final String VALIDATION_ERROR = """
            {"status":400,"detail":"Validation failed","code":"validation.failed"}
            """;
    public static final String TOKEN_INVALID_ERROR = """
            {"status":401,"detail":"Token invalid","code":"auth.token_invalid"}
            """;
    public static final String FORBIDDEN_ERROR = """
            {"status":403,"detail":"Forbidden","code":"auth.forbidden"}
            """;
    public static final String NOT_FOUND_ERROR = """
            {"status":404,"detail":"Resource not found","code":"resource.not_found"}
            """;
    public static final String CONFLICT_ERROR = """
            {"status":409,"detail":"Conflict","code":"resource.conflict"}
            """;
    public static final String DOMAIN_INVARIANT_ERROR = """
            {"status":422,"detail":"Domain invariant violated","code":"domain.invariant_violated"}
            """;
    public static final String IAM_UNAVAILABLE_ERROR = """
            {"status":503,"detail":"IAM unavailable","code":"integration.iam_unavailable"}
            """;
    public static final String WALLET_UNAVAILABLE_ERROR = """
            {"status":503,"detail":"Wallet unavailable","code":"integration.wallet_unavailable"}
            """;
    public static final String WALLET_INSUFFICIENT_FUNDS_ERROR = """
            {"status":422,"detail":"Insufficient funds","code":"wallet.insufficient_funds"}
            """;
    public static final String INTERNAL_ERROR = """
            {"status":500,"detail":"Unexpected error","code":"system.internal_error"}
            """;

    public static final String INSTITUTION_REQUEST = """
            {"name":"Universidad de San Carlos","description":"Institucion publica","contactEmail":"contacto@usac.edu.gt"}
            """;
    public static final String INSTITUTION_SUCCESS = """
            {"data":{"id":"c9e3e2d8-b3fb-4749-925d-02e15c8a5c07","name":"Universidad de San Carlos","description":"Institucion publica","contactEmail":"contacto@usac.edu.gt","active":true}}
            """;
    public static final String CONGRESS_REQUEST = """
            {"name":"Congreso Nacional de Ingenieria","description":"Evento academico anual","startDate":"2026-09-10","endDate":"2026-09-12","location":"Ciudad de Guatemala","price":85.00,"institutionId":"d2719de1-0409-4d2e-bf9b-a06f0ea74df7"}
            """;
    public static final String CONGRESS_SUCCESS = """
            {"data":{"id":"7d899e63-481d-4df8-87f1-7a8d8f437b68","name":"Congreso Nacional de Ingenieria","price":85.00}}
            """;
    public static final String ROOM_REQUEST = """
            {"name":"Sala Magna","capacity":120,"location":"Edificio A, nivel 2"}
            """;
    public static final String ROOM_SUCCESS = """
            {"data":{"id":"57bd53d0-60a9-4b5f-a52a-7de4013676bf","name":"Sala Magna","capacity":120}}
            """;
    public static final String ACTIVITY_REQUEST = """
            {"name":"Taller de Cloud Native","description":"Sesion practica","roomId":"f39f1f7f-e2d2-4f2e-8cb8-95630f1a9f8e","type":"TALLER","startTime":"2026-10-10T10:00:00Z","endTime":"2026-10-10T12:00:00Z","workshopCapacity":30,"leaderIds":["d0952fd5-4eb8-467b-93a2-3ee586d0e20b"]}
            """;
    public static final String ACTIVITY_SUCCESS = """
            {"data":{"id":"3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2","name":"Taller de Cloud Native","type":"TALLER","workshopCapacity":30}}
            """;
    public static final String CALL_SUCCESS = """
            {"data":{"id":"f9b5c6f2-88d4-4ef2-a3cf-2728d1f7fe7f","congressId":"9470bdea-8e37-4d0e-b2ae-545211ec4498","status":"OPEN"}}
            """;
    public static final String PROPOSAL_REQUEST = """
            {"title":"Arquitectura basada en eventos","description":"Trabajo sobre microservicios","type":"PONENCIA"}
            """;
    public static final String PROPOSAL_SUCCESS = """
            {"data":{"id":"f9b5c6f2-88d4-4ef2-a3cf-2728d1f7fe7f","status":"PENDING","type":"PONENCIA"}}
            """;
    public static final String COMMITTEE_REQUEST = """
            {"userId":"18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8"}
            """;
    public static final String COMMITTEE_SUCCESS = """
            {"data":{"congressId":"9470bdea-8e37-4d0e-b2ae-545211ec4498","userId":"18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8","fullName":"Ana Maria Lopez","email":"ana.lopez@example.com"}}
            """;
    public static final String ENROLLMENT_REQUEST = """
            {"paymentDate":"2026-10-10"}
            """;
    public static final String ENROLLMENT_SUCCESS = """
            {"data":{"id":"4c85767c-2739-46eb-8d35-5f639f8d38e2","congressId":"9470bdea-8e37-4d0e-b2ae-545211ec4498","userId":"18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8","paymentId":"f2b071d7-8179-41f8-92f4-e57fa4ef3b00","paymentDate":"2026-10-10"}}
            """;
    public static final String RESERVATION_SUCCESS = """
            {"data":{"id":"8ac57b86-1e92-4c2d-a10f-0af7ef3d6cc2","activityId":"3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2","userId":"18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8"}}
            """;
    public static final String ATTENDANCE_REQUEST = """
            {"activityId":"3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2","personalId":"A1234567"}
            """;
    public static final String ATTENDANCE_SUCCESS = """
            {"data":{"id":"998b9de4-8477-4c6f-9ce1-f2ce62da6293","activityId":"3a6d8f8e-a077-4a66-a7d3-c8dd90666fc2","personalId":"A1234567","registeredBy":"f8c9e44d-b953-4d7c-a894-e94f0a66c7f2"}}
            """;
    public static final String DIPLOMA_SUCCESS = """
            {"data":{"id":"d65a773a-d57e-41e8-8782-02f2f5aaf0a2","userId":"18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8","congressId":"9470bdea-8e37-4d0e-b2ae-545211ec4498","type":"PARTICIPATION","activityId":null,"available":true}}
            """;
    public static final String DIPLOMA_PRINT_DATA_SUCCESS = """
            {"data":{"diplomaId":"d65a773a-d57e-41e8-8782-02f2f5aaf0a2","userId":"18d7f0d1-20cf-47b0-a3eb-d8a292e3f3d8","userFullName":"Ana Maria Lopez","congressId":"9470bdea-8e37-4d0e-b2ae-545211ec4498","congressName":"Congreso Nacional de Ingenieria","activityId":null,"activityName":null,"type":"PARTICIPATION","issuedAt":"2026-10-10T10:00:00Z"}}
            """;
    public static final String PAGE_RESPONSE_SUCCESS = """
            {"data":{"items":[],"page":0,"size":20,"totalItems":0,"totalPages":0}}
            """;
}
