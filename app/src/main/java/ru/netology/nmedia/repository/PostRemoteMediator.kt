package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val service: ApiService,
    private val postDao: PostDao,
    private val db: AppDb,
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun initialize(): InitializeAction =
        if (postDao.isEmpty()) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>,
    ): MediatorResult {
        return try {
            val response = when (loadType) {
                LoadType.REFRESH -> {
                    if (postDao.isEmpty()) {
                        service.getLatest(state.config.initialLoadSize)
                    } else {
                        val topId = postDao.maxId()
                            ?: return MediatorResult.Success(endOfPaginationReached = true)
                        service.getAfter(topId, state.config.initialLoadSize)
                    }
                }

                LoadType.PREPEND -> {
                    val firstItem = state.firstItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    service.getAfter(firstItem.id, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val lastItem = state.lastItemOrNull()
                        ?: return MediatorResult.Success(endOfPaginationReached = false)
                    service.getBefore(lastItem.id, state.config.pageSize)
                }
            }

            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }

            val body = response.body() ?: throw ApiError(response.code(), response.message())

            db.withTransaction {
                postDao.insert(body.toEntity())
            }

            MediatorResult.Success(endOfPaginationReached = body.isEmpty())
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}