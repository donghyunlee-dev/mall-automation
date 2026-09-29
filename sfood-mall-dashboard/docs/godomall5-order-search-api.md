# 고도몰 5 주문조회 API

쇼핑몰에서 발생한 주문정보를 조회하는 API입니다.

## 기본 정보

| 항목 | 내용 |
|---|---|
| Real URL | `https://openhub.godo.co.kr/godomall5/order/Order_Search.php` |
| Sandbox URL | `http://sbopenhub.godo.co.kr/godomall5/order/Order_Search.php` |
| 요청 방식 | `POST` |

## Request

### 요청 파라미터

| 변수명 | 타입 | 필수 여부 | 설명 및 비고 |
|---|---|---|---|
| `dateType` | STRING | - | 기간검색 타입. `order`: 주문일, `modify`: 수정일 |
| `startDate` | DATE | 조건부 | 주문일 기준 조회 시작일. `orderNo`가 없으면 필수 |
| `endDate` | DATE | 조건부 | 주문일 기준 조회 종료일. `orderNo`가 없으면 필수 |
| `orderNo` | STRING | 조건부 | 주문번호. `startDate`, `endDate`가 없으면 필수 |
| `orderStatus` | STRING | - | 주문상태 코드 |
| `scmNo` | INTEGER | 사용불가 | 공급사 코드. 공통코드조회 API 참조 |
| `orderChannel` | STRING | - | 주문채널 코드. 생략하면 전체 주문 조회 |
| `searchType` | STRING | - | 검색어 검색 타입. `orderPhone`, `receiverPhone`, `orderCellPhone`, `receiverCellPhone` |
| `searchKeyword` | STRING | - | 검색어. 전화번호 입력 시 하이픈(`-`)을 포함하여 전송 |
| `sort` | STRING | - | 주문 정렬 코드. 생략하면 주문번호 내림차순 |
| `mallSno` | INTEGER | - | 상점번호 |
| `size` | INTEGER | - | 페이지당 주문 건수 |
| `lastOrder` | STRING | - | 다음 페이지 조회 기준 주문번호. `size`가 있을 때만 사용 가능하며, 해당 주문번호보다 작은 주문을 조회 |

### 페이지네이션

`size`를 지정하면 응답의 `lastOrder`로 다음 조회 여부를 확인할 수 있습니다. 다음 요청의 `lastOrder`에 이전 응답에서 사용한 마지막 주문번호를 전달하면 해당 주문번호보다 작은 주문을 조회합니다.

예시: `2501151716000289`를 전달하면 `orderNo < 2501151716000289`인 주문을 조회합니다.

## Response

### 공통 응답

| 변수명 | 설명 |
|---|---|
| `code` | API 연결 성공/실패 코드 |
| `msg` | API 연결 성공 또는 실패 사유 |
| `lastOrder` | 다음 페이지 존재 여부. `true`: 있음, `false`: 없음. `size` 지정 시에만 응답 |
| `order_data` | 주문정보 목록 |

## order_data

