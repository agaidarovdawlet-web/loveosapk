package com.example.loveosapk.domain.repository

import kotlinx.coroutines.flow.Flow

interface RemoteDataSource {
    fun <T> observeValue(path: String, clazz: Class<T>): Flow<Result<T>>
    suspend fun <T> getValueOnce(path: String, clazz: Class<T>): Result<T>
    suspend fun <T> setValue(path: String, value: T): Result<Unit>
    suspend fun updateChildren(path: String, values: Map<String, Any?>): Result<Unit>
}
