package com.demo.ingest.exceptions

import java.util.UUID

class FileNotFoundException(val fileId: UUID) : RuntimeException("File not found: $fileId")