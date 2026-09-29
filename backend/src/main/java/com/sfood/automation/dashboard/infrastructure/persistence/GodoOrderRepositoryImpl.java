package com.sfood.automation.dashboard.infrastructure.persistence;

import com.sfood.automation.dashboard.domain.model.GodoOrder;
import com.sfood.automation.dashboard.domain.port.GodoOrderRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class GodoOrderRepositoryImpl implements GodoOrderRepository {

    private final GodoOrderJpaRepository godoOrderJpaRepository;

    public GodoOrderRepositoryImpl(GodoOrderJpaRepository godoOrderJpaRepository) {
        this.godoOrderJpaRepository = godoOrderJpaRepository;
    }

    @Override
    public GodoOrder upsert(GodoOrder order) {
        GodoOrderEntity entity = godoOrderJpaRepository.findByOrderNo(order.orderNo())
            .orElseGet(GodoOrderEntity::new);
        applyTo(entity, order);
        return toDomain(godoOrderJpaRepository.save(entity));
    }

    @Override
    public Optional<GodoOrder> findByOrderNo(long orderNo) {
        return godoOrderJpaRepository.findByOrderNo(orderNo).map(this::toDomain);
    }

    private void applyTo(GodoOrderEntity entity, GodoOrder order) {
        entity.setOrderNo(order.orderNo());
        entity.setMemberId(order.memberId());
        entity.setMemberGrade(order.memberGrade());
        entity.setOrderStatus(order.orderStatus());
        entity.setOrderedAt(order.orderedAt());
        entity.setTotalGoodsPrice(order.totalGoodsPrice());
        entity.setTotalDeliveryCharge(order.totalDeliveryCharge());
        entity.setUseDeposit(order.useDeposit());
        entity.setUseMileage(order.useMileage());
        entity.setTotalGoodsDcPrice(order.totalGoodsDcPrice());
        entity.setTotalMemberDcPrice(order.totalMemberDcPrice());
        entity.setTotalCouponDcPrice(order.totalCouponDcPrice());
        entity.setPaymentFinalAmount(order.paymentFinalAmount());
        entity.setCreatedAt(order.createdAt());
        entity.setUpdatedAt(order.updatedAt());
    }

    private GodoOrder toDomain(GodoOrderEntity entity) {
        return new GodoOrder(
            entity.getId(),
            entity.getOrderNo(),
            entity.getMemberId(),
            entity.getMemberGrade(),
            entity.getOrderStatus(),
            entity.getOrderedAt(),
            entity.getTotalGoodsPrice(),
            entity.getTotalDeliveryCharge(),
            entity.getUseDeposit(),
            entity.getUseMileage(),
            entity.getTotalGoodsDcPrice(),
            entity.getTotalMemberDcPrice(),
            entity.getTotalCouponDcPrice(),
            entity.getPaymentFinalAmount(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
