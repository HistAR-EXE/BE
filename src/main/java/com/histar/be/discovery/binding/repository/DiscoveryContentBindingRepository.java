package com.histar.be.discovery.binding.repository;

import com.histar.be.discovery.binding.entity.DiscoveryContentBinding;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiscoveryContentBindingRepository extends JpaRepository<DiscoveryContentBinding, UUID> {

    List<DiscoveryContentBinding> findByLocationIdOrderBySortOrder(UUID locationId);
}