| 변수명 | 설명 |
|---|---|
| `orderNo` | 주문번호 |
| `memNo` | 회원번호 |
| `orderStatus` | 주문상태 코드 |
| `orderIp` | 주문자 IP. 사용불가 |
| `orderChannelFl` | 주문채널. 사용불가 |
| `orderTypeFl` | 주문유형(모바일, PC, 수기). 사용불가 |
| `orderEmail` | 이메일. 사용불가 |
| `orderGoodsNm` | 주문상품명 |
| `orderGoodsCnt` | 주문상품 개수 |
| `settlePrice` | 총 주문금액. 사용불가 |
| `taxSupplyPrice` | 최초 총 과세금액. 사용불가 |
| `taxVatPrice` | 최초 총 부가세 금액. 사용불가 |
| `taxFreePrice` | 최초 총 면세금액. 사용불가 |
| `realTaxSupplyPrice` | 실제 총 과세금액(환불 제외). 사용불가 |
| `realTaxVatPrice` | 실제 총 부가세(환불 제외). 사용불가 |
| `realTaxFreePrice` | 실제 총 면세금액(환불 제외). 사용불가 |
| `useMileage` | 주문 시 사용한 마일리지. 사용불가 |
| `useDeposit` | 주문 시 사용한 예치금. 사용불가 |
| `totalGoodsPrice` | 총 상품 금액 |
| `totalDeliveryCharge` | 총 배송비 |
| `totalGoodsDcPrice` | 총 상품 할인 금액. 사용불가 |
| `totalMemberDcPrice` | 총 회원 할인 금액. 사용불가 |
| `totalMemberOverlapDcPrice` | 총 그룹별 회원 중복할인 금액. 사용불가 |
| `totalCouponGoodsDcPrice` | 총 상품쿠폰 할인 금액. 사용불가 |
| `totalCouponOrderDcPrice` | 총 주문쿠폰 할인 금액. 사용불가 |
| `totalCouponDeliveryDcPrice` | 총 배송쿠폰 할인 금액. 사용불가 |
| `totalMileage` | 총 적립 마일리지. 사용불가 |
| `totalGoodsMileage` | 총 상품 적립 마일리지. 사용불가 |
| `totalMemberMileage` | 총 회원 적립 마일리지. 사용불가 |
| `totalCouponGoodsMileage` | 총 상품쿠폰 적립 마일리지. 사용불가 |
| `totalCouponOrderMileage` | 총 주문쿠폰 적립 마일리지. 사용불가 |
| `firstSaleFl` | 첫 구매 여부. `y`: 첫 구매. 사용불가 |
| `settleKind` | 주문 방법. 결제수단 코드 참조. 사용불가 |
| `paymentDt` | 입금일자. 사용불가 |
| `orderDate` | 주문일자 |
| `apiOrderNo` | 외부주문번호(네이버페이, 페이코 등). 사용불가 |
| `addField` | 주문 추가정보 JSON |
| `multiShippingFl` | 복수배송지 사용 여부. `y`: 사용, `n`: 미사용 |
| `bankName` | 입금계좌 은행명. 무통장입금·가상계좌의 입금대기 주문에서만 전달 |
| `accountNumber` | 입금계좌 계좌번호. `bankName`이 있을 때 함께 전달 |
| `depositor` | 입금계좌 예금주. `bankName`이 있을 때 함께 전달 |
| `memId` | 회원 아이디 |
| `memGroupNm` | 회원 그룹명 |
| `orderDeliveryData` | 배송비 정보 |
| `orderInfoData` | 배송지 정보 |
| `addGoodsData` | 추가상품 정보 |
| `giftData` | 사은품 정보 |
| `orderGoodsData` | 주문상품 정보 |
| `orderConsultData` | 요청사항·상담메모 정보 |
| `mallSno` | 상점번호. 사용불가 |
| `appOs` | 앱 주문 시 휴대폰 OS. 사용불가 |
| `pushCode` | 앱 주문 시 푸시 코드. 사용불가 |
| `statisticsAppOrderCntFl` | 앱 통계 주문건수 처리 여부. 사용불가 |
| `orderGoodsNmStandard` | 주문상품명(기준몰) |
| `overseasSettleCurrency` | 해외 PG 승인금액 적용 환율 코드. 사용불가 |
| `overseasSettlePrice` | 해외 PG 승인금액. 사용불가 |
| `totalDeliveryInsuranceFee` | 해외배송 EMS 보험료. 사용불가 |
| `totalMemberBankDcPrice` | 총 회원등급 브랜드 무통장 할인 금액. 사용불가 |
| `totalMemberDeliveryDcPrice` | 총 회원 배송비 할인. 사용불가 |
| `totalMyappDcPrice` | 총 마이앱 할인 금액. 사용불가 |
| `totalEnuriDcPrice` | 총 운영자 추가할인. 사용불가 |
| `totalDeliveryWeight` | 배송 총 무게. 사용불가 |
| `currencyPolicy` | 주문 당시 상점 통화 기본정책. 사용불가 |
| `exchangeRatePolicy` | 주문 당시 환율 기본정책. 사용불가 |
| `myappPolicy` | 주문 당시 마이앱 기본정책. 사용불가 |
| `adminMemo` | 관리자 메모. 사용불가 |
| `pgRealTaxSupplyPrice` | 실제 PG 과세금액. 사용불가 |
| `pgRealTaxVatPrice` | 실제 PG 부가세. 사용불가 |
| `pgRealTaxFreePrice` | 실제 PG 면세금액. 사용불가 |
| `bankdaManualNo` | 뱅크다 번호. 사용불가 |
| `bankdaManualFl` | 자동입금 수동 여부. 사용불가 |
| `bankdaManualMangerId` | 자동입금 수동처리 관리자 아이디. 사용불가 |
| `trackingKey` | 페이코 쇼핑 트래킹키. 사용불가 |
| `userHandleProcess` | 주문 환불·반품·교환 자동 승인 처리 여부. 사용불가 |
| `pgChargeBack` | 차지백 여부. 사용불가 |
| `fbPixelKey` | 페이스북 픽셀 쿠키값. 사용불가 |

