package com.sfood.automation.dashboard.infrastructure.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.dashboard.domain.model.GodoOrder;
import com.sfood.automation.dashboard.domain.model.GodoOrderItem;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 고도몰 Order_Search.php 응답(XML)을 파싱해 회원 기반 매출 집계용 데이터로 변환한다.
 * 9.2.3(dashboard-integration 분석 문서)에서 확정된 규칙을 그대로 반영:
 *  - orderStatus 화이트리스트({@link #ALLOWED_ORDER_STATUSES})에 없는 주문은 제외
 *  - memId 유무와 무관하게 주문은 전부 포함한다(원본 update_data.py와 동일) —
 *    memGroupNm이 빈 값이면 등급을 "비회원"으로 라벨링할 뿐, 주문 자체를 제외하지 않는다.
 *  - 결제_final(payment_final_amount)은 settlePrice가 아닌 사용 가능 필드로 직접 계산
 *  - deliveredAt은 API 값 그대로, omsShippedAt = deliveredAt + 1일(파생)
 */
public class GodoOrderResponseParser {

    private static final Set<String> ALLOWED_ORDER_STATUSES = Set.of(
        "o1", "p1", "g1", "d1", "d2", "f2", "f4", "s1", "e1", "e5"
    );
    private static final DateTimeFormatter DATE_PART = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final XmlMapper xmlMapper;

    public GodoOrderResponseParser(XmlMapper xmlMapper) {
        this.xmlMapper = xmlMapper;
    }

    public GodoOrderSearchResult parse(String responseBody) {
        try {
            JsonNode root = xmlMapper.readTree(responseBody);
            List<GodoOrder> orders = new ArrayList<>();
            List<GodoOrderItem> items = new ArrayList<>();
            Long nextLastOrder = null;

            for (JsonNode orderNode : asList(root.path("return").path("order_data"))) {
                long orderNo = Long.parseLong(text(orderNode, "orderNo"));
                nextLastOrder = nextLastOrder == null ? orderNo : Math.min(nextLastOrder, orderNo);

                String memberId = text(orderNode, "memId");
                String orderStatus = text(orderNode, "orderStatus");
                if (!ALLOWED_ORDER_STATUSES.contains(orderStatus)) {
                    continue;
                }

                GodoOrder order = toOrder(orderNode, orderNo, memberId, orderStatus);
                orders.add(order);

                for (JsonNode itemNode : asList(orderNode.path("orderGoodsData"))) {
                    items.add(toItem(itemNode, "GOODS", orderNo));
                }
                for (JsonNode itemNode : asList(orderNode.path("addGoodsData"))) {
                    items.add(toItem(itemNode, "ADD_GOODS", orderNo));
                }
            }

            boolean hasMore = "true".equalsIgnoreCase(text(root.path("header"), "lastOrder"));
            return new GodoOrderSearchResult(orders, items, hasMore, nextLastOrder);
        } catch (GodoOrderResponseParseException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new GodoOrderResponseParseException(exception);
        }
    }

    private GodoOrder toOrder(JsonNode orderNode, long orderNo, String memberId, String orderStatus) {
        BigDecimal totalGoodsPrice = decimal(orderNode, "totalGoodsPrice");
        BigDecimal totalDeliveryCharge = decimal(orderNode, "totalDeliveryCharge");
        BigDecimal useDeposit = decimal(orderNode, "useDeposit");
        BigDecimal useMileage = decimal(orderNode, "useMileage");
        BigDecimal totalGoodsDcPrice = decimal(orderNode, "totalGoodsDcPrice");
        BigDecimal totalMemberDcPrice = decimal(orderNode, "totalMemberDcPrice");
        BigDecimal totalCouponDcPrice = decimal(orderNode, "totalCouponGoodsDcPrice")
            .add(decimal(orderNode, "totalCouponOrderDcPrice"))
            .add(decimal(orderNode, "totalCouponDeliveryDcPrice"));

        BigDecimal paymentFinalAmount = totalGoodsPrice
            .add(totalDeliveryCharge)
            .subtract(useDeposit)
            .subtract(useMileage)
            .subtract(totalGoodsDcPrice)
            .subtract(totalMemberDcPrice)
            .subtract(totalCouponDcPrice)
            .multiply(BigDecimal.TEN)
            .divide(BigDecimal.valueOf(11), 2, RoundingMode.HALF_UP);

        String memberGrade = text(orderNode, "memGroupNm");
        if (memberGrade.isBlank()) {
            memberGrade = "비회원";
        }

        String now = java.time.Instant.now().toString();
        return new GodoOrder(
            null,
            orderNo,
            memberId,
            memberGrade,
            orderStatus,
            text(orderNode, "orderDate"),
            totalGoodsPrice,
            totalDeliveryCharge,
            useDeposit,
            useMileage,
            totalGoodsDcPrice,
            totalMemberDcPrice,
            totalCouponDcPrice,
            paymentFinalAmount,
            now,
            now
        );
    }

    private GodoOrderItem toItem(JsonNode itemNode, String itemType, long orderNo) {
        long godoLineNo = Long.parseLong(text(itemNode, "sno"));
        int productQty = Integer.parseInt(text(itemNode, "goodsCnt"));
        BigDecimal productUnitPrice = decimal(itemNode, "goodsPrice");
        BigDecimal productLineRevenue = productUnitPrice.multiply(BigDecimal.valueOf(productQty));
        String deliveredAt = validDateOrNull(text(itemNode, "deliveryDt"));
        String omsShippedAt = deliveredAt == null ? null : plusOneDay(deliveredAt);

        String now = java.time.Instant.now().toString();
        return new GodoOrderItem(
            null,
            itemType,
            godoLineNo,
            orderNo,
            text(itemNode, "goodsNm"),
            productQty,
            productUnitPrice,
            productLineRevenue,
            deliveredAt,
            omsShippedAt,
            now,
            now
        );
    }

    private String validDateOrNull(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank() || rawDateTime.contains("0000")) {
            return null;
        }
        return rawDateTime;
    }

    private String plusOneDay(String dateTime) {
        LocalDate date = LocalDate.parse(dateTime.substring(0, 10), DATE_PART);
        return date.plusDays(1).format(DATE_PART);
    }

    private List<JsonNode> asList(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> result = new ArrayList<>();
            node.forEach(result::add);
            return result;
        }
        return List.of(node);
    }

    private String text(JsonNode node, String fieldName) {
        return node.path(fieldName).asText("").trim();
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        String value = text(node, fieldName);
        return value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
