import streamlit as st
import pandas as pd
from pandas.tseries.offsets import MonthEnd
import numpy as np
from supabase import create_client
from io import BytesIO
import plotly.express as px
import plotly.graph_objects as go
from datetime import datetime, timedelta
from dateutil.relativedelta import relativedelta
import time

# -----------------------------------------------------------------------------
# 1. 페이지 설정 및 스타일
# -----------------------------------------------------------------------------

st.set_page_config(layout="wide", page_title="에쓰푸드몰 대시보드", page_icon="💰")

st.markdown("""
<style>
    .block-container {padding-top: 2rem; padding-bottom: 2rem;}
    [data-testid="stMetricValue"] {font-size: 1.8rem;}
            
    /* metric의 라벨(제목) 크기 조절 */
    [data-testid="stMetricLabel"] p {
        font-size: 15px !important;
        line-height: 1.2 !important;
        color: #555 !important; /* 약간 회색으로 처리해 대비를 낮춤 */
    }
    /* metric의 실제 숫자(값) 크기 조절 */
    [data-testid="stMetricValue"] {
        font-size: 24px !important;
    }
</style>
""", unsafe_allow_html=True)

def gray_memo(text, icon="📝"):
    st.markdown(f"""
        <div style="
            display: flex;
            align-items: center;
            gap: 12px;
            background-color: #F8F9FA;
            color: #495057;
            border-left: 5px solid #DEE2E6;
            border-radius: 10px;
            padding: 0px 20px;
            height: 40px;            /* selectbox 표준 높이 */
            box-sizing: border-box;
            font-size: 14px;
            box-shadow: 0 1px 2px rgba(0,0,0,0.05);
            /* 하단 마진을 살짝 주어 selectbox의 위치와 바닥을 맞춤 */
            margin-bottom: 17px;      
        ">
            <span style="font-size: 18px; flex-shrink: 0;">{icon}</span>
            <span style="white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
                {text}
            </span>
        </div>
    """, unsafe_allow_html=True)

# -----------------------------------------------------------------------------
# 2. 비밀번호 세팅
# -----------------------------------------------------------------------------

if "ACCESS_PASSWORD" in st.secrets:
    if "authenticated" not in st.session_state:
        st.session_state["authenticated"] = False

    if not st.session_state["authenticated"]:
        st.markdown("<br><br><br>", unsafe_allow_html=True) 
        
        col1, col2, col3 = st.columns([1, 2, 1])
        
        with col2:
            with st.form(key='login_form'):
                st.markdown("##### 접속을 위해 비밀번호를 입력하세요.")
                
                input_password = st.text_input("접속 비밀번호", type="password")
                
                submit_button = st.form_submit_button(label="로그인")
                
                if submit_button:
                    if input_password == st.secrets["ACCESS_PASSWORD"]:
                        st.session_state["authenticated"] = True
                        st.success("인증 성공! 접속 중...")
                        time.sleep(1)
                        st.rerun()
                    else:
                        st.error("비밀번호가 일치하지 않습니다.")
        
        st.stop()

# -----------------------------------------------------------------------------
# 3. 데이터 세팅
# -----------------------------------------------------------------------------

# 연결 설정
@st.cache_resource
def init_connection():
    url = st.secrets["supabase"]["url"]
    key = st.secrets["supabase"]["key"]
    return create_client(url, key)

supabase = init_connection()

# 데이터 로드 (캐싱)
@st.cache_data(ttl=3600, show_spinner=False) # 1시간마다 갱신 (어차피 어제 데이터라 하루 한번도 됨)
def load_parquet_data():
    bucket_name = "dashboard-data"
    file_path = "latest_sales.parquet"
    
    # 1. Storage에서 파일 다운로드 (Bytes 형태)
    response = supabase.storage.from_(bucket_name).download(file_path)
    
    # 2. Bytes -> Pandas DataFrame 변환
    df = pd.read_parquet(BytesIO(response))
    return df

df = load_parquet_data()
df = df[~df['상품명'].str.contains('선물')]

##### 개발 시에는 로컬에서 parquet 불러와서 사용
# df = pd.read_parquet('../정상업뎃 검증용/latest_sales.parquet')


# -----------------------------------------------------------------------------
# 4. 공통 차트 그리기 함수 (시각화 로직 통합)
# -----------------------------------------------------------------------------
def plot_combo_chart(x_data, bar_data, line_data, x_title, bar_name, line_name, title,
                     bar_fmt='%{y:,.0f}', line_fmt='%{y:,.0f}'):
    """막대(좌측축) + 라인(우측축) 콤보 차트"""
    fig = go.Figure()

    # 막대 그래프 (매출액 등)
    fig.add_trace(go.Bar(
        x=x_data, y=bar_data, name=bar_name, marker_color='#636EFA', yaxis='y',
        hovertemplate=f'<b>{bar_name}</b>: {bar_fmt}<extra></extra>'
    ))

    # 라인 그래프 (주문건수, 객단가 등)
    fig.add_trace(go.Scatter(
        x=x_data, y=line_data, name=line_name, mode='lines+markers',
        marker_color='#EF553B', yaxis='y2',
        hovertemplate=f'<b>{line_name}</b>: {line_fmt}<extra></extra>'
    ))

    fig.update_layout(
        title=dict(text=title, x=0, font=dict(size=16)),
        xaxis=dict(title=x_title, spikecolor='#D1D5DB', spikedash='solid', spikethickness=1),
        yaxis=dict(title=bar_name, showgrid=False),
        yaxis2=dict(title=line_name, overlaying='y', side='right', showgrid=False),
        legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1),
        margin=dict(l=20, r=20, t=50, b=20),
        height=350,
        hovermode='x unified'
    )
    return fig

def plot_horizontal_bar(df, x_col, y_col, title):
    """가로 막대 그래프 (인기 품목)"""
    fig = px.bar(df, x=x_col, y=y_col, orientation='h', title=title, text_auto='.2s')
    fig.update_traces(hovertemplate='<b>%{y}</b>: %{x:,.0f}<extra></extra>')
    fig.update_layout(yaxis={'categoryorder':'total ascending'}, margin=dict(l=20, r=20, t=40, b=20), height=350)
    return fig

def add_details_summary_row(df, avg_cols):
    """등급별 상세 지표 테이블 하단에 합계 행 추가 (avg_cols는 '-' 표시)"""
    row = {'회원등급': '합계'}
    for col in df.columns[1:]:
        row[col] = np.nan if col in avg_cols else int(df[col].sum())
    return pd.concat([df, pd.DataFrame([row])], ignore_index=True)

def _bold_last_row(df):
    """마지막 행(집계 행) 볼드 처리"""
    styles = pd.DataFrame('', index=df.index, columns=df.columns)
    styles.iloc[-1, :] = 'font-weight: bold'
    return styles

def _fmt(fmt_str):
    """None/NaN은 빈칸, 숫자는 fmt_str로 포맷"""
    def _f(x):
        if x is None or (isinstance(x, float) and np.isnan(x)):
            return '-'
        return fmt_str.format(x)
    return _f

def get_style_format(df, is_float=False):
    """pivot 테이블 스타일 포맷 반환 (첫 번째 컬럼 제외, NaN → '-')"""
    fmt_str = "{:,.1f}" if is_float else "{:,.0f}"
    return {col: _fmt(fmt_str) for col in df.columns[1:]}

@st.fragment
def render_monthly_detail(data_map):
    """월간 탭 — 일별 실적 상세 (segmented_control + dataframe)"""
    selection = st.segmented_control(
        "조회 항목 선택", options=list(data_map.keys()),
        selection_mode="single", default=list(data_map.keys())[0],
        key='monthly_select', label_visibility='collapsed'
    )
    if selection in data_map:
        df_view, is_float, is_avg = data_map[selection]
        agg_row = {'회원등급': '합계'}
        for col in df_view.columns[1:]:
            agg_row[col] = np.nan if is_avg else df_view[col].sum()
        df_display = pd.concat([df_view, pd.DataFrame([agg_row])], ignore_index=True)
        st.dataframe(
            df_display.style.apply(_bold_last_row, axis=None).format(get_style_format(df_display, is_float=is_float)),
            hide_index=True, use_container_width=True
        )
        st.write('')
    st.write('')

@st.fragment
def render_yearly_detail(data_map):
    """월간 탭 — 월별 실적 상세 (segmented_control + dataframe + 등급별 라인 차트)"""
    selection = st.segmented_control(
        "조회 항목 선택", options=list(data_map.keys()),
        selection_mode="single", default=list(data_map.keys())[0],
        key='yearly_select', label_visibility='collapsed'
    )
    if selection in data_map:
        df_view, is_float, is_avg = data_map[selection]
        agg_row = {'회원등급': '합계'}
        for col in df_view.columns[1:]:
            agg_row[col] = np.nan if is_avg else df_view[col].sum()
        df_display = pd.concat([df_view, pd.DataFrame([agg_row])], ignore_index=True)
        st.dataframe(
            df_display.style.apply(_bold_last_row, axis=None).format(get_style_format(df_display, is_float=is_float)),
            hide_index=True, use_container_width=True
        )
        st.write('')

        grade_fmt_map = {
            "매출액":    "%{y:,.0f}천원",
            "주문 건수": "%{y:,.0f}건",
            "주문자 수": "%{y:,.0f}명",
            "건당 수량": "%{y:.1f}",
            "건당 SKU":  "%{y:.1f}",
            "AOV":       "%{y:,.0f}원",
            "ARPPU":     "%{y:,.0f}원"
        }
        grade_fmt = grade_fmt_map.get(selection, "%{y:,.0f}")

        df_line = df_view.melt(id_vars='회원등급', var_name='배송연월', value_name=selection)
        fig_grade_line = px.line(
            df_line, x='배송연월', y=selection, color='회원등급',
            markers=True, title=f"등급별 {selection} 추이 (위 테이블에 연동)"
        )
        fig_grade_line.update_traces(
            hovertemplate=f'<b>%{{fullData.name}}</b>: {grade_fmt}<extra></extra>'
        )
        fig_grade_line.update_layout(
            title=dict(x=0, font=dict(size=16)),
            xaxis=dict(type='category', title=None, showgrid=False, spikecolor='#D1D5DB', spikedash='solid', spikethickness=1),
            yaxis=dict(title=None, showgrid=False),
            legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1),
            margin=dict(l=20, r=20, t=50, b=20),
            height=400,
            hovermode='x unified'
        )
        st.plotly_chart(fig_grade_line, use_container_width=True, key='grade_line_yearly')

