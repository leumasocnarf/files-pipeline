package com.demo.report.exceptions

import java.util.UUID

class SummaryNotFoundException(val fileId: UUID) : RuntimeException("No summary found for file $fileId")