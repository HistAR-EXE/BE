package com.histar.be.usercreation.repository;

import com.histar.be.usercreation.entity.UserCreation;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCreationRepository extends JpaRepository<UserCreation, UUID> {
}
