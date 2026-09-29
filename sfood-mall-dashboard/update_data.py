import os
import requests
import xml.etree.ElementTree as ET
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from io import BytesIO
from dotenv import load_dotenv
from supabase import create_client, Client
from pytz import timezone

# 1. 환경 변수 로드
load_dotenv()

API_DOMAIN = os.getenv("API_DOMAIN")
PARTNER_KEY = os.getenv("PARTNER_KEY")
KEY = os.getenv("KEY")
SUPABASE_URL = os.getenv("SUPABASE_URL")
SUPABASE_KEY = os.getenv("SUPABASE_KEY")

if not all([API_DOMAIN, PARTNER_KEY, KEY, SUPABASE_URL, SUPABASE_KEY]):
    raise ValueError("❌ .env 파일에 필수 키가 누락되었습니다.")

session = requests.Session()
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)
BUCKET_NAME = "dashboard-data"
MASTER_FILE = "latest_sales.parquet"


def get_text(node, tag, default=""):
    if node is None: return default
    target = node.find(tag)
    return target.text.strip() if target is not None and target.text else default


def call_godo_api(endpoint, params=None):
    url = f"{API_DOMAIN}/godomall5/{endpoint}"
    data = {'partner_key': PARTNER_KEY, 'key': KEY}
    if params: data.update(params)
    try:
        response = session.post(url, data=data, timeout=180)
        response.raise_for_status()
        root = ET.fromstring(response.text)
        header = root.find('header')
        code_node = header.find('code') if header is not None else None
        if code_node is not None and (code_node.text or '').strip() == '000':
            return root
        return None
    except Exception as e:
        print(f"❌ API Request Failed: {e}")
        return None


def parse_orders_from_xml(result_xml):
    """XML에서 주문 행 목록을 파싱하여 반환 (공통 파싱 로직)"""
    all_rows = []
    if result_xml is None:
        return all_rows

    for order in result_xml.findall('.//order_data'):
        base_info = {
            "주문 번호": get_text(order, 'orderNo'),
            "주문상태": get_text(order, 'orderStatus'),
            "회원 아이디": get_text(order, 'memId'),
            "회원등급명": get_text(order, 'memGroupNm'),
            "총 결제 금액": get_text(order, 'settlePrice', "0"),
            "사용된 총 마일리지": get_text(order, 'useMileage', "0"),
            "사용된 총 예치금": get_text(order, 'useDeposit', "0"),
            "총 품목 금액": get_text(order, 'totalGoodsPrice', "0"),
            "총 배송 금액": get_text(order, 'totalDeliveryCharge', "0"),
            "총 상품 할인 금액": get_text(order, 'totalGoodsDcPrice', "0"),
            "총 회원 할인 금액": get_text(order, 'totalMemberDcPrice', "0"),
            "총 상품쿠폰 할인 금액": get_text(order, 'totalCouponGoodsDcPrice', "0"),
            "총 주문쿠폰 할인 금액": get_text(order, 'totalCouponOrderDcPrice', "0"),
            "총 배송쿠폰 할인 금액": get_text(order, 'totalCouponDeliveryDcPrice', "0"),
            "주문일자": get_text(order, 'orderDate'),
            "결제일자": get_text(order, 'paymentDt'),
        }
        info = order.find('.//orderInfoData')
        base_info["회원명"] = get_text(info, 'orderName')
        base_info["주문자 핸드폰 번호"] = get_text(info, 'orderCellPhone')

        for item in order.findall('.//orderGoodsData'):
            row = base_info.copy()
            row["상품명"] = get_text(item, 'goodsNm')
            row["판매가"] = get_text(item, 'goodsPrice', "0")
            row["상품수량"] = get_text(item, 'goodsCnt', "0")
            row["배송일자"] = get_text(item, 'deliveryDt')
            all_rows.append(row)

    return all_rows


def fetch_orders_by_date(target_date):
    """어제 신규 주문 수집"""
    print(f"🔄 [{target_date}] 고도몰 신규 주문 수집 중...")
    endpoint = "order/Order_Search.php"
    params = {'dateType': 'order', 'startDate': target_date, 'endDate': target_date}
    result_xml = call_godo_api(endpoint, params)
    return pd.DataFrame(parse_orders_from_xml(result_xml))


