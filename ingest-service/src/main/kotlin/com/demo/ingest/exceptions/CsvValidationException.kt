package com.demo.ingest.exceptions

class CsvValidationException(val errors: List<String>) : RuntimeException("CSV validation failed")
