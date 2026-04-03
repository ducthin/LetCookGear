package com.ducthin.LetCookGear.repository;

import com.ducthin.LetCookGear.entity.Address;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {

	Optional<Address> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
