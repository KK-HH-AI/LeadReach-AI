package com.leadreach.crm.dto.req;

import lombok.Data;

/**
 * 创建线索请求：一次提交「采购商公司 + 联系人 + 线索」
 */
@Data
public class LeadCreateRequest {

    /** 线索标题，缺省用公司名 */
    private String title;
    /** 来源渠道，缺省 MANUAL */
    private String sourceType;
    private String sourceUrl;
    /** 线索备注 */
    private String remark;

    /** 采购商公司（必填） */
    private CompanyInfo company;
    /** 主联系人（可选） */
    private ContactInfo contact;

    @Data
    public static class CompanyInfo {
        private String name;
        private String countryCode;
        private String regionCode;
        private String website;
        private String industryCode;
        private String industryName;
        private String address;
    }

    @Data
    public static class ContactInfo {
        private String name;
        private String jobTitle;
        private String department;
        private String seniority;
        private String email;
        private String phone;
        private String whatsapp;
        private String linkedinUrl;
        private String countryCode;
        private String language;
        /** 是否关键人 */
        private Boolean isKp;
    }
}