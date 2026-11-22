package com.wagba.users.audit;

import com.wagba.users.common.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    /** MANDATORY: must run inside the caller's transaction, so the change and its audit row commit together. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actorId, UUID targetId, String action,
                       Map<String, Object> changes, String reason) {
        AuditLog entry = new AuditLog();
        entry.setActorUserId(actorId);
        entry.setTargetUserId(targetId);
        entry.setAction(action);
        entry.setChanges(changes);
        entry.setReason(reason);
        // new: the id of the current HTTP request, same one the outbox event gets
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId != null) {
            entry.setCorrelationId(UUID.fromString(correlationId));
        }

        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            entry.setIpAddress(req.getRemoteAddr());
            entry.setUserAgent(req.getHeader("User-Agent"));
        }
        repository.save(entry);
    }
}