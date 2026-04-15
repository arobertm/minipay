package ro.minipay.issuer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.minipay.issuer.model.CardAccount;

import java.util.Optional;

public interface CardAccountRepository extends JpaRepository<CardAccount, Long> {
    Optional<CardAccount> findByPan(String pan);
}
