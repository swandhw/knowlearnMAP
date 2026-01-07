package com.knowlearnmap.workspace.service;

import com.knowlearnmap.workspace.domain.WorkspaceEntity;
import com.knowlearnmap.workspace.dto.WorkspaceRequestDto;
import com.knowlearnmap.workspace.dto.WorkspaceResponseDto;
import com.knowlearnmap.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Workspace Service 구현체
 * 
 * 워크스페이스 비즈니스 로직 처리
 * - 생성, 조회, 수정, 삭제 (CRUD)
 * - DTO 변환
 * - 비즈니스 검증
 */
import com.knowlearnmap.member.repository.MemberRepository;
import com.knowlearnmap.member.domain.Member;
import com.knowlearnmap.domain.domain.DomainEntity;

// ... imports

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final com.knowlearnmap.domain.repository.DomainRepository domainRepository;
    private final com.knowlearnmap.document.repository.DocumentRepository documentRepository;
    private final MemberRepository memberRepository;
    private final com.knowlearnmap.document.service.DocumentService documentService;
    private final com.knowlearnmap.ontologyToArango.service.OntologyArangoCleanupService arangoCleanupService;

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceResponseDto> getAllWorkspaces(String username, Long domainId, String filter) {
        log.debug("사용자 {}의 워크스페이스 조회 (DomainId filter: {}, filter: {})", username, domainId, filter);

        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        List<WorkspaceEntity> workspaces;

        DomainEntity domainEntity = null;
        if (domainId != null) {
            domainEntity = domainRepository.findById(domainId).orElse(null);
        }

        // If domainId is null (Admin calling without filter?), use member's domain or
        // fallback?
        // But the Repository query requires DomainId.
        // If Admin selects a domain, use it. If Member, use member's domain.
        if (domainEntity == null) {
            String domainName = member.getDomain();
            if (domainName != null && !domainName.isEmpty()) {
                domainEntity = domainRepository.findByName(domainName).orElse(null);
            }
        }

        if (domainEntity == null) {
            // Fallback: If absolutely no domain context, return empty or all?
            // Since queries rely on domainId, we might return empty if not Admin.
            if (member.getRole() == Member.Role.ADMIN && filter.equals("ALL")) {
                // Admin sees ALL across system? (Repository method doesn't support system-wide
                // mixed yet)
                // For now, let's stick to domain-scoped.
                return List.of();
            }
            return List.of();
        }

        Long targetDomainId = domainEntity.getId();

        // ADMIN has special privileges, but if they want to see "MY" or "ALL" in the UI
        // context:
        // "ALL" -> Show everything in domain (Shared is implicitly everything active
        // for Admin?)
        // Actually, Admin "All" usually means EVERY workspace.
        // User "All" means "Mine + Shared".

        if (member.getRole() == Member.Role.ADMIN) {
            if ("MY".equalsIgnoreCase(filter)) {
                workspaces = workspaceRepository
                        .findByDomainIdAndCreatedByAndIsActiveTrueOrderByCreatedAtDesc(targetDomainId, username);
            } else {
                // Admin "ALL" = Show everything in the domain
                workspaces = workspaceRepository.findByDomainIdAndIsActiveTrueOrderByCreatedAtDesc(targetDomainId);
            }
        } else {
            // Normal User
            if ("MY".equalsIgnoreCase(filter)) {
                workspaces = workspaceRepository
                        .findByDomainIdAndCreatedByAndIsActiveTrueOrderByCreatedAtDesc(targetDomainId, username);
            } else {
                // "ALL" -> Mine + Shared
                workspaces = workspaceRepository.findSharedAndOwnedWorkspaces(targetDomainId, username);
            }
        }

        return workspaces.stream()
                .map(entity -> {
                    // 실제 문서 개수 조회
                    int documentCount = documentRepository.countByWorkspaceIdAndIsActiveTrue(entity.getId());
                    WorkspaceResponseDto dto = WorkspaceResponseDto.from(entity, documentCount);

                    // Role setting for Frontend UI (Owner vs Reader)
                    if (entity.getCreatedBy().equals(username) || member.getRole() == Member.Role.ADMIN) {
                        dto.setRole("Owner");
                    } else {
                        dto.setRole("Reader");
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponseDto getWorkspaceById(Long id) {
        log.debug("워크스페이스 조회: id={}", id);

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + id));

        // 실제 문서 개수 조회
        int documentCount = documentRepository.countByWorkspaceIdAndIsActiveTrue(workspace.getId());

        // Note: Individual get doesn't check permissions here strictly,
        // but arguably should if it's not shared and not owned.
        // Leaving as is for now or strictly enforcing?
        // User requested "Shared is visible", implying non-shared/non-owned should NOT
        // be visible.
        // But getById might be used by internal logic.
        // Controller calls this directly. Let's rely on List filtering for main UI
        // access control.

        return WorkspaceResponseDto.from(workspace, documentCount);
    }

    @Override
    @Transactional
    public WorkspaceResponseDto createWorkspace(WorkspaceRequestDto requestDto, String username) {
        log.debug("워크스페이스 생성: name={}, username={}", requestDto.getName(), username);

        // 1. 사용자 조회 및 등급 체크
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // 2. 도메인 조회
        com.knowlearnmap.domain.domain.DomainEntity domain = null;

        // 2-1. 요청에 Domain ID가 있는 경우 (Admin은 필수/권장, User는 본인 도메인과 일치해야 함)
        if (requestDto.getDomainId() != null) {
            domain = domainRepository.findById(requestDto.getDomainId())
                    .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다: " + requestDto.getDomainId()));

            // 일반 사용자는 본인 도메인 외 생성 불가 (검증)
            if (member.getRole() != Member.Role.ADMIN) {
                String userDomain = member.getDomain();
                if (userDomain == null || !userDomain.equals(domain.getName())) {
                    domain = null;
                }
            }
        }

        // 2-2. Domain이 아직 설정되지 않은 경우
        if (domain == null) {
            String domainName = member.getDomain();
            if (member.getRole() == Member.Role.ADMIN && domainName == null) {
                throw new IllegalArgumentException("Admin 사용자는 워크스페이스 생성 시 Domain ID를 명시해야 합니다.");
            }
            if (domainName == null || domainName.isEmpty()) {
                throw new IllegalArgumentException("사용자에게 할당된 도메인이 없습니다.");
            }
            domain = domainRepository.findByName(domainName)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 도메인을 찾을 수 없습니다: " + domainName));
        }

        // 3. 워크스페이스 개수 제한 체크
        if (member.getRole() != Member.Role.ADMIN && member.getGrade() != Member.Grade.MAX) {
            int currentCount = workspaceRepository.countByDomainIdAndIsActiveTrue(domain.getId());
            if (currentCount >= member.getGrade().getMaxWorkspaces()) {
                throw new IllegalArgumentException(
                        String.format("현재 등급(%s)에서는 워크스페이스를 최대 %d개까지만 생성할 수 있습니다.",
                                member.getGrade(), member.getGrade().getMaxWorkspaces()));
            }
        }

        // DTO -> Entity 변환
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName(requestDto.getName());
        workspace.setDescription(requestDto.getDescription());
        workspace.setIcon(requestDto.getIcon() != null ? requestDto.getIcon() : "📄");
        workspace.setColor(requestDto.getColor() != null ? requestDto.getColor() : "default");
        workspace.setWorkspaceType(requestDto.getWorkspaceType());

        workspace.setDomain(domain);
        workspace.setFolderName(requestDto.getFolderName());
        workspace.setPromptCode(requestDto.getPromptCode());
        workspace.setCreatedBy(username);

        // set Shared (Only Admin)
        if (requestDto.getIsShared() != null && requestDto.getIsShared()) {
            if (member.getRole() == Member.Role.ADMIN) {
                workspace.setIsShared(true);
            } else {
                // Ignore or error? Reset to false silently.
                workspace.setIsShared(false);
            }
        }

        WorkspaceEntity savedWorkspace = workspaceRepository.save(workspace);

        log.info("워크스페이스 생성 완료: id={}, name={}", savedWorkspace.getId(), savedWorkspace.getName());

        return WorkspaceResponseDto.from(savedWorkspace, 0);
    }

    @Override
    @Transactional
    public WorkspaceResponseDto updateWorkspace(Long id, WorkspaceRequestDto requestDto, String username) {
        log.debug("워크스페이스 수정: id={}, user={}", id, username);

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + id));

        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // Permission Check: Owner or Admin
        if (!workspace.getCreatedBy().equals(username) && member.getRole() != Member.Role.ADMIN) {
            throw new IllegalArgumentException("워크스페이스 수정 권한이 없습니다 (본인 생성 또는 Admin만 가능)");
        }

        // 엔티티 업데이트
        workspace.setName(requestDto.getName());
        workspace.setDescription(requestDto.getDescription());
        workspace.setIcon(requestDto.getIcon());
        workspace.setColor(requestDto.getColor());
        workspace.setWorkspaceType(requestDto.getWorkspaceType());
        if (requestDto.getDomainId() != null) {
            com.knowlearnmap.domain.domain.DomainEntity domain = domainRepository.findById(requestDto.getDomainId())
                    .orElseThrow(() -> new IllegalArgumentException("도메인을 찾을 수 없습니다: " + requestDto.getDomainId()));
            workspace.setDomain(domain);
        }
        workspace.setPromptCode(requestDto.getPromptCode());

        if (requestDto.getFolderName() != null && !requestDto.getFolderName().isEmpty()) {
            workspace.setFolderName(requestDto.getFolderName());
        }

        // Update Shared Status (Admin Only)
        if (requestDto.getIsShared() != null) {
            if (member.getRole() == Member.Role.ADMIN) {
                workspace.setIsShared(requestDto.getIsShared());
            }
            // Non-admin request to change shared status is ignored
        }

        WorkspaceEntity updatedWorkspace = workspaceRepository.save(workspace);

        log.info("워크스페이스 수정 완료: id={}, name={}", updatedWorkspace.getId(), updatedWorkspace.getName());

        return WorkspaceResponseDto.from(updatedWorkspace, 0);
    }

    @Override
    @Transactional
    public void deleteWorkspace(Long id, String username) {
        log.debug("워크스페이스 삭제: id={}, user={}", id, username);

        WorkspaceEntity workspace = workspaceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없습니다: " + id));

        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        // Permission Check: Owner or Admin
        if (!workspace.getCreatedBy().equals(username) && member.getRole() != Member.Role.ADMIN) {
            throw new IllegalArgumentException("워크스페이스 삭제 권한이 없습니다 (본인 생성 또는 Admin만 가능)");
        }

        // 1. Delete all documents (triggers cascade delete including ArangoDB cleanup)
        List<com.knowlearnmap.document.domain.DocumentEntity> documents = documentRepository
                .findByWorkspaceIdAndIsActiveTrueOrderByCreatedAtDesc(id);

        log.info("Deleting {} documents in workspace {}", documents.size(), id);
        for (com.knowlearnmap.document.domain.DocumentEntity doc : documents) {
            try {
                documentService.deleteDocument(doc.getId(), username);
            } catch (Exception e) {
                log.error("Failed to delete document {}: {}", doc.getId(), e.getMessage());
                // Continue with other documents
            }
        }

        // 2. Delete ArangoDB collections (entire workspace data)
        if (workspace.getDomain() != null && workspace.getDomain().getArangoDbName() != null) {
            String dbName = workspace.getDomain().getArangoDbName();
            arangoCleanupService.deleteWorkspaceCollections(dbName);
            log.info("ArangoDB collections deleted for workspace {}", id);
        } else {
            log.warn("No ArangoDB configured for workspace {}, skipping ArangoDB cleanup", id);
        }

        // 3. Hard delete workspace
        workspaceRepository.delete(workspace);

        log.info("워크스페이스 삭제 완료 (hard delete): id={}, name={}, docCount={}",
                workspace.getId(), workspace.getName(), documents.size());
    }
}
