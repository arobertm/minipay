package ro.minipay.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.minipay.audit.model.AuditEntry;

import java.util.List;
import java.util.Optional;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    Optional<AuditEntry> findTopByOrderBySequenceNumberDesc();

    List<AuditEntry> findAllByOrderBySequenceNumberAsc();

    Page<AuditEntry> findAllByOrderBySequenceNumberDesc(Pageable pageable);

    Optional<AuditEntry> findByTxnId(String txnId);
}
