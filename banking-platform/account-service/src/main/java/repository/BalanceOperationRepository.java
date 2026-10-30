package repository;

import domain.BalanceOperation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceOperationRepository extends JpaRepository<BalanceOperation, UUID> {}