### addField 구조

`addField`는 JSON 형식의 주문 추가정보입니다.

| 변수명 | 설명 |
|---|---|
| `name` | 추가항목명 |
| `data` | 추가항목 입력값. 입력방법이 `order`이면 Text, `goods`이면 Array |
| `goodsNm` | 상품명. 입력방법이 `goods`인 경우 존재 |
| `process` | 입력방법. `order`: 공통입력, `goods`: 상품별입력 |

## orderDeliveryData

| 변수명 | 설명 |
|---|---|
| `sno` | 배송비 정보 고유번호 |
| `orderInfoSno` | 배송지 정보 고유번호. `orderInfoData.sno`와 동일하며 복수배송지일 때 존재 |
| `scmNo` | 공급사 번호 |
| `commission` | 배송비 수수료율. 사용불가 |
| `scmAdjustNo` | 공급사 정산 고유번호 |
| `scmAdjustAfterNo` | 공급사 정산 후 환불 정산번호 |
| `deliveryCharge` | 총 배송비(지역별 배송비 포함) |
| `deliveryPolicyCharge` | 총 배송비(지역별 배송비 제외) |
| `deliveryAreaCharge` | 총 지역별 배송비 |
| `deliveryFixFl` | 배송정책. `fixed`: 고정, `free`: 무료, `price`: 가격별, `weight`: 무게별, `count`: 수량별 |
| `deliveryCollectFl` | 배송비 결제방법. `pre`: 선불, `later`: 착불 |
| `deliveryCollectPrice` | 착불 시 발생한 배송비 |
| `deliveryWholeFreePrice` | 동일 배송비 무료조건 적용 시 배송비 |
| `divisionDeliveryUseDeposit` | 주문할인 금액의 안분된 예치금. 사용불가 |
| `divisionDeliveryUseMileage` | 주문할인 금액의 안분된 마일리지. 사용불가 |
| `divisionDeliveryCharge` | 총 배송비 쿠폰 안분 금액. 사용불가 |
| `divisionMemberDeliveryDcPrice` | 회원 배송비 무료 안분 금액. 사용불가 |
| `deliveryInsuranceFee` | EMS 배송 시 적용되는 해외배송 보험료. 사용불가 |
| `deliveryWeightInfo` | 총 배송무게(상품+박스). 사용불가 |
| `overseasDeliveryPolicy` | 주문 당시 해외 배송정책. 사용불가 |
| `statisticsOrderFl` | 주문·매출 통계 처리 상태. 사용불가 |

## orderInfoData

