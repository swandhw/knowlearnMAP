package com.knowlearnmap.member.repository;

import com.knowlearnmap.member.domain.UpgradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UpgradeRequestRepository extends JpaRepository<UpgradeRequest, Long> {
    List<UpgradeRequest> findByMemberId(Long memberId);

    List<UpgradeRequest> findByStatus(UpgradeRequest.RequestStatus status);
}
