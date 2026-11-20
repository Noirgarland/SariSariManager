package com.example.sarisari.manager.domain.model

/** Thrown by repositories when a unique name is already used. */
class DuplicateNameException(val name: String) : Exception("Name already exists: $name")
