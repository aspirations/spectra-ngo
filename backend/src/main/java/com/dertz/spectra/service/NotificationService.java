package com.dertz.spectra.service;

import com.dertz.spectra.Enum.NotificationEmailStatus;
import com.dertz.spectra.Enum.NotificationType;
import com.dertz.spectra.exception.ResourceNotFoundException;
import com.dertz.spectra.model.AppNotification;
import com.dertz.spectra.repository.AppNotificationRepository;
import com.dertz.spectra.security.BranchScope;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final AppNotificationRepository notificationRepository;
	private final Environment environment;

	@Transactional
	public AppNotification notify(Long tenantId, Long branchId, Long userId, NotificationType type, String title,
			String body, String entityType, Long entityId) {
		AppNotification row = notificationRepository.save(AppNotification.builder()
				.tenantId(tenantId)
				.branchId(branchId)
				.userId(userId)
				.type(type)
				.title(title)
				.body(body)
				.entityType(entityType)
				.entityId(entityId)
				.emailStatus(NotificationEmailStatus.PENDING)
				.build());
		row.setEmailStatus(mailConfigured() ? NotificationEmailStatus.PENDING : NotificationEmailStatus.SKIPPED);
		return notificationRepository.save(row);
	}

	@Transactional(readOnly = true)
	public List<AppNotification> mine() {
		return notificationRepository.findByUserIdAndBranchIdOrderByCreatedDateDesc(
				BranchScope.currentUserId(), BranchScope.requireBranchId());
	}

	@Transactional(readOnly = true)
	public long unreadCount() {
		return notificationRepository.countByUserIdAndBranchIdAndReadAtIsNull(
				BranchScope.currentUserId(), BranchScope.requireBranchId());
	}

	@Transactional
	public AppNotification markRead(Long id) {
		AppNotification row = notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
		if (!BranchScope.currentUserId().equals(row.getUserId())) {
			throw new ResourceNotFoundException("Notification not found");
		}
		if (row.getReadAt() == null) {
			row.setReadAt(Instant.now());
		}
		return notificationRepository.save(row);
	}

	private boolean mailConfigured() {
		return StringUtils.hasText(environment.getProperty("spring.mail.host"));
	}
}
