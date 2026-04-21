package com.demo.ingest.domain

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "ingested_files")
@EntityListeners(AuditingEntityListener::class)
class IngestedFile(

    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val filename: String,

    @Column(nullable = false)
    val contentType: String,

    @Column(nullable = false)
    val fileSize: Long,

    @Lob
    @Column(nullable = false, updatable = false)
    val fileData: ByteArray,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null
)