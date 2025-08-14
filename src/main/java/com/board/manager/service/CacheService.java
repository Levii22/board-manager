package com.board.manager.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class CacheService {

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictBoardCache(Integer boardId) {
        log.debug("Evicting cache for board: {}", boardId);
        
        // Evict specific board cache
        Objects.requireNonNull(cacheManager.getCache("board")).evict(boardId);
        
        // Evict boards list cache (since it might contain this board)
        Objects.requireNonNull(cacheManager.getCache("boards")).clear();
        
        // Evict tasks cache for this board
        Objects.requireNonNull(cacheManager.getCache("tasks")).evict("board:" + boardId);
    }

    public void evictBoardsCache() {
        log.debug("Evicting all boards cache");
        Objects.requireNonNull(cacheManager.getCache("boards")).clear();
    }

    public void evictTaskCache(Integer boardId) {
        log.debug("Evicting task cache for board: {}", boardId);
        
        // Evict tasks cache for this board
        Objects.requireNonNull(cacheManager.getCache("tasks")).evict("board:" + boardId);
        
        // Also evict the board cache since it contains tasks
        Objects.requireNonNull(cacheManager.getCache("board")).evict(boardId);
        Objects.requireNonNull(cacheManager.getCache("boards")).clear();
    }

    public void evictUserRelatedCaches(Integer userId) {
        log.debug("Evicting user-related caches for user: {}", userId);
        
        // Clear all boards cache since user permissions might have changed
        Objects.requireNonNull(cacheManager.getCache("boards")).clear();
        Objects.requireNonNull(cacheManager.getCache("board")).clear();
        Objects.requireNonNull(cacheManager.getCache("tasks")).clear();
    }
}
