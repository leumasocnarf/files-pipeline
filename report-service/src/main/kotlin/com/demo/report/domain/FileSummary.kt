package com.demo.report.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "file_summaries")
@EntityListeners(AuditingEntityListener::class)
class FileSummary(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(name = "file_id", nullable = false, unique = true)
    val fileId: UUID,

    @Column(name = "job_id", nullable = false)
    val jobId: UUID,

    @Column(nullable = false)
    val filename: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: SummaryStatus,

    @Column(name = "total_rows", nullable = false)
    val totalRows: Int,

    @Column(name = "valid_rows", nullable = false)
    val validRows: Int,

    @Column(name = "invalid_rows", nullable = false)
    val invalidRows: Int,

    @Column(name = "summary_data", columnDefinition = "text")
    val summaryData: String?,

    @Column(name = "error_message")
    val errorMessage: String?,

    @Column(name = "processed_at", nullable = false)
    val processedAt: Instant,

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null,
)

enum class SummaryStatus { COMPLETED, FAILED }
