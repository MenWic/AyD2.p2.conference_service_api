package ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query;

import ayd2.p2b.conference_service_api.feature.report.application.attendance_summary.port.AttendanceByActivityQueryPort;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class JpaAttendanceByActivityQuery implements AttendanceByActivityQueryPort {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<AttendanceActivityItem> query(UUID congressId, UUID activityIdFilter, UUID roomIdFilter,
                                              OffsetDateTime dateFrom, OffsetDateTime dateTo) {
        StringBuilder jpql = new StringBuilder("""
                select a.id, a.name, r.name, a.startTime, a.endTime, count(att.id)
                from ActivityEntity a
                join a.room r
                left join AttendanceEntity att on att.activityId = a.id
                where a.congressId = :congressId
                """);

        if (activityIdFilter != null) {
            jpql.append(" and a.id = :activityId");
        }
        if (roomIdFilter != null) {
            jpql.append(" and a.roomId = :roomId");
        }
        if (dateFrom != null) {
            jpql.append(" and a.startTime >= :dateFrom");
        }
        if (dateTo != null) {
            jpql.append(" and a.startTime < :dateTo");
        }

        jpql.append("""
                 group by a.id, a.name, r.name, a.startTime, a.endTime
                 order by a.startTime asc, a.name asc
                """);

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("congressId", congressId);

        if (activityIdFilter != null) {
            query.setParameter("activityId", activityIdFilter);
        }
        if (roomIdFilter != null) {
            query.setParameter("roomId", roomIdFilter);
        }
        if (dateFrom != null) {
            query.setParameter("dateFrom", dateFrom);
        }
        if (dateTo != null) {
            query.setParameter("dateTo", dateTo);
        }

        return query.getResultList().stream()
                .map(row -> AttendanceActivityItem.builder()
                        .activityId((UUID) row[0])
                        .activityName((String) row[1])
                        .roomName((String) row[2])
                        .startTime((OffsetDateTime) row[3])
                        .endTime((OffsetDateTime) row[4])
                        .attendanceCount((Long) row[5])
                        .build())
                .toList();
    }
}