| 변수명 | 설명 |
|---|---|
| `sno` | 배송지 정보 고유번호. 복수배송지일 때 존재 |
| `orderInfoCd` | 배송지 정보 순번. 기본값 `1`, 복수배송지인 경우 증가 |
| `orderName` | 주문자 이름 |
| `orderEmail` | 주문자 이메일 |
| `orderPhone` | 주문자 전화번호 |
| `orderCellPhone` | 주문자 휴대폰 번호 |
| `orderZipcode` | 주문자 우편번호 |
| `orderZonecode` | 주문자 새 우편번호 |
| `orderAddress` | 주문자 주소 |
| `orderAddressSub` | 주문자 나머지 주소 |
| `receiverName` | 수취인 이름 |
| `receiverPhone` | 수취인 전화번호 |
| `receiverCellPhone` | 수취인 휴대폰 번호 |
| `receiverZipcode` | 수취인 우편번호 |
| `receiverZonecode` | 수취인 새 우편번호 |
| `receiverAddress` | 수취인 주소 |
| `receiverAddressSub` | 수취인 나머지 주소 |
| `orderMemo` | 주문 시 남기는 글 |
| `receiverUseSafeNumberFl` | 안심번호 상태. `n`: 사용 안 함, `y`: 사용, `c`: 사용 해지, `w`: 발급 대기 |
| `receiverSafeNumberDt` | 안심번호 발급일시 |
| `receiverSafeNumber` | 안심번호 |
| `orderPhonePrefixCode` | 주문자 전화번호 국가코드 |
| `orderPhonePrefix` | 주문자 전화번호 국가번호 |
| `orderCellPhonePrefixCode` | 주문자 휴대폰 국가코드 |
| `orderCellPhonePrefix` | 주문자 휴대폰 국가번호 |
| `orderState` | 주문자 주·지방·지역 |
| `orderCity` | 주문자 도시 |
| `receiverCountryCode` | 수취인 국가 코드 |
| `receiverPhonePrefixCode` | 수취인 전화번호 국가코드 |
| `receiverPhonePrefix` | 수취인 전화번호 국가번호 |
| `receiverCellPhonePrefixCode` | 수취인 휴대폰 국가코드 |
| `receiverCellPhonePrefix` | 수취인 휴대폰 국가번호 |
| `receiverCountry` | 수취인 국가 |
| `receiverState` | 수취인 주·지방·지역 |
| `receiverCity` | 수취인 도시 |
| `deliveryVisit` | 배송방법. `y`: 방문배송, `n`: 방문배송 제외, `a`: 방문배송 포함 |
| `visitAddress` | 방문 수령지 주소 |
| `visitName` | 방문자명 |
| `visitPhone` | 방문자 연락처 |
| `visitMemo` | 방문수령 메모 |
| `packetCode` | 묶음배송코드 |
| `smsFl` | SMS 수신 동의 여부 |

## addGoodsData

추가상품 정보입니다. 상품 관련 필드는 `orderGoodsData`와 유사하며 추가상품 식별을 위한 필드가 포함됩니다.

| 변수명 | 설명 |
|---|---|
| `sno` | 주문 추가상품 고유번호 |
| `orderNo` | 주문번호 |
| `apiOrderGoodsNo` | 외부채널 품목 고유번호 |
| `orderCd` | 주문순서 |
| `orderGroupCd` | 수량 부분취소 시 그룹번호 |
| `orderStatus` | 주문상태 |
| `orderDeliverySno` | 배송비정보 고유번호 |
| `invoiceCompany` | 택배사명 |
| `invoiceCompanySno` | 택배사 번호 |
| `invoiceNo` | 송장번호 |
| `scmNo` | 공급사 번호 |
| `commission` | 공급사 수수료율. 사용불가 |
| `addGoodsNo` | 추가상품번호 |
| `parentGoodsNo` | 부모상품 상품코드 |
| `goodsCd` | 상품코드 |
| `listImageData` | 상품 썸네일 이미지 URL |
| `goodsModelNo` | 모델명 |
| `goodsNm` | 상품명 |
| `goodsCnt` | 구매수량 |
| `goodsPrice` | 상품가격 |
| `addGoodsPrice` | 추가상품금액 |
| `optionPrice` | 옵션금액 |
| `optionTextPrice` | 입력옵션금액 |
| `fixedPrice` | 정가 |
| `costPrice` | 매입가 |
| `optionSno` | 상품옵션 고유번호. 사용불가 |
| `optionInfo` | 옵션정보 JSON. 사용불가 |
| `optionTextInfo` | 입력옵션정보 JSON. 사용불가 |
| `cancelDt` | 취소일자 |
| `paymentDt` | 입금일자. 사용불가 |
| `invoiceDt` | 송장번호 등록일 |
| `deliveryDt` | 배송일자 |
| `deliveryCompleteDt` | 배송완료일자 |
| `finishDt` | 구매확정일자 |
| `enuri` | 에누리 |
| `claimData` | 클레임 정보 |
| `goodsNmStandard` | 기준몰 상품명 |
| `brandCd` | 브랜드코드 |

