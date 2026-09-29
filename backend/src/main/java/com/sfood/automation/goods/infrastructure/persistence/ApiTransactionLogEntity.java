package com.sfood.automation.goods.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "api_transaction_log")
public class ApiTransactionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "api_type", nullable = false)
    private String apiType;

    @Column(name = "godo_goods_code", nullable = false)
    private String godoGoodsCode;

    @Column(name = "request_url", nullable = false)
    private String requestUrl;

    @Column(name = "request_body")
    private String requestBody;

    @Column(name = "response_body")
    private String responseBody;

    @Column(name = "result_code")
    private String resultCode;

    @Column(name = "result_message")
    private String resultMessage;

    @Column(name = "is_success", nullable = false)
    private Integer isSuccess;

    @Column(name = "requested_at", nullable = false)
    private String requestedAt;

    @Column(name = "responded_at")
    private String respondedAt;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getApiType() { return apiType; }
    public void setApiType(String apiType) { this.apiType = apiType; }
    public String getGodoGoodsCode() { return godoGoodsCode; }
    public void setGodoGoodsCode(String godoGoodsCode) { this.godoGoodsCode = godoGoodsCode; }
    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }
    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public String getResultCode() { return resultCode; }
    public void setResultCode(String resultCode) { this.resultCode = resultCode; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public Integer getIsSuccess() { return isSuccess; }
    public void setIsSuccess(Integer isSuccess) { this.isSuccess = isSuccess; }
    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
    public String getRespondedAt() { return respondedAt; }
    public void setRespondedAt(String respondedAt) { this.respondedAt = respondedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
