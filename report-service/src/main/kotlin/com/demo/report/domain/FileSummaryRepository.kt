package com.demo.report.domain

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FileSummaryRepository : JpaRepository<FileSummary, UUID> {

    fun findByFileId(fileId: UUID): FileSummary?

    fun findAllByStatus(status: SummaryStatus, pageable: Pageable): Page<FileSummary>

    fun findAllByOrderByProcessedAtDesc(pageable: Pageable): Page<FileSummary>

    fun existsByFileId(fileId: UUID): Boolean
}