그 외 할인·마일리지·재고·통계·배송 관련 상세 필드는 원문에서 대부분 공급사 사용불가로 정의되어 있습니다.

### 상품·추가상품 공통 상세 필드

다음 필드는 `addGoodsData` 또는 `orderGoodsData`에 포함될 수 있습니다.

| 변수명 | 설명 |
|---|---|
| `divisionUseMileage` | 주문할인 금액의 안분된 마일리지. 사용불가 |
| `divisionGoodsDeliveryUseDeposit` | 주문할인 금액의 안분된 배송비 예치금. 사용불가 |
| `divisionGoodsDeliveryUseMileage` | 주문할인 금액의 안분된 배송비 마일리지. 사용불가 |
| `divisionCouponOrderDcPrice` | 주문할인 금액의 안분된 주문쿠폰. 사용불가 |
| `divisionUseDeposit` | 주문할인 금액의 안분된 예치금. 사용불가 |
| `divisionCouponOrderMileage` | 주문에 지급될 마일리지의 안분된 마일리지. 사용불가 |
| `goodsDcPrice` | 쿠폰할인금액. 사용불가 |
| `memberDcPrice` | 회원할인금액(추가상품 제외). 사용불가 |
| `memberOverlapDcPrice` | 회원그룹 중복할인 금액(추가상품 제외). 사용불가 |
| `couponGoodsDcPrice` | 상품쿠폰 할인 금액. 사용불가 |
| `goodsMileage` | 적립 마일리지(추가상품 제외). 사용불가 |
| `memberMileage` | 회원 적립 마일리지(추가상품 제외). 사용불가 |
| `couponGoodsMileage` | 상품쿠폰 적립 마일리지(추가상품 제외). 사용불가 |
| `minusDepositFl` | 사용 예치금 차감 여부. `n`: 미차감, `y`: 차감. 사용불가 |
| `minusRestoreDepositFl` | 사용 예치금 복원 여부. `n`: 미복원, `y`: 복원. 사용불가 |
| `minusMileageFl` | 사용 마일리지 차감 여부. `n`: 미차감, `y`: 차감. 사용불가 |
| `minusRestoreMileageFl` | 사용 마일리지 복원 여부. `n`: 미복원, `y`: 복원. 사용불가 |
| `plusMileageFl` | 적립 마일리지 차감 여부. `n`: 미차감, `y`: 차감. 사용불가 |
| `plusRestoreMileageFl` | 적립 마일리지 복원 여부. `n`: 미복원, `y`: 복원. 사용불가 |
| `minusStockFl` | 재고 차감 여부. `n`: 미차감, `y`: 차감. 사용불가 |
| `minusRestoreStockFl` | 재고 복원 여부. `n`: 미복원, `y`: 복원. 사용불가 |
| `goodsType` | 주문상품 종류. 사용불가 |
| `timeSaleFl` | 타임세일 구매 여부. 사용불가 |
| `parentMustFl` | 추가상품 종속성 여부. 사용불가 |
| `optionCostPrice` | 옵션 매입가. 사용불가 |
| `timeSalePrice` | 타임세일 할인 금액. 상품에만 적용. 사용불가 |
| `brandBankSalePrice` | 브랜드 무통장결제 세일 할인 금액. 상품에만 적용. 사용불가 |
| `myappDcPrice` | 마이앱 할인 금액. 추가상품 제외. 사용불가 |
| `goodsDeliveryCollectPrice` | 상품별 착불 시 발생한 배송비. 사용불가 |
| `goodsDeliveryCollectFl` | 상품별 배송비 결제방법. `pre`: 선불, `later`: 착불. 사용불가 |
| `cateAllCd` | 상품에 연결된 전체 카테고리 코드. 사용불가 |
| `hscode` | HS code. 사용불가 |
| `mileageGiveDt` | 마일리지 지급 유예에 따른 실제 지급일. 사용불가 |
| `checkoutData` | 간편구매 추가 데이터. 사용불가 |
| `statisticsOrderFl` | 주문·매출 통계 처리 상태. 사용불가 |
| `statisticsGoodsFl` | 상품 통계 처리 상태. 사용불가 |
| `sendSmsFl` | 문자 발송 여부. 사용불가 |
| `deliveryMethodFl` | 배송방식. 사용불가 |
| `goodsDiscountInfo` | 주문 당시 상품 할인정보. 사용불가 |
| `goodsMileageAddInfo` | 주문 당시 상품 적립정보. 사용불가 |
| `inflow` | 외부 인입 플랫폼. 사용불가 |
| `linkMainTheme` | 메인 상품 진열에서 장바구니에 담은 정보. 사용불가 |
| `visitAddress` | 방문 수령지 주소. 사용불가 |
| `goodsVolume` | 상품 용량. 사용불가 |
| `couponMileageFl` | 쿠폰으로 적립되는 마일리지의 품목별 지급 현황. 사용불가 |
| `easypayScmReceiptFl` | 이지페이 하위가맹점 등록 여부. 사용불가 |
| `deliveryScheduleFl` | 배송일정 사용 여부. 사용불가 |

