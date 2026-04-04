package com.demo.ingest.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID


@Entity
@Table(name = "ingested_files")
@EntityListeners(AuditingEntityListener::class)
class IngestedFile(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    val filename: String,

    @Column(name = "content_type", nullable = false)
    val contentType: String,

    @Column(name = "file_size", nullable = false)
    val fileSize: Long,

    @Lob
    @Column(name = "file_data", nullable = false, updatable = false)
    val fileData: ByteArray,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: IngestedFileStatus = IngestedFileStatus.PENDING,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
)

enum class IngestedFileStatus {
    PENDING, PUBLISHED, FAILED
}
