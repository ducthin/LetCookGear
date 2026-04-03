package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findTopByOrderIdOrderByCreatedAtAsc(Long orderId);

    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findTopByTransactionRefOrderByCreatedAtDesc(String transactionRef);
}