## giftData

| 변수명 | 설명 |
|---|---|
| `sno` | 주문-사은품 매칭 고유번호 |
| `presentTitle` | 사은품 지급조건명 |
| `giftNo` | 사은품번호 |
| `giftCd` | 사은품코드 |
| `giftNm` | 사은품명 |
| `giftCnt` | 사은품 지급수량 |

## orderGoodsData

주문상품 정보입니다. `addGoodsData`와 대부분 동일한 상품·할인·재고·배송 필드를 가지며, 다음 주문상품 고유 필드를 포함합니다.

| 변수명 | 설명 |
|---|---|
| `sno` | 주문상품 고유번호 |
| `orderNo` | 주문번호 |
| `apiOrderGoodsNo` | 외부채널 품목 고유번호 |
| `orderCd` | 주문순서 |
| `orderGroupCd` | 수량 부분취소 시 그룹번호 |
| `orderStatus` | 주문상태 |
| `orderDeliverySno` | 배송비정보 고유번호 |
| `invoiceCompany` | 택배사명 |
| `invoiceCompanySno` | 택배사 번호 |
| `invoiceNo` | 송장번호 |
| `scmNo` | 공급사 번호 |
| `commission` | 공급사 수수료율. 사용불가 |
| `goodsNo` | 상품번호 |
| `goodsCd` | 상품코드 |
| `listImageData` | 상품 썸네일 이미지 URL |
| `goodsModelNo` | 모델명 |
| `goodsNm` | 상품명 |
| `goodsCnt` | 구매수량 |
| `goodsPrice` | 상품가격 |
| `addGoodsPrice` | 추가상품금액 |
| `optionPrice` | 옵션금액 |
| `optionTextPrice` | 입력옵션금액 |
| `fixedPrice` | 정가 |
| `costPrice` | 매입가 |
| `optionSno` | 상품옵션 고유번호. 사용불가 |
| `optionInfo` | 옵션정보 JSON |
| `optionTextInfo` | 입력옵션정보 JSON |
| `cancelDt` | 취소일자 |
| `paymentDt` | 입금일자. 사용불가 |
| `invoiceDt` | 송장번호 등록일 |
| `deliveryDt` | 배송일자 |
| `deliveryCompleteDt` | 배송완료일자 |
| `finishDt` | 구매확정일자 |
| `enuri` | 에누리 |
| `claimData` | 클레임 정보 |
| `parentGoodsNo` | 추가상품의 부모상품 |
| `goodsNmStandard` | 기준몰 상품명 |
| `brandCd` | 브랜드코드 |

