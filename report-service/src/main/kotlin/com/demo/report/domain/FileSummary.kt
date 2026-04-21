package com.demo.report.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "file_summaries")
@EntityListeners(AuditingEntityListener::class)
class FileSummary(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val fileId: UUID,

    @Column(nullable = false)
    val jobId: UUID,

    @Column(nullable = false)
    val filename: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val status: SummaryStatus,

    @Column(nullable = false)
    val totalRows: Int,

    @Column(nullable = false)
    val validRows: Int,

    @Column(nullable = false)
    val invalidRows: Int,

    @Column(columnDefinition = "text")
    val summaryData: String?,

    val errorMessage: String?,

    @Column(nullable = false)
    val processedAt: Instant,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,
)

enum class SummaryStatus { COMPLETED, FAILED }