def fetch_orders_for_refresh(cutoff_dt: pd.Timestamp, yesterday_dt: pd.Timestamp):
    """최근 21일치 주문을 한 번에 재수집 (어제 포함)"""
    start_date = cutoff_dt.strftime('%Y-%m-%d')
    end_date = yesterday_dt.strftime('%Y-%m-%d')

    print(f"🔁 최근 21일치 주문 재수집 중... ({start_date} ~ {end_date}, API 1회 호출)")
    endpoint = "order/Order_Search.php"
    params = {'dateType': 'order', 'startDate': start_date, 'endDate': end_date}
    result_xml = call_godo_api(endpoint, params)
    return pd.DataFrame(parse_orders_from_xml(result_xml))


def preprocess_orders(df):
    if df.empty: return df
    print("🛠 데이터 전처리 중...")

    # 1. 상태 필터링
    # status_map = {  # 기존
    #     'o1': '입금대기', 'p1': '결제완료', 'g1': '상품준비중', 'g2': '구매발주',
    #     'g3': '상품입고', 'g4': '상품출고', 'd1': '배송중', 'd2': '배송완료',
    #     's1': '구매확정', 'e1': '교환접수', 'e2': '교환반송중', 'e3': '재배송중',
    #     'e4': '교환보류', 'e5': '교환완료', 'z1': '추가입금대기', 'z2': '추가결제완료',
    #     'z3': '추가배송중', 'z4': '추가배송완료', 'z5': '교환추가완료'
    # }
    status_map = {'o1': '입금대기',
        'p1': '결제완료',
        'g1': '상품준비중',
        'd1': '배송중',
        'd2': '배송완료',
        'f2': '고객결제중단',
        'f4': 'PG 확인요망',
        's1': '구매확정',
        'e1': '교환접수',
        'e5': '교환완료'}
    df = df[df['주문상태'].isin(status_map.keys())].copy()
    df['주문상태'] = df['주문상태'].map(status_map)

    # 2. 숫자형 변환
    float_cols = [
        '총 결제 금액', '사용된 총 마일리지', '사용된 총 예치금', '총 품목 금액',
        '총 배송 금액', '총 상품 할인 금액', '총 회원 할인 금액', '판매가',
        '총 상품쿠폰 할인 금액', '총 주문쿠폰 할인 금액', '총 배송쿠폰 할인 금액'
    ]
    for col in float_cols:
        df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0.0).astype(float)

    int_cols = ['주문 번호', '상품수량']
    for col in int_cols:
        df[col] = pd.to_numeric(df[col], errors='coerce').fillna(0).astype('int64')

    # 3. 쿠폰 할인 통합
    df['총 쿠폰 할인 금액'] = (
        df['총 상품쿠폰 할인 금액'] +
        df['총 주문쿠폰 할인 금액'] +
        df['총 배송쿠폰 할인 금액']
    )
    df = df.drop(['총 상품쿠폰 할인 금액', '총 주문쿠폰 할인 금액', '총 배송쿠폰 할인 금액'], axis=1)

    # 4. 날짜 파싱
    date_cols = ['주문일자', '결제일자', '배송일자']
    for col in date_cols:
        df[col] = df[col].astype(str).replace({r'.*0000.*': None}, regex=True)
        df[col] = pd.to_datetime(df[col], errors='coerce')

    # 5. 요일 파생
    weekday_dict = {0: '월', 1: '화', 2: '수', 3: '목', 4: '금', 5: '토', 6: '일'}
    df['주문요일'] = df['주문일자'].dt.weekday.map(weekday_dict)
    df['결제요일'] = df['결제일자'].dt.weekday.map(weekday_dict)
    df['배송요일'] = df['배송일자'].dt.weekday.map(weekday_dict)

    # 6. 회원 구분
    df['회원등급명'] = df['회원등급명'].replace('', '비회원').fillna('비회원')
    df['회원 아이디'] = df['회원 아이디'].replace({'': None, 'nan': None})
    df['구분자_비회원포함'] = np.where(
        df['회원 아이디'].isna(),
        df['회원명'] + '_' + df['주문자 핸드폰 번호'],
        df['회원 아이디']
    )
    df['회원등급명_요약'] = np.where(
        df['회원등급명'].str.contains('사업자', na=False),
        '사업자회원',
        df['회원등급명']
    )

    # 7. 매출 지표
    df['상품별 단순 총매출'] = df['판매가'] * df['상품수량']
    df['결제_final'] = (
        df['총 품목 금액'] + df['총 배송 금액']
        - df['사용된 총 예치금'] - df['사용된 총 마일리지']
        - df['총 상품 할인 금액'] - df['총 회원 할인 금액']
        - df['총 쿠폰 할인 금액']
    ) * (10 / 11)

    return df


