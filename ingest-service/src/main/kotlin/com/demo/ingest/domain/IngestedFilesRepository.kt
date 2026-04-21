package com.demo.ingest.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface IngestedFilesRepository : JpaRepository<IngestedFile, UUID>