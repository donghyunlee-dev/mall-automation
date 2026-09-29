package com.sfood.automation.dashboard.infrastructure.persistence;

import com.sfood.automation.dashboard.domain.model.GodoOrderItem;
import com.sfood.automation.dashboard.domain.port.GodoOrderItemRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class GodoOrderItemRepositoryImpl implements GodoOrderItemRepository {

    private final GodoOrderItemJpaRepository godoOrderItemJpaRepository;

    public GodoOrderItemRepositoryImpl(GodoOrderItemJpaRepository godoOrderItemJpaRepository) {
        this.godoOrderItemJpaRepository = godoOrderItemJpaRepository;
    }

    @Override
    public GodoOrderItem upsert(GodoOrderItem item) {
        GodoOrderItemEntity entity = godoOrderItemJpaRepository
            .findByItemTypeAndGodoLineNo(item.itemType(), item.godoLineNo())
            .orElseGet(GodoOrderItemEntity::new);
        applyTo(entity, item);
        return toDomain(godoOrderItemJpaRepository.save(entity));
    }

    @Override
    public List<GodoOrderItem> findByOrderNo(long orderNo) {
        return godoOrderItemJpaRepository.findByOrderNo(orderNo).stream()
            .map(this::toDomain)
            .toList();
    }

    private void applyTo(GodoOrderItemEntity entity, GodoOrderItem item) {
        entity.setItemType(item.itemType());
        entity.setGodoLineNo(item.godoLineNo());
        entity.setOrderNo(item.orderNo());
        entity.setProductName(item.productName());
        entity.setProductQty(item.productQty());
        entity.setProductUnitPrice(item.productUnitPrice());
        entity.setProductLineRevenue(item.productLineRevenue());
        entity.setDeliveredAt(item.deliveredAt());
        entity.setOmsShippedAt(item.omsShippedAt());
        entity.setCreatedAt(item.createdAt());
        entity.setUpdatedAt(item.updatedAt());
    }

    private GodoOrderItem toDomain(GodoOrderItemEntity entity) {
        return new GodoOrderItem(
            entity.getId(),
            entity.getItemType(),
            entity.getGodoLineNo(),
            entity.getOrderNo(),
            entity.getProductName(),
            entity.getProductQty(),
            entity.getProductUnitPrice(),
            entity.getProductLineRevenue(),
            entity.getDeliveredAt(),
            entity.getOmsShippedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
