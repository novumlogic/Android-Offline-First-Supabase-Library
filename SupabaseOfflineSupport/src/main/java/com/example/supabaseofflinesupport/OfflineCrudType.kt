package com.example.supabaseofflinesupport
/**
 *  Enum class representing the different types of CRUD operations that were performed when offline.
 *  @property NONE denotes no operation
 *  @property INSERT denotes insert operation
 *  @property UPDATE denotes update operation
 *  @property DELETE denotes delete operation
 */
enum class OfflineCrudType {
    NONE, INSERT, UPDATE, DELETE
}