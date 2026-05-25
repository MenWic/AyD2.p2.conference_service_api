package ayd2.p2b.conference_service_api.unit.domain;

import ayd2.p2b.conference_service_api.feature.call.domain.exception.CallDomainException;
import ayd2.p2b.conference_service_api.feature.enrollment.domain.exception.EnrollmentDomainException;
import ayd2.p2b.conference_service_api.feature.proposal.domain.exception.ProposalDomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    @Test
    void call_domain_exception_carries_message() {
        CallDomainException ex = new CallDomainException("call error");
        assertThat(ex.getMessage()).isEqualTo("call error");
    }

    @Test
    void enrollment_domain_exception_carries_message() {
        EnrollmentDomainException ex = new EnrollmentDomainException("enrollment error");
        assertThat(ex.getMessage()).isEqualTo("enrollment error");
    }

    @Test
    void proposal_domain_exception_carries_message() {
        ProposalDomainException ex = new ProposalDomainException("proposal error");
        assertThat(ex.getMessage()).isEqualTo("proposal error");
    }
}