@st.fragment
def render_q_monthly_detail(data_map):
    """분기 탭 — 월별 실적 상세 (segmented_control + dataframe)"""
    selection = st.segmented_control(
        "조회 항목 선택", options=list(data_map.keys()),
        selection_mode="single", default=list(data_map.keys())[0],
        key='quarterly_detail_select', label_visibility='collapsed'
    )
    if selection in data_map:
        df_view, is_float, is_avg = data_map[selection]
        agg_row = {'회원등급': '합계'}
        for col in df_view.columns[1:]:
            agg_row[col] = np.nan if is_avg else df_view[col].sum()
        df_display = pd.concat([df_view, pd.DataFrame([agg_row])], ignore_index=True)
        st.dataframe(
            df_display.style.apply(_bold_last_row, axis=None).format(get_style_format(df_display, is_float=is_float)),
            hide_index=True, use_container_width=True
        )
    st.write('')

@st.fragment
def render_q_trend_detail(data_map):
    """분기 탭 — 분기별 실적 상세 (segmented_control + dataframe + 등급별 라인 차트)"""
    selection = st.segmented_control(
        "조회 항목 선택", options=list(data_map.keys()),
        selection_mode="single", default=list(data_map.keys())[0],
        key='quarterly_trend_select', label_visibility='collapsed'
    )
    if selection in data_map:
        df_view, is_float, is_avg = data_map[selection]
        agg_row = {'회원등급': '합계'}
        for col in df_view.columns[1:]:
            agg_row[col] = np.nan if is_avg else df_view[col].sum()
        df_display = pd.concat([df_view, pd.DataFrame([agg_row])], ignore_index=True)
        st.dataframe(
            df_display.style.apply(_bold_last_row, axis=None).format(get_style_format(df_display, is_float=is_float)),
            hide_index=True, use_container_width=True
        )
        st.write('')

        q_grade_fmt_map = {
            "매출액":    "%{y:,.0f}천원",
            "주문 건수": "%{y:,.0f}건",
            "주문자 수": "%{y:,.0f}명",
            "건당 수량": "%{y:.1f}",
            "건당 SKU":  "%{y:.1f}",
            "AOV":       "%{y:,.0f}원",
            "ARPPU":     "%{y:,.0f}원"
        }
        q_grade_fmt = q_grade_fmt_map.get(selection, "%{y:,.0f}")

        df_line = df_view.melt(id_vars='회원등급', var_name='배송분기', value_name=selection)
        fig_q_grade_line = px.line(
            df_line, x='배송분기', y=selection, color='회원등급',
            markers=True, title=f"등급별 {selection} 추이 (위 테이블에 연동)"
        )
        fig_q_grade_line.update_traces(
            hovertemplate=f'<b>%{{fullData.name}}</b>: {q_grade_fmt}<extra></extra>'
        )
        fig_q_grade_line.update_layout(
            title=dict(x=0, font=dict(size=16)),
            xaxis=dict(type='category', title=None, showgrid=False, spikecolor='#D1D5DB', spikedash='solid', spikethickness=1),
            yaxis=dict(title=None, showgrid=False),
            legend=dict(orientation="h", yanchor="bottom", y=1.02, xanchor="right", x=1),
            margin=dict(l=20, r=20, t=50, b=20),
            height=400,
            hovermode='x unified'
        )
        st.plotly_chart(fig_q_grade_line, use_container_width=True, key='grade_line_quarterly')

# -----------------------------------------------------------------------------
# 5. UI 레이아웃 구성
# -----------------------------------------------------------------------------

# 상단: 날짜 선택 (사이드바 미사용)
c1, c2 = st.columns([3, 1], vertical_alignment="bottom")
with c1:
    st.title("💰🐇 에쓰푸드몰 대시보드")
with c2:
    c2_1, c2_2 = st.columns([3, 2], vertical_alignment="bottom")
    with c2_1:
        # 기본값: 어제
        target_date = st.date_input("기준 일자", datetime.now() - timedelta(days=1), label_visibility="collapsed")
    with c2_2:
        if st.button("데이터 최신화", width='stretch'):
            load_parquet_data.clear() 
            st.rerun()

st.write('')
st.write('')
st.write('')

# 탭 구성
tab_daily, tab_weekly, tab_monthly, tab_quarterly = st.tabs(["⚡ **일간** (주문)", "📈 **주간** (주문)", "🏆 **월간** (출고)", "📊 **분기** (출고)"])

# 어제 날짜를 기준으로 잡음 ("end_date")
end_date = target_date

# =============================================================================
# [Tab 1] 일간 분석
# =============================================================================

