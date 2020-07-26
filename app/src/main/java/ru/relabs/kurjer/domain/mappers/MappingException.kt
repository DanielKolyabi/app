package ru.relabs.kurjer.domain.mappers

class MappingException(val field: String, val value: Any) : RuntimeException("Mapping error on field $field got $value")