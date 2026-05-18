package ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressDeletionGuardPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JpaCongressDeletionGuardAdapter implements CongressDeletionGuardPort {

    private final EntityManager entityManager;

    public JpaCongressDeletionGuardAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<String> findBlockingDependencies(UUID congressId) {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("rooms", "rooms");
        checks.put("activities", "activities");
        checks.put("calls", "calls");
        checks.put("committee_members", "committee_members");
        checks.put("enrollments", "enrollments");

        List<String> blockers = new ArrayList<>();
        for (Map.Entry<String, String> check : checks.entrySet()) {
            if (existsByCongressId(check.getKey(), congressId)) {
                blockers.add(check.getValue());
            }
        }
        return blockers;
    }

    private boolean existsByCongressId(String tableName, UUID congressId) {
        String sql = "select exists(select 1 from " + tableName + " t where t.congress_id = :congressId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("congressId", congressId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
