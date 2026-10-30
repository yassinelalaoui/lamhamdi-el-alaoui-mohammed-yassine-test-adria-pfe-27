package repository;

import domain.Account;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findAllByCustomerReferenceOrderByIdAsc(String customerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") UUID id);
}