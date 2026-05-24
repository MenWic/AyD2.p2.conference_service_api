package ayd2.p2b.conference_service_api.integration.exception;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public final class IntegrationExceptions {

    private IntegrationExceptions() {
    }

    public static ApiException iamUnavailable() {
        return iamUnavailable("IAM service is currently unavailable");
    }

    public static ApiException iamUnavailable(String detail) {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "integration.iam_unavailable",
                detail);
    }

    public static ApiException walletUnavailable() {
        return walletUnavailable("Wallet service is currently unavailable");
    }

    public static ApiException walletUnavailable(String detail) {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "system.integration_error",
                detail);
    }
}