주문상품 응답에는 이외에도 상품 할인금액, 적립 마일리지, 예치금·재고 차감 여부, 배송 방식, 외부 인입 플랫폼 등의 필드가 포함됩니다. 원문상 `사용불가`로 표시된 필드는 공급사에서 사용할 수 없습니다.

## claimData

| 변수명 | 설명 |
|---|---|
| `beforeStatus` | 이전 주문상태 |
| `handleMode` | 클레임 모드. `r`: 환불접수, `b`: 반품접수, `e`: 교환접수, `z`: 교환 추가, `c`: 취소 |
| `handleCompleteFl` | 처리완료 여부. `y`: 환불완료, `n`: 환불접수 |
| `handleReason` | 클레임 사유 |
| `handleDetailReason` | 클레임 상세사유 |
| `handleDt` | 처리완료일자 |
| `refundPrice` | 결제 환불금액 |
| `refundUseDeposit` | 예치금 환불금액 |
| `refundUseMileage` | 적립금 환원금액 |
| `refundDeliveryCharge` | 배송비 환불금액 |
| `refundCharge` | 환불수수료 |
| `exchangeInfoData` | 다른 상품 교환 차액정보 |
| `handleDetailReasonShowFl` | 처리상세사유 출력 여부 |
| `refundDeliveryUseDeposit` | 배송비에 할당된 사용 예치금 환원 |
| `refundDeliveryUseMileage` | 배송비에 할당된 사용 마일리지 환원 |
| `refundDeliveryInsuranceFee` | 환불 해외배송 보험료 |
| `refundDeliveryCoupon` | 취소 배송비쿠폰 금액 |
| `handleGroupCd` | 클레임 1회 처리당 그룹 |
| `refundUseDepositCommission` | 사용 예치금 수수료 |
| `refundUseMileageCommission` | 사용 마일리지 수수료 |
| `regDt` | 등록일 |

## exchangeInfoData

| 변수명 | 설명 |
|---|---|
| `ehDifferencePrice` | 총 차액 |
| `ehCancelDeliveryPrice` | 취소된 배송비 |
| `ehAddDeliveryPrice` | 추가된 배송비 |
| `ehRefundMethod` | 환불수단 |
| `ehRefundName` | 환불 예금주 |
| `ehRefundBankName` | 환불 은행명 |
| `ehRefundBankAccountNumber` | 환불 계좌번호 |
| `ehSettleName` | 추가결제 입금자명 |
| `ehSettleBankAccountInfo` | 추가결제 은행정보 |
| `ehEnuri` | 에누리 |

## orderConsultData

| 변수명 | 설명 |
|---|---|
| `sno` | 요청사항·상담메모 고유번호 |
| `orderNo` | 주문번호 |
| `requestMemo` | 요청사항 |
| `consultMemo` | 상담메모 |
| `regDt` | 등록일 |
| `modDt` | 수정일 |

## 조회 조건 요약

- `orderNo`로 조회하는 경우 `startDate`, `endDate` 없이 조회할 수 있습니다.
- `orderNo` 없이 기간으로 조회하는 경우 `startDate`, `endDate`가 필요합니다.
- `orderChannel`을 생략하면 전체 주문을 조회합니다.
- `sort`를 생략하면 주문번호 내림차순으로 정렬됩니다.
- `size`를 사용하면 `lastOrder` 기반의 페이지네이션이 가능합니다.
- 전화번호 검색 시 `searchType`과 `searchKeyword`를 함께 사용하며, 전화번호는 하이픈을 포함해 전달합니다.

> 출처: `godomall5_openAPI_spec_v1.0_20250616` 중 4.1 주문조회 API(원문 50~62페이지)
