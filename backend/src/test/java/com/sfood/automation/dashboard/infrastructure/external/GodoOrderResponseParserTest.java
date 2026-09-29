package com.sfood.automation.dashboard.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sfood.automation.dashboard.domain.model.GodoOrder;
import com.sfood.automation.dashboard.domain.model.GodoOrderItem;
import com.sfood.automation.dashboard.domain.model.GodoOrderSearchResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class GodoOrderResponseParserTest {

    private final GodoOrderResponseParser parser = new GodoOrderResponseParser(new XmlMapper());

    @Test
    void should_compute_payment_final_amount_from_usable_fields_only() {
        String xml = orderXml(
            "2603241752568170", "member01", "일반회원", "s1",
            "11000.00", "3000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("449051", "리얼 스모크햄", 1, "11000.00", "2026-03-25 09:38:10")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.orders()).hasSize(1);
        GodoOrder order = result.orders().get(0);
        assertThat(order.orderNo()).isEqualTo(2603241752568170L);
        assertThat(order.memberId()).isEqualTo("member01");
        assertThat(order.memberGrade()).isEqualTo("일반회원");
        assertThat(order.orderStatus()).isEqualTo("s1");
        // (11000 + 3000 - 0 - 0 - 0 - 0 - 0) * 10/11 = 12727.27 (반올림)
        assertThat(order.paymentFinalAmount()).isEqualByComparingTo("12727.27");
    }

    @Test
    void should_exclude_order_when_status_not_in_whitelist() {
        String xml = orderXml(
            "1", "member01", "일반회원", "c1",
            "10000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("1", "상품", 1, "10000.00", "2026-03-25 09:38:10")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.orders()).isEmpty();
        assertThat(result.items()).isEmpty();
    }

    @Test
    void should_include_guest_order_and_label_grade_as_non_member() {
        // 원본 데이터 파이프라인(update_data.py)은 회원 아이디 유무와 무관하게 주문을 전부 포함하고,
        // 회원등급명(memGroupNm)이 빈 값일 때만 "비회원"으로 라벨링한다.
        String xml = orderXml(
            "1", "", "", "s1",
            "10000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("1", "상품", 1, "10000.00", "2026-03-25 09:38:10")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.orders()).hasSize(1);
        GodoOrder order = result.orders().get(0);
        assertThat(order.memberId()).isEmpty();
        assertThat(order.memberGrade()).isEqualTo("비회원");
        assertThat(result.items()).hasSize(1);
    }

    @Test
    void should_keep_non_blank_member_grade_unchanged() {
        String xml = orderXml(
            "1", "member01", "일반회원", "s1",
            "10000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("1", "상품", 1, "10000.00", "2026-03-25 09:38:10")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.orders().get(0).memberGrade()).isEqualTo("일반회원");
    }

    @Test
    void should_derive_oms_shipped_at_as_delivered_at_plus_one_day() {
        String xml = orderXml(
            "1", "member01", "일반회원", "s1",
            "10000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("1", "상품", 1, "10000.00", "2026-03-25 09:38:10")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        GodoOrderItem item = result.items().get(0);
        assertThat(item.deliveredAt()).isEqualTo("2026-03-25 09:38:10");
        assertThat(item.omsShippedAt()).isEqualTo("2026-03-26");
    }

    @Test
    void should_set_oms_shipped_at_null_when_not_yet_delivered() {
        String xml = orderXml(
            "1", "member01", "일반회원", "o1",
            "10000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("1", "상품", 1, "10000.00", "0000-00-00 00:00:00")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        GodoOrderItem item = result.items().get(0);
        assertThat(item.deliveredAt()).isNull();
        assertThat(item.omsShippedAt()).isNull();
    }

    @Test
    void should_tag_add_goods_data_items_with_add_goods_type() {
        String xml = """
            <data>
                <header><code>000</code><msg>success</msg></header>
                <return>
                    <order_data idx="1">
                        <orderNo>1</orderNo>
                        <orderStatus>s1</orderStatus>
                        <memId>member01</memId>
                        <memGroupNm>일반회원</memGroupNm>
                        <orderDate>2026-03-24 17:52:56</orderDate>
                        <totalGoodsPrice>10000.00</totalGoodsPrice>
                        <totalDeliveryCharge>0.00</totalDeliveryCharge>
                        <useDeposit>0.00</useDeposit>
                        <useMileage>0.00</useMileage>
                        <totalGoodsDcPrice>0.00</totalGoodsDcPrice>
                        <totalMemberDcPrice>0.00</totalMemberDcPrice>
                        <totalCouponGoodsDcPrice>0.00</totalCouponGoodsDcPrice>
                        <totalCouponOrderDcPrice>0.00</totalCouponOrderDcPrice>
                        <totalCouponDeliveryDcPrice>0.00</totalCouponDeliveryDcPrice>
                        <addGoodsData idx="1">
                            <sno>1</sno>
                            <goodsNm>사은품</goodsNm>
                            <goodsCnt>1</goodsCnt>
                            <goodsPrice>0.00</goodsPrice>
                            <deliveryDt>2026-03-25 09:38:10</deliveryDt>
                        </addGoodsData>
                    </order_data>
                </return>
            </data>
            """;

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).itemType()).isEqualTo("ADD_GOODS");
    }

    @Test
    void should_set_has_more_true_when_header_last_order_flag_is_true() {
        String xml = """
            <data>
                <header><code>000</code><msg>success</msg><lastOrder>true</lastOrder></header>
                <return></return>
            </data>
            """;

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.hasMore()).isTrue();
    }

    @Test
    void should_set_has_more_false_when_header_last_order_flag_is_absent() {
        String xml = orderXml(
            "1", "member01", "일반회원", "s1",
            "10000.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00", "0.00",
            goodsItem("1", "상품", 1, "10000.00", "2026-03-25 09:38:10")
        );

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void should_compute_next_last_order_as_minimum_order_no_across_all_orders_including_filtered() {
        String xml = """
            <data>
                <header><code>000</code><msg>success</msg></header>
                <return>
                    <order_data idx="1">
                        <orderNo>5</orderNo>
                        <orderStatus>s1</orderStatus>
                        <memId>member01</memId>
                        <memGroupNm>일반회원</memGroupNm>
                        <orderDate>2026-03-24 17:52:56</orderDate>
                        <totalGoodsPrice>10000.00</totalGoodsPrice>
                        <totalDeliveryCharge>0.00</totalDeliveryCharge>
                        <useDeposit>0.00</useDeposit>
                        <useMileage>0.00</useMileage>
                        <totalGoodsDcPrice>0.00</totalGoodsDcPrice>
                        <totalMemberDcPrice>0.00</totalMemberDcPrice>
                        <totalCouponGoodsDcPrice>0.00</totalCouponGoodsDcPrice>
                        <totalCouponOrderDcPrice>0.00</totalCouponOrderDcPrice>
                        <totalCouponDeliveryDcPrice>0.00</totalCouponDeliveryDcPrice>
                    </order_data>
                    <order_data idx="2">
                        <orderNo>3</orderNo>
                        <orderStatus>c1</orderStatus>
                        <memId>member02</memId>
                        <memGroupNm>일반회원</memGroupNm>
                        <orderDate>2026-03-24 17:00:00</orderDate>
                        <totalGoodsPrice>0.00</totalGoodsPrice>
                        <totalDeliveryCharge>0.00</totalDeliveryCharge>
                        <useDeposit>0.00</useDeposit>
                        <useMileage>0.00</useMileage>
                        <totalGoodsDcPrice>0.00</totalGoodsDcPrice>
                        <totalMemberDcPrice>0.00</totalMemberDcPrice>
                        <totalCouponGoodsDcPrice>0.00</totalCouponGoodsDcPrice>
                        <totalCouponOrderDcPrice>0.00</totalCouponOrderDcPrice>
                        <totalCouponDeliveryDcPrice>0.00</totalCouponDeliveryDcPrice>
                    </order_data>
                </return>
            </data>
            """;

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.orders()).hasSize(1);
        assertThat(result.nextLastOrder()).isEqualTo(3L);
    }

    @Test
    void should_parse_multiple_orders_when_response_has_more_than_one_order() {
        String xml = """
            <data>
                <header><code>000</code><msg>success</msg></header>
                <return>
                    <order_data idx="1">
                        <orderNo>1</orderNo>
                        <orderStatus>s1</orderStatus>
                        <memId>member01</memId>
                        <memGroupNm>일반회원</memGroupNm>
                        <orderDate>2026-03-24 17:52:56</orderDate>
                        <totalGoodsPrice>10000.00</totalGoodsPrice>
                        <totalDeliveryCharge>0.00</totalDeliveryCharge>
                        <useDeposit>0.00</useDeposit>
                        <useMileage>0.00</useMileage>
                        <totalGoodsDcPrice>0.00</totalGoodsDcPrice>
                        <totalMemberDcPrice>0.00</totalMemberDcPrice>
                        <totalCouponGoodsDcPrice>0.00</totalCouponGoodsDcPrice>
                        <totalCouponOrderDcPrice>0.00</totalCouponOrderDcPrice>
                        <totalCouponDeliveryDcPrice>0.00</totalCouponDeliveryDcPrice>
                        <orderGoodsData idx="1">
                            <sno>1</sno>
                            <goodsNm>상품A</goodsNm>
                            <goodsCnt>1</goodsCnt>
                            <goodsPrice>10000.00</goodsPrice>
                            <deliveryDt>2026-03-25 09:38:10</deliveryDt>
                        </orderGoodsData>
                    </order_data>
                    <order_data idx="2">
                        <orderNo>2</orderNo>
                        <orderStatus>s1</orderStatus>
                        <memId>member02</memId>
                        <memGroupNm>일반회원</memGroupNm>
                        <orderDate>2026-03-24 18:00:00</orderDate>
                        <totalGoodsPrice>20000.00</totalGoodsPrice>
                        <totalDeliveryCharge>0.00</totalDeliveryCharge>
                        <useDeposit>0.00</useDeposit>
                        <useMileage>0.00</useMileage>
                        <totalGoodsDcPrice>0.00</totalGoodsDcPrice>
                        <totalMemberDcPrice>0.00</totalMemberDcPrice>
                        <totalCouponGoodsDcPrice>0.00</totalCouponGoodsDcPrice>
                        <totalCouponOrderDcPrice>0.00</totalCouponOrderDcPrice>
                        <totalCouponDeliveryDcPrice>0.00</totalCouponDeliveryDcPrice>
                        <orderGoodsData idx="1">
                            <sno>2</sno>
                            <goodsNm>상품B</goodsNm>
                            <goodsCnt>2</goodsCnt>
                            <goodsPrice>10000.00</goodsPrice>
                            <deliveryDt>2026-03-25 09:38:10</deliveryDt>
                        </orderGoodsData>
                    </order_data>
                </return>
            </data>
            """;

        GodoOrderSearchResult result = parser.parse(xml);

        assertThat(result.orders()).hasSize(2);
        assertThat(result.items()).hasSize(2);
    }

    private String orderXml(
        String orderNo, String memId, String memGroupNm, String orderStatus,
        String totalGoodsPrice, String totalDeliveryCharge, String useDeposit, String useMileage,
        String totalGoodsDcPrice, String totalMemberDcPrice,
        String couponGoodsDc, String couponOrderDc, String couponDeliveryDc,
        String itemXml
    ) {
        return """
            <data>
                <header><code>000</code><msg>success</msg></header>
                <return>
                    <order_data idx="1">
                        <orderNo>%s</orderNo>
                        <orderStatus>%s</orderStatus>
                        <memId>%s</memId>
                        <memGroupNm>%s</memGroupNm>
                        <orderDate>2026-03-24 17:52:56</orderDate>
                        <totalGoodsPrice>%s</totalGoodsPrice>
                        <totalDeliveryCharge>%s</totalDeliveryCharge>
                        <useDeposit>%s</useDeposit>
                        <useMileage>%s</useMileage>
                        <totalGoodsDcPrice>%s</totalGoodsDcPrice>
                        <totalMemberDcPrice>%s</totalMemberDcPrice>
                        <totalCouponGoodsDcPrice>%s</totalCouponGoodsDcPrice>
                        <totalCouponOrderDcPrice>%s</totalCouponOrderDcPrice>
                        <totalCouponDeliveryDcPrice>%s</totalCouponDeliveryDcPrice>
                        %s
                    </order_data>
                </return>
            </data>
            """.formatted(
                orderNo, orderStatus, memId, memGroupNm,
                totalGoodsPrice, totalDeliveryCharge, useDeposit, useMileage,
                totalGoodsDcPrice, totalMemberDcPrice, couponGoodsDc, couponOrderDc, couponDeliveryDc,
                itemXml
            );
    }

    private String goodsItem(String sno, String goodsNm, int goodsCnt, String goodsPrice, String deliveryDt) {
        return """
            <orderGoodsData idx="1">
                <sno>%s</sno>
                <goodsNm>%s</goodsNm>
                <goodsCnt>%d</goodsCnt>
                <goodsPrice>%s</goodsPrice>
                <deliveryDt>%s</deliveryDt>
            </orderGoodsData>
            """.formatted(sno, goodsNm, goodsCnt, goodsPrice, deliveryDt);
    }
}
