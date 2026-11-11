package com.bakery.bakeryplatform.service.catalogue;

import com.bakery.bakeryplatform.model.catalogue.Item;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class ItemPopularityService {
    private final ConcurrentMap<String, LongAdder> addToCartCounts = new ConcurrentHashMap<>();

    public void recordAddToCart(String itemId) {
        addToCartCounts.computeIfAbsent(itemId, ignored -> new LongAdder()).increment();
    }

    public List<Item> getPopularItems(List<Item> items, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        return items.stream()
                .filter(candidate -> candidate != null && candidate.getId() != null)
                .filter(candidate -> "Available".equals(candidate.getAvailability()))
                .filter(candidate -> addToCartCounts.containsKey(candidate.getId()))
                .sorted(Comparator
                        .<Item>comparingLong(candidate -> addToCartCounts.get(candidate.getId()).sum())
                        .reversed()
                        .thenComparing(Item::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(limit)
                .toList();
    }
}