def upsert_new_data(df_old: pd.DataFrame, df_new: pd.DataFrame) -> pd.DataFrame:
    """주문번호+상품명을 key로 upsert. df_new의 행이 df_old에 있으면 덮어쓰고, 없으면 추가."""
    if df_old.empty:
        return df_new
    if df_new.empty:
        return df_old

    KEY_COLS = ['주문 번호', '상품명']
    original_columns = df_old.columns.tolist()  # 컬럼 순서 기준 보존

    df_old_indexed = df_old.set_index(KEY_COLS)
    df_new_indexed = df_new.set_index(KEY_COLS)

    df_old_filtered = df_old_indexed[~df_old_indexed.index.isin(df_new_indexed.index)]
    df_combined = pd.concat([df_old_filtered, df_new_indexed]).reset_index()

    # 컬럼 순서 고정
    df_combined = df_combined[original_columns]

    return df_combined


def download_existing_data():
    print(f"📥 기존 데이터({MASTER_FILE}) 다운로드 중...")
    try:
        data = supabase.storage.from_(BUCKET_NAME).download(MASTER_FILE)
        df_old = pd.read_parquet(BytesIO(data))
        print(f"✅ 기존 데이터 로드 성공: {len(df_old)}행")
        return df_old
    except Exception as e:
        err = str(e).lower()
        if '404' in err or 'not found' in err:
            print("⚠️ 기존 파일 없음. 빈 데이터프레임 생성.")
            return pd.DataFrame()
        raise


def upload_merged_data(df):
    print(f"🚀 통합 데이터 업로드 중... (총 {len(df)}행)")
    buffer = BytesIO()
    df.to_parquet(buffer, index=False)
    buffer_bytes = buffer.getvalue()
    supabase.storage.from_(BUCKET_NAME).upload(
        path=MASTER_FILE,
        file=buffer_bytes,
        file_options={"upsert": "true", "content-type": "application/octet-stream"}
    )
    print("✅ 업데이트 완료! (Overwritten)")


def main():
    kst = timezone('Asia/Seoul')
    yesterday = (datetime.now(kst) - timedelta(days=1)).strftime('%Y-%m-%d')
    yesterday_dt = pd.Timestamp(yesterday)
    cutoff_dt = yesterday_dt - pd.Timedelta(days=21)
    print(f"📅 작업 기준일(KST): {yesterday}")

    # 1. 기존 누적 데이터 로드
    df_old = download_existing_data()

    # 2. 어제 신규 주문 수집 및 전처리
    df_new = fetch_orders_by_date(yesterday)
    df_new = preprocess_orders(df_new)

    # 3. 최근 21일치 주문 재수집 및 전처리 (API 1회 호출)
    df_refresh = pd.DataFrame()
    if not df_old.empty:
        df_refresh = fetch_orders_for_refresh(cutoff_dt, yesterday_dt)
        df_refresh = preprocess_orders(df_refresh)
        if not df_refresh.empty:
            print(f"✅ 재수집 완료: {len(df_refresh)}행")
        else:
            print("✅ 재수집 결과 없음.")

    # 4. 신규/재수집 데이터가 모두 없으면 기존 데이터 유지하고 종료
    if df_new.empty and df_refresh.empty:
        print("⚠️ 신규 및 재수집 데이터 없음. 기존 데이터 유지하고 종료.")
        return

    # 5. 병합 (삭제 후 재삽입 방식)
    # 순서: 기존에서 21일치 제거 → 재수집으로 교체 → 어제 신규 추가
    if not df_old.empty and not df_refresh.empty:
        df_old_trimmed = df_old[df_old['주문일자'].notna() & (df_old['주문일자'] < cutoff_dt)]
        df_combined = pd.concat([df_old_trimmed, df_refresh], ignore_index=True)
    elif df_old.empty:
        df_combined = df_refresh
    else:
        df_combined = df_old
    df_combined = upsert_new_data(df_combined, df_new)

    # 6. 업로드
    upload_merged_data(df_combined)


if __name__ == "__main__":
    main()