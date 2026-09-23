package com.leadreach.crm.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页结构：{list, total, page, size}
 */
@Data
public class PageResult<T> {

    /** 当前页数据 */
    private List<T> list;
    /** 总条数 */
    private long total;
    /** 当前页码，从 1 开始 */
    private long page;
    /** 每页条数 */
    private long size;

    public static <T> PageResult<T> of(IPage<T> p) {
        PageResult<T> r = new PageResult<>();
        r.setList(p.getRecords());
        r.setTotal(p.getTotal());
        r.setPage(p.getCurrent());
        r.setSize(p.getSize());
        return r;
    }

    /** 把查询结果换成另一批对象（如实体转 VO）后仍保持分页信息 */
    public static <T> PageResult<T> of(IPage<?> p, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.setList(records);
        r.setTotal(p.getTotal());
        r.setPage(p.getCurrent());
        r.setSize(p.getSize());
        return r;
    }
}