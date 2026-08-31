package com.kiezmarkt.listing.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * RFC 9457 problem details. Every error response in this service uses this
 * shape with media type {@code application/problem+json}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Problem {

    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;

    public Problem() {
    }

    public Problem(String type, String title, int status, String detail, String instance) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }
}
