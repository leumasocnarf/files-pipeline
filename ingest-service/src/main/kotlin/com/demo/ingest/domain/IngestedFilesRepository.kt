package com.demo.ingest.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface IngestedFilesRepository : JpaRepository<IngestedFile, UUID> {

    @Modifying
    @Query("UPDATE IngestedFile f SET f.status = :status WHERE f.id = :id")
    fun updateStatus(@Param("id") id: UUID, @Param("status") status: IngestedFileStatus)
}