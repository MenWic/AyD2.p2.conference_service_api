package ayd2.p2b.conference_service_api.integration.port;

import ayd2.p2b.conference_service_api.integration.dto.WalletPaymentRegisterRequest;

import java.util.UUID;

public interface WalletPaymentPort {

  /**
   * Registers a payment in wallet-service.
   * Returns the created payment ID.
   * Throws ApiException on: insufficient funds (422), unavailable (503), conflict
   * (409).
   */
  UUID registerPayment(WalletPaymentRegisterRequest request, String idempotencyKey, String accessToken);
}