with tab_daily:
    st.write('')
    st.write('')

    df_day_order = df[df['주문일자'].dt.strftime('%Y-%m-%d') == str(end_date)]

    if df_day_order.empty:
        st.warning("선택한 날짜에 데이터가 없습니다.")
    else:

        # 지표 정의 구간

        dailyOrderRevenue = df_day_order.drop_duplicates(subset=['주문 번호'])['결제_final'].sum()
        dailyOrderCount = df_day_order['주문 번호'].nunique()
        dailyOrderAov = dailyOrderRevenue / dailyOrderCount if dailyOrderCount > 0 else 0
        dailyBasketSKU = df_day_order.groupby(['주문 번호'])['상품명'].nunique().values.mean()
        dailyBasketSize = df_day_order.groupby(['주문 번호'])['상품수량'].sum().values.mean()

        df_day_order = df_day_order.copy()
        df_day_order['주문시간'] = df_day_order['주문일자'].dt.hour
        hourlyRevenueTrend = df_day_order.drop_duplicates(subset=['주문 번호']).groupby(['주문시간'])['결제_final'].sum().reset_index(name="매출액")

        all_hours = pd.DataFrame({'주문시간': range(24)})
        hourlyRevenueTrend = all_hours.merge(hourlyRevenueTrend, on='주문시간', how='left').fillna(0).sort_values(by='주문시간')

        temp_var = df_day_order.drop_duplicates(subset=['주문 번호'])
        userGradeSales = temp_var.groupby(['회원등급명_요약'])['결제_final'].sum().reset_index(name="매출액").rename(columns={'회원등급명_요약':'회원등급'})
        userGradeCount = temp_var.groupby(['회원등급명_요약'])['주문 번호'].nunique().reset_index(name="주문건수").rename(columns={'회원등급명_요약':'회원등급'})
        userGradeNumber = temp_var.groupby(['회원등급명_요약'])['구분자_비회원포함'].nunique().reset_index(name="주문자 수").rename(columns={'회원등급명_요약':'회원등급'})
        userGradeBasketSKU = df_day_order.groupby(['회원등급명_요약', '주문 번호'])['상품명'].nunique().reset_index().groupby(['회원등급명_요약'])['상품명'].mean().reset_index(name='건당 SKU').rename(columns={'회원등급명_요약':'회원등급'})
        userGradeBasketSize = df_day_order.groupby(['회원등급명_요약', '주문 번호'])['상품수량'].sum().reset_index().groupby(['회원등급명_요약'])['상품수량'].mean().reset_index(name='건당 수량').rename(columns={'회원등급명_요약':'회원등급'})

        userGradeDetails = pd.merge(userGradeSales, userGradeCount, on='회원등급')
        userGradeDetails = pd.merge(userGradeDetails, userGradeNumber, on='회원등급')
        userGradeDetails = pd.merge(userGradeDetails, userGradeBasketSKU, on='회원등급')
        userGradeDetails = pd.merge(userGradeDetails, userGradeBasketSize, on='회원등급')
        userGradeDetails['매출액'] = userGradeDetails['매출액'].astype(int)
        userGradeDetails['객단가'] = (userGradeDetails['매출액'] / userGradeDetails['주문건수']).astype(int)

        temp1 = df_day_order.groupby(['상품명']).agg({
                    '상품별 단순 총매출':'sum',
                    '상품수량':'sum'
                }).reset_index()

        target_condition = (
            (temp1['상품명'].str.contains('잠봉 슬라이스 500g|잠봉 슬라이스 400g')) &
            (~temp1['상품명'].str.contains('한정'))
        )
        jambong_ratio = temp1[target_condition][['상품명', '상품별 단순 총매출', '상품수량']].copy()

        new_row = pd.DataFrame({
            '상품명': ['🥩 잠봉 슬라이스 (3종 총합)'],
            '상품별 단순 총매출': [temp1.loc[target_condition, '상품별 단순 총매출'].sum()],
            '상품수량': [temp1.loc[target_condition, '상품수량'].sum()]
        })

        temp1 = pd.concat([temp1[~target_condition], new_row], ignore_index=True)

        dailyOrderTopItems = temp1.sort_values(by='상품별 단순 총매출', ascending=False).reset_index(drop=True).rename(columns={
                            '상품별 단순 총매출':'주문금액',
                            '상품수량':'주문수량'
                        }).head(6)

        # 1. 핵심 지표 (Metrics)
        
        daily_buyers = userGradeNumber['주문자 수'].sum()
        daily_order_freq = round(dailyOrderCount / daily_buyers, 1) if daily_buyers > 0 else 0.0
        d1, d2, d3, d4, d5, d6, d7 = st.columns(7)
        d1.metric("일간 총 매출 (VAT-)", f"{dailyOrderRevenue:,.0f}원")
        d2.metric("총 주문 건수", f"{dailyOrderCount:,}건")
        d3.metric("총 주문자 수", f"{daily_buyers:,}명")
        d4.metric("총 주문 빈도", f"{daily_order_freq:.1f}회")
        d5.metric("주문당 평균 구매 SKU", f"{dailyBasketSKU:,.1f}개")
        d6.metric("주문당 평균 구매 수량", f"{dailyBasketSize:,.1f}개")
        d7.metric("객단가 (AOV)", f"{dailyOrderAov:,.0f}원")
        
        st.markdown("---")

        # 2. 시간대별 매출 & 등급별 분석 (Grid 2:1 비율)

        row2_1, row2_2 = st.columns([2, 1], gap='large')
        
        with row2_1:
            # 모든 시간대가 나오도록 인덱스 재설정 (0~23)
            fig_hourly = px.area(hourlyRevenueTrend, x='주문시간', y='매출액', title="시간대별 매출 추이", markers=True)
            fig_hourly.update_layout(height=350, xaxis=dict(tickmode='linear', dtick=1))
            st.plotly_chart(fig_hourly, use_container_width=True)

        with row2_2:
            # 주문자 등급별 매출 비교
            fig_pie = px.pie(userGradeSales, values='매출액', names='회원등급', title="등급별 일매출 비중", hole=0.4)
            fig_pie.update_layout(height=350, margin=dict(l=20, r=20, t=40, b=20))
            st.plotly_chart(fig_pie, use_container_width=True)
        
        st.write('')
        st.write('')
        st.write('')
        st.write('')

        # 3. 등급별 상세 & 인기 품목 (Grid 1:1)
        row3_1, row3_2 = st.columns(2, gap='large')
        
        with row3_1:
            st.write("###### 등급별 상세 지표")
            st.dataframe(
                    add_details_summary_row(userGradeDetails, avg_cols=['객단가', '건당 SKU', '건당 수량']).style
                    .apply(_bold_last_row, axis=None)
                    .format({
                        "매출액":    _fmt("{:,}"),
                        "주문건수":  _fmt("{:,}"),
                        "주문자 수": _fmt("{:,}"),
                        "건당 SKU":  _fmt("{:,.1f}"),
                        "건당 수량": _fmt("{:,.1f}"),
                        "객단가":    _fmt("{:,.0f}")
                    }),
                    hide_index=True,
                    use_container_width=True
                    )
            
        with row3_2:
            fig_item = px.bar(dailyOrderTopItems, x='주문금액', y='상품명', orientation='h',
                              text='주문수량', title="일간 인기 품목 Top 6 (주문금액 & 주문수량)", color='주문금액')
            fig_item.update_yaxes(
                autorange="reversed",
                title_text=""
                )
            fig_item.update_xaxes(
                tickformat=",d",  # 천 단위 콤마
                title_text="주문금액 (원)",
                title_standoff=30
                )
            fig_item.update_layout(height=300, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
            fig_item.update_traces(
                texttemplate='%{text:,}개',
                hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
            )
            st.plotly_chart(fig_item, use_container_width=True)

            if not jambong_ratio.empty:
                jambong_ratio = jambong_ratio.sort_values('상품별 단순 총매출', ascending=False).reset_index(drop=True)
                fig_jambong = px.bar(
                    jambong_ratio, x='상품별 단순 총매출', y='상품명', orientation='h',
                    text='상품수량', title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)", color='상품별 단순 총매출'
                )
                fig_jambong.update_yaxes(autorange="reversed", title_text="")
                fig_jambong.update_xaxes(tickformat=",d", title_text="주문금액 (원)", title_standoff=30)
                fig_jambong.update_layout(height=200, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
                fig_jambong.update_traces(
                    texttemplate='%{text:,}개',
                    hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
                )
                st.plotly_chart(fig_jambong, use_container_width=True, key='jambong_daily')

        st.markdown("---")

        # 4. 최근 20일 추이

        start_date_20 = end_date - timedelta(days=19)
        df_20day = df[df['주문일자'].dt.normalize().between(str(start_date_20), str(end_date))].copy()

        weekday_names_d = ['월', '화', '수', '목', '금', '토', '일']
        df_20day['주문연월일'] = (
            df_20day['주문일자'].dt.strftime('%y-%m-%d') +
            '(' + df_20day['주문일자'].dt.weekday.apply(lambda x: weekday_names_d[x]) + ')'
        )

        revenue_20day = df_20day.drop_duplicates(subset=['주문 번호']).groupby(['주문연월일']).agg(
            매출액=('결제_final', 'sum'),
            주문건수=('주문 번호', 'nunique')
        ).reset_index()

        target_weekday_str = f'({weekday_names_d[end_date.weekday()]})'
        tickvals = revenue_20day['주문연월일'].tolist()
        ticktext = [
            f'<b><span style="color:#2563EB">{label}</span></b>'
            if label.endswith(target_weekday_str)
            else label
            for label in tickvals
        ]

        fig_20day = plot_combo_chart(
            revenue_20day['주문연월일'], revenue_20day['매출액'], revenue_20day['주문건수'],
            None, "매출액", "주문건수", "최근 20일 매출/주문 (추세 확인용)",
            bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
        )
        fig_20day.update_xaxes(
            type='category',
            title_text=None,
            tickmode='array',
            tickvals=tickvals,
            ticktext=ticktext
        )
        fig_20day.update_layout(yaxis_title=None, yaxis2_title=None, showlegend=True)
        st.plotly_chart(fig_20day, use_container_width=True)


# =============================================================================
# [Tab 2] 주간 분석
# =============================================================================

with tab_weekly:
    st.write('')
    st.write('')

    start_date_w = end_date - timedelta(days=end_date.weekday())

    # 지표 정의 구간

    df_week_order = df[df['주문일자'].dt.normalize().between(str(start_date_w), str(end_date))].copy()

    weeklyOrderRevenue = df_week_order.drop_duplicates(subset=['주문 번호'])['결제_final'].sum()
    weeklyOrderCount = df_week_order['주문 번호'].nunique()
    weeklyBasketSKU = df_week_order.groupby(['주문 번호'])['상품명'].nunique().values.mean()
    weeklyBasketSize = df_week_order.groupby(['주문 번호'])['상품수량'].sum().values.mean()
    weeklyOrderAov = weeklyOrderRevenue / weeklyOrderCount if weeklyOrderCount > 0 else 0
    
    weekday_names = ['월', '화', '수', '목', '금', '토', '일']
    df_week_order['주문연월일'] = (
        df_week_order['주문일자'].dt.strftime('%y-%m-%d') + 
        '(' + df_week_order['주문일자'].dt.weekday.apply(lambda x: weekday_names[x]) + ')'
    )
    weeklyRevenueByDay = df_week_order.drop_duplicates(subset=['주문 번호']).groupby(['주문연월일']).agg(
    매출액=('결제_final', 'sum'),
    주문건수=('주문 번호','nunique')
    ).reset_index()
    temp_var_w = df_week_order.drop_duplicates(subset=['주문 번호'])
    userGradeSalesWeekly = temp_var_w.groupby(['회원등급명_요약'])['결제_final'].sum().reset_index(name="매출액").rename(columns={'회원등급명_요약':'회원등급'})
    userGradeCountWeekly = temp_var_w.groupby(['회원등급명_요약'])['주문 번호'].nunique().reset_index(name="주문건수").rename(columns={'회원등급명_요약':'회원등급'})
    userGradeNumberWeekly = temp_var_w.groupby(['회원등급명_요약'])['구분자_비회원포함'].nunique().reset_index(name="주문자 수").rename(columns={'회원등급명_요약':'회원등급'})
    userGradeBasketSKUWeekly = df_week_order.groupby(['회원등급명_요약', '주문 번호'])['상품명'].nunique().reset_index().groupby(['회원등급명_요약'])['상품명'].mean().reset_index(name='건당 SKU').rename(columns={'회원등급명_요약':'회원등급'})
    userGradeBasketSizeWeekly = df_week_order.groupby(['회원등급명_요약', '주문 번호'])['상품수량'].sum().reset_index().groupby(['회원등급명_요약'])['상품수량'].mean().reset_index(name='건당 수량').rename(columns={'회원등급명_요약':'회원등급'})

    userGradeDetailsWeekly = pd.merge(userGradeSalesWeekly, userGradeCountWeekly, on='회원등급')
    userGradeDetailsWeekly = pd.merge(userGradeDetailsWeekly, userGradeNumberWeekly, on='회원등급')
    userGradeDetailsWeekly = pd.merge(userGradeDetailsWeekly, userGradeBasketSKUWeekly, on='회원등급')
    userGradeDetailsWeekly = pd.merge(userGradeDetailsWeekly, userGradeBasketSizeWeekly, on='회원등급')
    userGradeDetailsWeekly['매출액'] = userGradeDetailsWeekly['매출액'].astype(int)
    userGradeDetailsWeekly['객단가'] = (userGradeDetailsWeekly['매출액'] / userGradeDetailsWeekly['주문건수']).astype(int)

    temp2 = df_week_order.groupby(['상품명']).agg({
                    '상품별 단순 총매출':'sum',
                    '상품수량':'sum'
                }).reset_index()

    target_condition = (
        (temp2['상품명'].str.contains('잠봉 슬라이스 500g|잠봉 슬라이스 400g')) &
        (~temp2['상품명'].str.contains('한정'))
    )
    jambong_ratio = temp2[target_condition][['상품명', '상품별 단순 총매출', '상품수량']].copy()

    new_row = pd.DataFrame({
        '상품명': ['🥩 잠봉 슬라이스 (3종 총합)'],
        '상품별 단순 총매출': [temp2.loc[target_condition, '상품별 단순 총매출'].sum()],
        '상품수량': [temp2.loc[target_condition, '상품수량'].sum()]
    })

    temp2 = pd.concat([temp2[~target_condition], new_row], ignore_index=True)

    weeklyOrderTopItems = temp2.sort_values(by='상품별 단순 총매출', ascending=False).reset_index(drop=True).rename(columns={
                        '상품별 단순 총매출':'주문금액',
                        '상품수량':'주문수량'
                    }).head(6)

    # 1. 핵심 지표 (Metrics)
        
    weekly_buyers = userGradeNumberWeekly['주문자 수'].sum()
    weekly_order_freq = round(weeklyOrderCount / weekly_buyers, 1) if weekly_buyers > 0 else 0.0
    w1, w2, w3, w4, w5, w6, w7 = st.columns(7, gap='large')
    w1.metric("주간 총 매출 (VAT-)", f"{weeklyOrderRevenue:,.0f}원")
    w2.metric("총 주문 건수", f"{weeklyOrderCount:,}건")
    w3.metric("총 주문자 수", f"{weekly_buyers:,}명")
    w4.metric("총 주문 빈도", f"{weekly_order_freq:.1f}회")
    w5.metric("주문당 평균 구매 SKU", f"{weeklyBasketSKU:,.1f}개")
    w6.metric("주문당 평균 구매 수량", f"{weeklyBasketSize:,.1f}개")
    w7.metric("객단가 (AOV)", f"{weeklyOrderAov:,.0f}원")
    
    st.markdown("---")

    st.write('')
    st.write('')

    # 2. 이번 주 매출

    row1_1, row1_2 = st.columns([2, 1], gap='large')

    with row1_1:
            fig_w_trend = plot_combo_chart(
                weeklyRevenueByDay['주문연월일'], weeklyRevenueByDay['매출액'], weeklyRevenueByDay['주문건수'],
                "주차 (Week)", "매출액", "주문건수", "이번 주 매출/주문 추이",
                bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
            )
            # x축 제목 제거
            fig_w_trend.update_xaxes(title_text=None)

            # 모든 y축(왼쪽 y1, 오른쪽 y2 등)의 제목을 한꺼번에 제거
            fig_w_trend.update_layout(
                yaxis_title=None,
                yaxis2_title=None,
                showlegend=True # 범례는 유지
            )
            st.plotly_chart(fig_w_trend, use_container_width=True)
            
    with row1_2:
        # 주간 등급별 매출 비교
        fig_pie = px.pie(userGradeSalesWeekly, values='매출액', names='회원등급', title="등급별 매출 비중", hole=0.4)
        fig_pie.update_layout(height=350, margin=dict(l=20, r=20, t=40, b=20))
        st.plotly_chart(fig_pie, use_container_width=True, key='pie_weekly')

    st.write('')
    st.write('')
    st.write('')
    st.write('')
    
    # 2. 요일별 추이 & 인기품목
    row2_1, row2_2 = st.columns(2, gap='large')
    
    with row2_1:
        # 등급별 상세 지표
        st.write("###### 등급별 상세 지표")
        st.dataframe(
                add_details_summary_row(userGradeDetailsWeekly, avg_cols=['객단가', '건당 SKU', '건당 수량']).style
                .apply(_bold_last_row, axis=None)
                .format({
                    "매출액":    _fmt("{:,}"),
                    "주문건수":  _fmt("{:,}"),
                    "주문자 수": _fmt("{:,}"),
                    "건당 SKU":  _fmt("{:,.1f}"),
                    "건당 수량": _fmt("{:,.1f}"),
                    "객단가":    _fmt("{:,.0f}")
                }),
                hide_index=True,
                use_container_width=True
                )

    with row2_2:
        fig_item = px.bar(weeklyOrderTopItems, x='주문금액', y='상품명', orientation='h', 
                            text='주문수량', title="주간 인기 품목 Top 6 (주문금액 & 주문수량)", color='주문금액')
        fig_item.update_yaxes(
            autorange="reversed",
            title_text=""
            )
        fig_item.update_xaxes(
            tickformat=",d",  # 천 단위 콤마
            title_text="주문금액 (원)",
            title_standoff=30
            )
        fig_item.update_layout(height=300, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
        fig_item.update_traces(
            texttemplate='%{text:,}개',
            hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
        )
        st.plotly_chart(fig_item, use_container_width=True)

        if not jambong_ratio.empty:
            jambong_ratio = jambong_ratio.sort_values('상품별 단순 총매출', ascending=False).reset_index(drop=True)
            fig_jambong = px.bar(
                jambong_ratio, x='상품별 단순 총매출', y='상품명', orientation='h',
                text='상품수량', title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)", color='상품별 단순 총매출'
            )
            fig_jambong.update_yaxes(autorange="reversed", title_text="")
            fig_jambong.update_xaxes(tickformat=",d", title_text="주문금액 (원)", title_standoff=30)
            fig_jambong.update_layout(height=200, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
            fig_jambong.update_traces(
                texttemplate='%{text:,}개',
                hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
            )
            st.plotly_chart(fig_jambong, use_container_width=True, key='jambong_weekly')

    st.markdown("---")

    # 3. 최근 5주 추이
    start_date_5w = start_date_w - timedelta(weeks=4)
    df_5w = df[df['주문일자'].dt.normalize().between(str(start_date_5w), str(end_date))].copy()

    df_5w['주문주차'] = df_5w['주문일자'].dt.to_period('W').apply(
        lambda p: p.start_time.strftime('%y/%m/%d')
    )
    revenue_5w = df_5w.drop_duplicates(subset=['주문 번호']).groupby('주문주차').agg(
        매출액=('결제_final', 'sum'),
        주문건수=('주문 번호', 'nunique')
    ).reset_index()

    fig_5w = plot_combo_chart(
        revenue_5w['주문주차'], revenue_5w['매출액'], revenue_5w['주문건수'],
        None, "매출액", "주문건수", "최근 5주 매출/주문 (추세 확인용)",
        bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
    )
    fig_5w.update_traces(selector=dict(type='bar'), width=0.25)
    fig_5w.update_xaxes(type='category', title_text=None)
    fig_5w.update_layout(yaxis_title=None, yaxis2_title=None, showlegend=True)
    st.plotly_chart(fig_5w, use_container_width=True)

# =============================================================================
# [Tab 3] 월간 분석
# =============================================================================

with tab_monthly:
    st.write('')
    
    cm1, cm2 = st.columns([3, 1], vertical_alignment="bottom")
    with cm1:
        gray_memo("월간 대시보드에선 월(month) 단위로 확인이 가능합니다. 우측 선택창을 활용하세요.", icon="ⓘ")
    with cm2:
        month_range = pd.date_range(start='2025-01-01', end=end_date, freq='MS')
        month_list = [d.strftime('%Y-%m') for d in month_range][::-1]
        selected_month = st.selectbox(
            "조회 월을 선택하세요",
            label_visibility="collapsed",
            options=month_list,
            index=0  # 가장 최신 달(이번 달)을 기본값으로 설정
        )

    st.write('')
    st.write('')

    start_date_m = pd.to_datetime(selected_month)
    end_date_m = start_date_m + MonthEnd(0)

    # 지표 정의 구간 (Month)

    # OMS출고일 = 고도몰 배송일자 + 1 패턴 (완벽하지는 않음)
    df['OMS출고일'] = pd.to_datetime(df['배송일자']) + pd.Timedelta(days=1)

    df_month_delivery = df[df['OMS출고일'].dt.normalize().between(str(start_date_m), str(end_date_m))].copy()

    monthlyDeliveryRevenue = df_month_delivery.drop_duplicates(subset=['주문 번호'])['결제_final'].sum()
    monthlyDeliveryCount = df_month_delivery['주문 번호'].nunique()
    monthlyBasketSKU = df_month_delivery.groupby(['주문 번호'])['상품명'].nunique().values.mean()
    monthlyBasketSize = df_month_delivery.groupby(['주문 번호'])['상품수량'].sum().values.mean()
    monthlyDeliveryAov = monthlyDeliveryRevenue / monthlyDeliveryCount if monthlyDeliveryCount > 0 else 0
    monthlyDeliveryArppu = monthlyDeliveryRevenue / df_month_delivery['구분자_비회원포함'].nunique()
    
    df_month_delivery['배송연월'] = df_month_delivery['배송일자'].dt.strftime('%y-%m')
    weekday_names = ['월', '화', '수', '목', '금', '토', '일']
    df_month_delivery['배송연월일'] = (
        df_month_delivery['배송일자'].dt.strftime('%y-%m-%d') + 
        '(' + df_month_delivery['배송일자'].dt.weekday.apply(lambda x: weekday_names[x]) + ')'
    )

    monthlyRevenue = df_month_delivery.drop_duplicates(subset=['주문 번호']).groupby(['배송연월일']).agg(
    매출액=('결제_final', 'sum'),
    주문건수=('주문 번호','nunique')
    ).reset_index()

    temp_var_m = df_month_delivery.drop_duplicates(subset=['주문 번호'])
    userGradeSalesMonthly = temp_var_m.groupby(['회원등급명_요약'])['결제_final'].sum().reset_index(name="매출액").rename(columns={'회원등급명_요약':'회원등급'})
    userGradeCountMonthly = temp_var_m.groupby(['회원등급명_요약'])['주문 번호'].nunique().reset_index(name="주문건수").rename(columns={'회원등급명_요약':'회원등급'})
    userGradeNumberMonthly = temp_var_m.groupby(['회원등급명_요약'])['구분자_비회원포함'].nunique().reset_index(name="주문자 수").rename(columns={'회원등급명_요약':'회원등급'})
    userGradeBasketSKUMonthly = df_month_delivery.groupby(['회원등급명_요약', '주문 번호'])['상품명'].nunique().reset_index().groupby(['회원등급명_요약'])['상품명'].mean().reset_index(name='건당 SKU').rename(columns={'회원등급명_요약':'회원등급'})
    userGradeBasketSizeMonthly = df_month_delivery.groupby(['회원등급명_요약', '주문 번호'])['상품수량'].sum().reset_index().groupby(['회원등급명_요약'])['상품수량'].mean().reset_index(name='건당 수량').rename(columns={'회원등급명_요약':'회원등급'})    

    userGradeDetailsMonthly = pd.merge(userGradeSalesMonthly, userGradeCountMonthly, on='회원등급')
    userGradeDetailsMonthly = pd.merge(userGradeDetailsMonthly, userGradeNumberMonthly, on='회원등급')
    userGradeDetailsMonthly = pd.merge(userGradeDetailsMonthly, userGradeBasketSKUMonthly, on='회원등급')
    userGradeDetailsMonthly = pd.merge(userGradeDetailsMonthly, userGradeBasketSizeMonthly, on='회원등급')
    userGradeDetailsMonthly['매출액'] = userGradeDetailsMonthly['매출액'].astype(int)
    userGradeDetailsMonthly['AOV'] = (userGradeDetailsMonthly['매출액'] / userGradeDetailsMonthly['주문건수']).astype(int)
    userGradeDetailsMonthly['ARPPU'] = (userGradeDetailsMonthly['매출액'] / userGradeDetailsMonthly['주문자 수']).astype(int)
    
    temp3 = df_month_delivery.groupby(['상품명']).agg({
                    '상품별 단순 총매출':'sum',
                    '상품수량':'sum'
                }).reset_index()

    target_condition = (
        (temp3['상품명'].str.contains('잠봉 슬라이스 500g|잠봉 슬라이스 400g')) &
        (~temp3['상품명'].str.contains('한정'))
    )
    jambong_ratio = temp3[target_condition][['상품명', '상품별 단순 총매출', '상품수량']].copy()

    new_row = pd.DataFrame({
        '상품명': ['🥩 잠봉 슬라이스 (3종 총합)'],
        '상품별 단순 총매출': [temp3.loc[target_condition, '상품별 단순 총매출'].sum()],
        '상품수량': [temp3.loc[target_condition, '상품수량'].sum()]
    })

    temp3 = pd.concat([temp3[~target_condition], new_row], ignore_index=True)

    monthlyOrderTopItems = temp3.sort_values(by='상품별 단순 총매출', ascending=False).reset_index(drop=True).rename(columns={
                        '상품별 단순 총매출':'주문금액',
                        '상품수량':'주문수량'
                    }).head(6)

    userGradeSalesMonthlyOverview = temp_var_m.pivot_table(
    index='회원등급명_요약', columns='배송연월일', values='결제_final', aggfunc='sum'
    ).fillna(0)
    userGradeSalesMonthlyOverview = (userGradeSalesMonthlyOverview / 1000).reset_index().rename(columns={'회원등급명_요약':'회원등급'})

    userGradeCountMonthlyOverview = temp_var_m.pivot_table(
        index='회원등급명_요약', columns='배송연월일', values='주문 번호', aggfunc='nunique'
    ).fillna(0).reset_index().rename(columns={'회원등급명_요약':'회원등급'})

    userGradeNumberMonthlyOverview = temp_var_m.pivot_table(
        index='회원등급명_요약', columns='배송연월일', values='구분자_비회원포함', aggfunc='nunique'
    ).fillna(0).reset_index().rename(columns={'회원등급명_요약':'회원등급'})

    
    # 지표 정의 구간 (Year)

    start_date_y = pd.Timestamp(start_date_m.year - 1, 1, 1)
    end_date_y = end_date_m

    df_year_delivery = df[
        df['OMS출고일'].dt.normalize().between(str(start_date_y), str(end_date_y))
    ].copy()
    df_year_delivery['배송연월'] = df_year_delivery['OMS출고일'].dt.strftime('%y-%m')
    weekday_names = ['월', '화', '수', '목', '금', '토', '일']
    df_year_delivery['배송연월일'] = (
        df_year_delivery['배송일자'].dt.strftime('%y-%m-%d') + 
        '(' + df_year_delivery['배송일자'].dt.weekday.apply(lambda x: weekday_names[x]) + ')'
    )

    yearlyRevenue = df_year_delivery.drop_duplicates(subset=['주문 번호']).groupby(['배송연월']).agg(
    매출액=('결제_final', 'sum'),
    주문건수=('주문 번호','nunique')
    ).reset_index()

    temp_var_y = df_year_delivery.drop_duplicates(subset=['주문 번호'])

    # 기본 집계 (Raw Data 계산)
    sales_raw = temp_var_y.pivot_table(index='회원등급명_요약', columns='배송연월', values='결제_final', aggfunc='sum').fillna(0)
    count_raw = temp_var_y.pivot_table(index='회원등급명_요약', columns='배송연월', values='주문 번호', aggfunc='nunique').fillna(0)
    number_raw = temp_var_y.pivot_table(index='회원등급명_요약', columns='배송연월', values='구분자_비회원포함', aggfunc='nunique').fillna(0)

    # 파생 지표 계산 (AOV, ARPPU)
    aov_raw = (sales_raw / count_raw).fillna(0)
    arppu_raw = (sales_raw / number_raw).fillna(0)

    # Basket 지표 계산
    basket_sku_raw = df_year_delivery.groupby(['회원등급명_요약', '배송연월', '주문 번호'])['상품명'].nunique().reset_index() \
        .pivot_table(index='회원등급명_요약', columns='배송연월', values='상품명', aggfunc='mean').fillna(0)

    basket_size_raw = df_year_delivery.groupby(['회원등급명_요약', '배송연월', '주문 번호'])['상품수량'].sum().reset_index() \
        .pivot_table(index='회원등급명_요약', columns='배송연월', values='상품수량', aggfunc='mean').fillna(0)

    # 출력용 DataFrame 변환
    userGradeSalesYearly = (sales_raw / 1000).reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    userGradeCountYearly = count_raw.reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    userGradeNumberYearly = number_raw.reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    userGradeAovYearly = aov_raw.reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    userGradeArppuYearly = arppu_raw.reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    userGradeBasketSKUYearly = basket_sku_raw.reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    userGradeBasketSizeYearly = basket_size_raw.reset_index().rename(columns={'회원등급명_요약':'회원등급'})
    

    # 1. 핵심 지표 (Metrics)
        
    monthly_buyers = userGradeNumberMonthly['주문자 수'].sum()
    monthly_order_freq = round(monthlyDeliveryCount / monthly_buyers, 1) if monthly_buyers > 0 else 0.0
    m1, m2, m3, m4, m5, m6, m7, m8 = st.columns(8, gap='small')
    m1.metric("월간 총 매출 (VAT-)", f"{monthlyDeliveryRevenue:,.0f}원")
    m2.metric("총 주문 건수", f"{monthlyDeliveryCount:,}건")
    m3.metric("총 주문자 수", f"{monthly_buyers:,}명")
    m4.metric("총 주문 빈도", f"{monthly_order_freq:.1f}회")
    m5.metric("주문당 평균 구매 SKU", f"{monthlyBasketSKU:,.1f}개")
    m6.metric("주문당 평균 구매 수량", f"{monthlyBasketSize:,.1f}개")
    m7.metric("객단가 (AOV)", f"{monthlyDeliveryAov:,.0f}원")
    m8.metric("결제자당 평균 매출 (ARPPU)", f"{monthlyDeliveryArppu:,.0f}원")
    
    st.markdown("---")

    st.write('')
    st.write('')

    # 2. 최근 30일 매출

    row1_1, row1_2 = st.columns([2, 1], gap='large')

    with row1_1:
            fig_m_trend = plot_combo_chart(
                monthlyRevenue['배송연월일'], monthlyRevenue['매출액'], monthlyRevenue['주문건수'],
                "주차 (Week)", "매출액", "주문 건수", "월간 출고 추이",
                bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
            )
            # x축 제목 제거
            fig_m_trend.update_xaxes(title_text=None)

            # 모든 y축(왼쪽 y1, 오른쪽 y2 등)의 제목을 한꺼번에 제거
            fig_m_trend.update_layout(
                yaxis_title=None,
                yaxis2_title=None,
                showlegend=True # 범례는 유지
            )
            st.plotly_chart(fig_m_trend, use_container_width=True)
            
    with row1_2:
        # 주간 등급별 매출 비교
        fig_pie = px.pie(userGradeSalesMonthly, values='매출액', names='회원등급', title="등급별 매출 비중", hole=0.4)
        fig_pie.update_layout(height=350, margin=dict(l=20, r=20, t=40, b=20))
        st.plotly_chart(fig_pie, use_container_width=True, key='pie_monthly')

    st.write('')
    st.write('')
    st.write('')
    st.write('')

    # 2. 요일별 추이 & 인기품목
    row2_1, row2_2 = st.columns(2, gap='large')
    
    with row2_1:
        # 등급별 상세 지표
        st.write("###### 등급별 상세 지표")
        st.dataframe(
                add_details_summary_row(userGradeDetailsMonthly, avg_cols=['AOV', 'ARPPU', '건당 SKU', '건당 수량']).style
                .apply(_bold_last_row, axis=None)
                .format({
                    "매출액":    _fmt("{:,}"),
                    "주문건수":  _fmt("{:,}"),
                    "주문자 수": _fmt("{:,}"),
                    "건당 SKU":  _fmt("{:,.1f}"),
                    "건당 수량": _fmt("{:,.1f}"),
                    "AOV":       _fmt("{:,.0f}"),
                    "ARPPU":     _fmt("{:,.0f}")
                }),
                hide_index=True,
                use_container_width=True
                )
    
    with row2_2:
        fig_item = px.bar(monthlyOrderTopItems, x='주문금액', y='상품명', orientation='h',
                            text='주문수량', title="월간 인기 품목 Top 6 (주문금액 & 주문수량)", color='주문금액')
        fig_item.update_yaxes(
            autorange="reversed",
            title_text=""
            )
        fig_item.update_xaxes(
            tickformat=",d",  # 천 단위 콤마
            title_text="주문금액 (원)",
            title_standoff=30
            )
        fig_item.update_layout(height=300, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
        fig_item.update_traces(
            texttemplate='%{text:,}개',
            hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
        )
        st.plotly_chart(fig_item, use_container_width=True)

        if not jambong_ratio.empty:
            jambong_ratio = jambong_ratio.sort_values('상품별 단순 총매출', ascending=False).reset_index(drop=True)
            fig_jambong = px.bar(
                jambong_ratio, x='상품별 단순 총매출', y='상품명', orientation='h',
                text='상품수량', title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)", color='상품별 단순 총매출'
            )
            fig_jambong.update_yaxes(autorange="reversed", title_text="")
            fig_jambong.update_xaxes(tickformat=",d", title_text="주문금액 (원)", title_standoff=30)
            fig_jambong.update_layout(height=200, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
            fig_jambong.update_traces(
                texttemplate='%{text:,}개',
                hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
            )
            st.plotly_chart(fig_jambong, use_container_width=True, key='jambong_monthly')


    # 3. 일별 실적 상세

    st.write("###### 일별 실적 상세 (매출액 단위: 천 원)")

    render_monthly_detail({
        "매출액":    (userGradeSalesMonthlyOverview, False, False),
        "주문 건수": (userGradeCountMonthlyOverview, False, False),
        "주문자 수": (userGradeNumberMonthlyOverview, False, False)
    })

    # 3-1. 전년 동일월 비교

    start_date_py = start_date_m - relativedelta(years=1)
    end_date_py   = start_date_py + MonthEnd(0)
    df_prev_month = df[df['OMS출고일'].dt.normalize().between(str(start_date_py), str(end_date_py))].copy()

    py_revenue = df_prev_month.drop_duplicates(subset=['주문 번호'])['결제_final'].sum()
    py_count   = df_prev_month['주문 번호'].nunique()
    py_buyers  = df_prev_month['구분자_비회원포함'].nunique()
    py_aov     = py_revenue / py_count if py_count > 0 else 0
    cy_buyers  = df_month_delivery['구분자_비회원포함'].nunique()

    compare_sales = pd.DataFrame({
        '월': [start_date_py.strftime('%Y-%m'), start_date_m.strftime('%Y-%m')],
        '매출액': [py_revenue, monthlyDeliveryRevenue],
        '주문건수': [py_count, monthlyDeliveryCount]
    })
    compare_buyers = pd.DataFrame({
        '월': [start_date_py.strftime('%Y-%m'), start_date_m.strftime('%Y-%m')],
        '구매자 수': [py_buyers, cy_buyers],
        'AOV': [py_aov, monthlyDeliveryAov]
    })

    col_yoy1, col_yoy2 = st.columns(2, gap='large')

    with col_yoy1:
        fig_yoy1 = plot_combo_chart(
            compare_sales['월'], compare_sales['매출액'], compare_sales['주문건수'],
            None, "매출액", "주문건수", "전년 동월 비교 — 매출, 주문건수 (이번 달은 어제까지 누적)",
            bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
        )
        fig_yoy1.update_traces(selector=dict(type='bar'), width=0.4, marker_color='#A5ACFC',
                               texttemplate='<b>%{y:,.0f}원</b>', textposition='inside', insidetextanchor='start', textfont=dict(color='black'))
        fig_yoy1.update_traces(selector=dict(type='scatter'), line=dict(dash='dot', color='#2563EB'), marker=dict(color='#2563EB'),
                               mode='lines+markers+text', texttemplate='<b>%{y:,d}건</b>', textposition='top center', textfont=dict(color='#2563EB'))
        fig_yoy1.update_xaxes(type='category', title_text=None)
        fig_yoy1.update_layout(
            yaxis_title=None, yaxis2_title=None,
            yaxis=dict(range=[0, compare_sales['매출액'].max() * 1.3], showticklabels=False),
            yaxis2=dict(range=[0, compare_sales['주문건수'].max() * 1.5], showticklabels=False)
        )
        st.plotly_chart(fig_yoy1, use_container_width=True)

    with col_yoy2:
        fig_yoy2 = plot_combo_chart(
            compare_buyers['월'], compare_buyers['구매자 수'], compare_buyers['AOV'],
            None, "구매자 수", "AOV", "전년 동월 비교 — 구매자 수, AOV (이번 달은 어제까지 누적)",
            bar_fmt='%{y:,d}명', line_fmt='%{y:,.0f}원'
        )
        fig_yoy2.update_traces(selector=dict(type='bar'), width=0.4, marker_color='#A5ACFC',
                               texttemplate='<b>%{y:,d}명</b>', textposition='inside', insidetextanchor='start', textfont=dict(color='black'))
        fig_yoy2.update_traces(selector=dict(type='scatter'), line=dict(dash='dot', color='#2563EB'), marker=dict(color='#2563EB'),
                               mode='lines+markers+text', texttemplate='<b>%{y:,.0f}원</b>', textposition='top center', textfont=dict(color='#2563EB'))
        fig_yoy2.update_xaxes(type='category', title_text=None)
        fig_yoy2.update_layout(
            yaxis_title=None, yaxis2_title=None,
            yaxis=dict(range=[0, compare_buyers['구매자 수'].max() * 1.3], showticklabels=False),
            yaxis2=dict(range=[0, compare_buyers['AOV'].max() * 1.5], showticklabels=False)
        )
        st.plotly_chart(fig_yoy2, use_container_width=True)

    st.write('')
    st.divider()
    st.write('')

    # 4. 최근 2개년 추이

    fig_y_trend = plot_combo_chart(
        yearlyRevenue['배송연월'], yearlyRevenue['매출액'], yearlyRevenue['주문건수'],
        "주차 (Week)", "매출액", "주문 건수", "최근 2개년 추이",
        bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
    )
    # x축 제목 제거
    fig_y_trend.update_xaxes(type='category', title_text=None)

    # 모든 y축(왼쪽 y1, 오른쪽 y2 등)의 제목을 한꺼번에 제거
    fig_y_trend.update_layout(
        yaxis_title=None,
        yaxis2_title=None,
        showlegend=True # 범례는 유지
    )
    st.plotly_chart(fig_y_trend, use_container_width=True)

    st.write('')
    st.write('')

    # 5. 월별 실적 상세
    
    st.write("###### 월별 실적 상세 (매출액 단위: 천 원)")

    render_yearly_detail({
        "매출액":    (userGradeSalesYearly,    False, False),
        "주문 건수": (userGradeCountYearly,    False, False),
        "주문자 수": (userGradeNumberYearly,   False, False),
        "건당 수량": (userGradeBasketSizeYearly, True, True),
        "건당 SKU":  (userGradeBasketSKUYearly,  True, True),
        "AOV":       (userGradeAovYearly,      False, True),
        "ARPPU":     (userGradeArppuYearly,    False, True)
    })

# =============================================================================
# [Tab 4] 분기 분석
# =============================================================================

with tab_quarterly:
    st.write('')

    cq1, cq2 = st.columns([3, 1], vertical_alignment="bottom")
    with cq1:
        gray_memo("분기 대시보드에선 분기(quarter) 단위로 확인이 가능합니다. 우측 선택창을 활용하세요.", icon="ⓘ")
    with cq2:
        q_periods = pd.period_range(start='2025-01-01', end=end_date, freq='Q')
        quarter_list = [f"{p.year}-{p.quarter}Q" for p in q_periods][::-1]
        selected_quarter = st.selectbox(
            "조회 분기를 선택하세요",
            label_visibility="collapsed",
            options=quarter_list,
            index=0
        )

    st.write('')
    st.write('')

    q_year = int(selected_quarter[:4])
    q_num  = int(selected_quarter[5])
    p_q    = pd.Period(f"{q_year}Q{q_num}", freq='Q')
    start_date_q = p_q.to_timestamp(how='start')
    end_date_q   = p_q.to_timestamp(how='end').normalize()

    # OMS출고일 컬럼 (tab_monthly와 독립적으로 설정)
    df['OMS출고일'] = pd.to_datetime(df['배송일자']) + pd.Timedelta(days=1)

    df_q_delivery = df[df['OMS출고일'].dt.normalize().between(str(start_date_q), str(end_date_q))].copy()

    if df_q_delivery.empty:
        st.warning("선택한 분기에 해당하는 출고 데이터가 없습니다.")
    else:
        # 지표 계산
        quarterlyDeliveryRevenue = df_q_delivery.drop_duplicates(subset=['주문 번호'])['결제_final'].sum()
        quarterlyDeliveryCount   = df_q_delivery['주문 번호'].nunique()
        quarterlyBasketSKU       = df_q_delivery.groupby(['주문 번호'])['상품명'].nunique().values.mean()
        quarterlyBasketSize      = df_q_delivery.groupby(['주문 번호'])['상품수량'].sum().values.mean()
        quarterlyDeliveryAov     = quarterlyDeliveryRevenue / quarterlyDeliveryCount if quarterlyDeliveryCount > 0 else 0
        quarterly_buyers         = df_q_delivery['구분자_비회원포함'].nunique()
        quarterlyDeliveryArppu   = quarterlyDeliveryRevenue / quarterly_buyers if quarterly_buyers > 0 else 0

        df_q_delivery['배송연월'] = df_q_delivery['OMS출고일'].dt.strftime('%y-%m')

        temp_q_dedup = df_q_delivery.drop_duplicates(subset=['주문 번호'])

        # 분기 내 월별 집계 (combo chart용)
        quarterlyRevenueByMonth = temp_q_dedup.groupby('배송연월').agg(
            매출액=('결제_final', 'sum'),
            주문건수=('주문 번호', 'nunique')
        ).reset_index()

        # 등급별 집계
        userGradeSalesQuarterly      = temp_q_dedup.groupby('회원등급명_요약')['결제_final'].sum().reset_index(name="매출액").rename(columns={'회원등급명_요약': '회원등급'})
        userGradeCountQuarterly      = temp_q_dedup.groupby('회원등급명_요약')['주문 번호'].nunique().reset_index(name="주문건수").rename(columns={'회원등급명_요약': '회원등급'})
        userGradeNumberQuarterly     = temp_q_dedup.groupby('회원등급명_요약')['구분자_비회원포함'].nunique().reset_index(name="주문자 수").rename(columns={'회원등급명_요약': '회원등급'})
        userGradeBasketSKUQuarterly  = df_q_delivery.groupby(['회원등급명_요약', '주문 번호'])['상품명'].nunique().reset_index().groupby('회원등급명_요약')['상품명'].mean().reset_index(name='건당 SKU').rename(columns={'회원등급명_요약': '회원등급'})
        userGradeBasketSizeQuarterly = df_q_delivery.groupby(['회원등급명_요약', '주문 번호'])['상품수량'].sum().reset_index().groupby('회원등급명_요약')['상품수량'].mean().reset_index(name='건당 수량').rename(columns={'회원등급명_요약': '회원등급'})

        userGradeDetailsQuarterly = pd.merge(userGradeSalesQuarterly, userGradeCountQuarterly, on='회원등급')
        userGradeDetailsQuarterly = pd.merge(userGradeDetailsQuarterly, userGradeNumberQuarterly, on='회원등급')
        userGradeDetailsQuarterly = pd.merge(userGradeDetailsQuarterly, userGradeBasketSKUQuarterly, on='회원등급')
        userGradeDetailsQuarterly = pd.merge(userGradeDetailsQuarterly, userGradeBasketSizeQuarterly, on='회원등급')
        userGradeDetailsQuarterly['매출액'] = userGradeDetailsQuarterly['매출액'].astype(int)
        userGradeDetailsQuarterly['AOV']   = (userGradeDetailsQuarterly['매출액'] / userGradeDetailsQuarterly['주문건수']).astype(int)
        userGradeDetailsQuarterly['ARPPU'] = (userGradeDetailsQuarterly['매출액'] / userGradeDetailsQuarterly['주문자 수']).astype(int)

        # 인기 품목
        temp_q = df_q_delivery.groupby('상품명').agg({
            '상품별 단순 총매출': 'sum', '상품수량': 'sum'
        }).reset_index()
        target_cond_q = (
            temp_q['상품명'].str.contains('잠봉 슬라이스 500g|잠봉 슬라이스 400g') &
            ~temp_q['상품명'].str.contains('한정')
        )
        jambong_ratio = temp_q[target_cond_q][['상품명', '상품별 단순 총매출', '상품수량']].copy()
        new_row_q = pd.DataFrame({
            '상품명': ['🥩 잠봉 슬라이스 (3종 총합)'],
            '상품별 단순 총매출': [temp_q.loc[target_cond_q, '상품별 단순 총매출'].sum()],
            '상품수량': [temp_q.loc[target_cond_q, '상품수량'].sum()]
        })
        temp_q = pd.concat([temp_q[~target_cond_q], new_row_q], ignore_index=True)
        quarterlyOrderTopItems = temp_q.sort_values(by='상품별 단순 총매출', ascending=False).reset_index(drop=True).rename(columns={
            '상품별 단순 총매출': '주문금액', '상품수량': '주문수량'
        }).head(6)

        # 월별 실적 상세 pivot (분기 내 3개월)
        userGradeSalesQOverview  = temp_q_dedup.pivot_table(index='회원등급명_요약', columns='배송연월', values='결제_final', aggfunc='sum').fillna(0)
        userGradeSalesQOverview  = (userGradeSalesQOverview / 1000).reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeCountQOverview  = temp_q_dedup.pivot_table(index='회원등급명_요약', columns='배송연월', values='주문 번호', aggfunc='nunique').fillna(0).reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeNumberQOverview = temp_q_dedup.pivot_table(index='회원등급명_요약', columns='배송연월', values='구분자_비회원포함', aggfunc='nunique').fillna(0).reset_index().rename(columns={'회원등급명_요약': '회원등급'})

        # ---- 1. 핵심 지표 ----
        quarterly_order_freq = round(quarterlyDeliveryCount / quarterly_buyers, 1) if quarterly_buyers > 0 else 0.0
        m1, m2, m3, m4, m5, m6, m7, m8 = st.columns(8, gap='small')
        m1.metric("분기 총 매출 (VAT-)", f"{quarterlyDeliveryRevenue:,.0f}원")
        m2.metric("총 주문 건수", f"{quarterlyDeliveryCount:,}건")
        m3.metric("총 주문자 수", f"{quarterly_buyers:,}명")
        m4.metric("총 주문 빈도", f"{quarterly_order_freq:.1f}회")
        m5.metric("주문당 평균 구매 SKU", f"{quarterlyBasketSKU:,.1f}개")
        m6.metric("주문당 평균 구매 수량", f"{quarterlyBasketSize:,.1f}개")
        m7.metric("객단가 (AOV)", f"{quarterlyDeliveryAov:,.0f}원")
        m8.metric("결제자당 평균 매출 (ARPPU)", f"{quarterlyDeliveryArppu:,.0f}원")

        st.markdown("---")
        st.write('')
        st.write('')

        # ---- 2. 분기 내 월별 추이 + 파이 ----
        row_q1_1, row_q1_2 = st.columns([2, 1], gap='large')
        with row_q1_1:
            fig_q_trend = plot_combo_chart(
                quarterlyRevenueByMonth['배송연월'], quarterlyRevenueByMonth['매출액'], quarterlyRevenueByMonth['주문건수'],
                None, "매출액", "주문건수", "분기 내 월별 출고 추이",
                bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
            )
            fig_q_trend.update_traces(selector=dict(type='bar'), width=0.3)
            fig_q_trend.update_xaxes(type='category', title_text=None)
            fig_q_trend.update_layout(yaxis_title=None, yaxis2_title=None, showlegend=True)
            st.plotly_chart(fig_q_trend, use_container_width=True)

        with row_q1_2:
            fig_q_pie = px.pie(userGradeSalesQuarterly, values='매출액', names='회원등급', title="등급별 매출 비중", hole=0.4)
            fig_q_pie.update_layout(height=350, margin=dict(l=20, r=20, t=40, b=20))
            st.plotly_chart(fig_q_pie, use_container_width=True, key='pie_quarterly')

        st.write('')
        st.write('')
        st.write('')
        st.write('')

        # ---- 3. 등급별 상세 + 인기 품목 ----
        row_q2_1, row_q2_2 = st.columns(2, gap='large')
        with row_q2_1:
            st.write("###### 등급별 상세 지표")
            st.dataframe(
                add_details_summary_row(userGradeDetailsQuarterly, avg_cols=['AOV', 'ARPPU', '건당 SKU', '건당 수량']).style
                .apply(_bold_last_row, axis=None)
                .format({
                    "매출액":    _fmt("{:,}"), "주문건수":  _fmt("{:,}"), "주문자 수": _fmt("{:,}"),
                    "건당 SKU":  _fmt("{:,.1f}"), "건당 수량": _fmt("{:,.1f}"),
                    "AOV":       _fmt("{:,.0f}"), "ARPPU":     _fmt("{:,.0f}")
                }),
                hide_index=True, use_container_width=True
            )

        with row_q2_2:
            fig_item_q = px.bar(quarterlyOrderTopItems, x='주문금액', y='상품명', orientation='h',
                                text='주문수량', title="분기 인기 품목 Top 6 (주문금액 & 주문수량)", color='주문금액')
            fig_item_q.update_yaxes(autorange="reversed", title_text="")
            fig_item_q.update_xaxes(tickformat=",d", title_text="주문금액 (원)", title_standoff=30)
            fig_item_q.update_layout(height=300, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
            fig_item_q.update_traces(
                texttemplate='%{text:,}개',
                hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
            )
            st.plotly_chart(fig_item_q, use_container_width=True)

            if not jambong_ratio.empty:
                jambong_ratio = jambong_ratio.sort_values('상품별 단순 총매출', ascending=False).reset_index(drop=True)
                fig_jambong = px.bar(
                    jambong_ratio, x='상품별 단순 총매출', y='상품명', orientation='h',
                    text='상품수량', title="🥩 잠봉 슬라이스 판매 비중 (주문금액 & 주문수량)", color='상품별 단순 총매출'
                )
                fig_jambong.update_yaxes(autorange="reversed", title_text="")
                fig_jambong.update_xaxes(tickformat=",d", title_text="주문금액 (원)", title_standoff=30)
                fig_jambong.update_layout(height=200, margin=dict(l=0, r=0, t=30, b=0), coloraxis_showscale=False)
                fig_jambong.update_traces(
                    texttemplate='%{text:,}개',
                    hovertemplate='<b>%{y}</b><br>주문금액: %{x:,.0f}원<br>주문수량: %{text:,}개<extra></extra>'
                )
                st.plotly_chart(fig_jambong, use_container_width=True, key='jambong_quarterly')

        # ---- 4. 월별 실적 상세 (분기 내 3개월) ----
        st.write("###### 월별 실적 상세 (매출액 단위: 천 원)")

        render_q_monthly_detail({
            "매출액":    (userGradeSalesQOverview,  False, False),
            "주문 건수": (userGradeCountQOverview,  False, False),
            "주문자 수": (userGradeNumberQOverview, False, False)
        })

        # ---- 5. 전년 동분기 비교 ----
        p_pq          = p_q - 4
        start_date_pq = p_pq.to_timestamp(how='start')
        end_date_pq   = p_pq.to_timestamp(how='end').normalize()
        df_prev_q     = df[df['OMS출고일'].dt.normalize().between(str(start_date_pq), str(end_date_pq))].copy()

        pq_revenue = df_prev_q.drop_duplicates(subset=['주문 번호'])['결제_final'].sum()
        pq_count   = df_prev_q['주문 번호'].nunique()
        pq_buyers  = df_prev_q['구분자_비회원포함'].nunique()
        pq_aov     = pq_revenue / pq_count if pq_count > 0 else 0

        compare_q_sales = pd.DataFrame({
            '분기': [f"{p_pq.year}-{p_pq.quarter}Q", selected_quarter],
            '매출액': [pq_revenue, quarterlyDeliveryRevenue],
            '주문건수': [pq_count, quarterlyDeliveryCount]
        })
        compare_q_buyers_df = pd.DataFrame({
            '분기': [f"{p_pq.year}-{p_pq.quarter}Q", selected_quarter],
            '구매자 수': [pq_buyers, quarterly_buyers],
            'AOV': [pq_aov, quarterlyDeliveryAov]
        })

        col_qyoy1, col_qyoy2 = st.columns(2, gap='large')
        with col_qyoy1:
            fig_qyoy1 = plot_combo_chart(
                compare_q_sales['분기'], compare_q_sales['매출액'], compare_q_sales['주문건수'],
                None, "매출액", "주문건수", "전년 동분기 비교 — 매출, 주문건수 (이번 분기는 어제까지 누적)",
                bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
            )
            fig_qyoy1.update_traces(selector=dict(type='bar'), width=0.3, marker_color='#A5ACFC',
                                   texttemplate='<b>%{y:,.0f}원</b>', textposition='inside', insidetextanchor='start', textfont=dict(color='black'))
            fig_qyoy1.update_traces(selector=dict(type='scatter'), line=dict(dash='dot', color='#2563EB'), marker=dict(color='#2563EB'),
                                   mode='lines+markers+text', texttemplate='<b>%{y:,d}건</b>', textposition='top center', textfont=dict(color='#2563EB'))
            fig_qyoy1.update_xaxes(type='category', title_text=None)
            fig_qyoy1.update_layout(
                yaxis_title=None, yaxis2_title=None,
                yaxis=dict(range=[0, compare_q_sales['매출액'].max() * 1.3], showticklabels=False),
                yaxis2=dict(range=[0, compare_q_sales['주문건수'].max() * 1.5], showticklabels=False)
            )
            st.plotly_chart(fig_qyoy1, use_container_width=True)

        with col_qyoy2:
            fig_qyoy2 = plot_combo_chart(
                compare_q_buyers_df['분기'], compare_q_buyers_df['구매자 수'], compare_q_buyers_df['AOV'],
                None, "구매자 수", "AOV", "전년 동분기 비교 — 구매자 수, AOV (이번 분기는 어제까지 누적)",
                bar_fmt='%{y:,d}명', line_fmt='%{y:,.0f}원'
            )
            fig_qyoy2.update_traces(selector=dict(type='bar'), width=0.3, marker_color='#A5ACFC',
                                   texttemplate='<b>%{y:,d}명</b>', textposition='inside', insidetextanchor='start', textfont=dict(color='black'))
            fig_qyoy2.update_traces(selector=dict(type='scatter'), line=dict(dash='dot', color='#2563EB'), marker=dict(color='#2563EB'),
                                   mode='lines+markers+text', texttemplate='<b>%{y:,.0f}원</b>', textposition='top center', textfont=dict(color='#2563EB'))
            fig_qyoy2.update_xaxes(type='category', title_text=None)
            fig_qyoy2.update_layout(
                yaxis_title=None, yaxis2_title=None,
                yaxis=dict(range=[0, compare_q_buyers_df['구매자 수'].max() * 1.3], showticklabels=False),
                yaxis2=dict(range=[0, compare_q_buyers_df['AOV'].max() * 1.5], showticklabels=False)
            )
            st.plotly_chart(fig_qyoy2, use_container_width=True)

        st.write('')
        st.divider()
        st.write('')

        # ---- 6. 최근 8분기 추이 ----
        p_8q_start    = p_q - 7
        start_date_8q = p_8q_start.to_timestamp(how='start')
        df_8q = df[df['OMS출고일'].dt.normalize().between(str(start_date_8q), str(end_date_q))].copy()
        df_8q['배송분기'] = df_8q['OMS출고일'].dt.to_period('Q').apply(
            lambda period: f"{str(period.year)[2:]}-{period.quarter}Q"
        )

        quarterlyTrend = df_8q.drop_duplicates(subset=['주문 번호']).groupby('배송분기').agg(
            매출액=('결제_final', 'sum'),
            주문건수=('주문 번호', 'nunique')
        ).reset_index()

        fig_8q_trend = plot_combo_chart(
            quarterlyTrend['배송분기'], quarterlyTrend['매출액'], quarterlyTrend['주문건수'],
            None, "매출액", "주문건수", "최근 8분기 추이",
            bar_fmt='%{y:,.0f}원', line_fmt='%{y:,d}건'
        )
        fig_8q_trend.update_traces(selector=dict(type='bar'), width=0.25)
        fig_8q_trend.update_xaxes(type='category', title_text=None)
        fig_8q_trend.update_layout(yaxis_title=None, yaxis2_title=None, showlegend=True)
        st.plotly_chart(fig_8q_trend, use_container_width=True)

        st.write('')
        st.write('')

        # ---- 7. 분기별 실적 상세 ----
        st.write("###### 분기별 실적 상세 (매출액 단위: 천 원)")

        temp_8q_dedup = df_8q.drop_duplicates(subset=['주문 번호'])

        sales_8q_raw   = temp_8q_dedup.pivot_table(index='회원등급명_요약', columns='배송분기', values='결제_final', aggfunc='sum').fillna(0)
        count_8q_raw   = temp_8q_dedup.pivot_table(index='회원등급명_요약', columns='배송분기', values='주문 번호', aggfunc='nunique').fillna(0)
        number_8q_raw  = temp_8q_dedup.pivot_table(index='회원등급명_요약', columns='배송분기', values='구분자_비회원포함', aggfunc='nunique').fillna(0)
        aov_8q_raw     = (sales_8q_raw / count_8q_raw.replace(0, float('nan'))).fillna(0)
        arppu_8q_raw   = (sales_8q_raw / number_8q_raw.replace(0, float('nan'))).fillna(0)
        basket_sku_8q  = df_8q.groupby(['회원등급명_요약', '배송분기', '주문 번호'])['상품명'].nunique().reset_index() \
            .pivot_table(index='회원등급명_요약', columns='배송분기', values='상품명', aggfunc='mean').fillna(0)
        basket_size_8q = df_8q.groupby(['회원등급명_요약', '배송분기', '주문 번호'])['상품수량'].sum().reset_index() \
            .pivot_table(index='회원등급명_요약', columns='배송분기', values='상품수량', aggfunc='mean').fillna(0)

        userGradeSalesQTrend  = (sales_8q_raw / 1000).reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeCountQTrend  = count_8q_raw.reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeNumberQTrend = number_8q_raw.reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeAovQTrend    = aov_8q_raw.reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeArppuQTrend  = arppu_8q_raw.reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeBasketSKUQT  = basket_sku_8q.reset_index().rename(columns={'회원등급명_요약': '회원등급'})
        userGradeBasketSizeQT = basket_size_8q.reset_index().rename(columns={'회원등급명_요약': '회원등급'})

        render_q_trend_detail({
            "매출액":    (userGradeSalesQTrend,  False, False),
            "주문 건수": (userGradeCountQTrend,  False, False),
            "주문자 수": (userGradeNumberQTrend, False, False),
            "건당 수량": (userGradeBasketSizeQT,  True,  True),
            "건당 SKU":  (userGradeBasketSKUQT,   True,  True),
            "AOV":       (userGradeAovQTrend,    False,  True),
            "ARPPU":     (userGradeArppuQTrend,  False,  True)
        })
