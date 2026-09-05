package com.dertz.spectra.model;

import static jakarta.persistence.TemporalType.TIMESTAMP;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@CreatedDate
	@Temporal(TIMESTAMP)
	@Column(updatable = false)
	protected Date createdDate;

	@LastModifiedDate
	@Temporal(TIMESTAMP)
	@Column(insertable = false)
	protected Date lastModifiedDate;

	@CreatedBy
	@Column(updatable = false)
	protected Long createdBy;

	@LastModifiedBy
	@Column(insertable = false)
	protected Long lastModifiedBy;

	public boolean isIdValid() {
		return getId() != null && getId() > 0;
	}
}
