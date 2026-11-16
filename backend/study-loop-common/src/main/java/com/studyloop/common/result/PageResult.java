package com.studyloop.common.result;

import java.util.List;

/**
 * 分页返回结构。
 * <p>
 * 为什么不直接用 MyBatis-Plus 的 Page 返回给前端？
 * 因为 Page 是数据库层的对象，字段名和结构以后可能会变，
 * 转成自己的 PageResult 后，前端只依赖这个类的字段。
 */
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private List<T> records;

    public PageResult() {
    }

    public PageResult(long total, long page, long size, List<T> records) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.records = records;
    }

    public static <T> PageResult<T> of(long total, long page, long size, List<T> records) {
        return new PageResult<>(total, page, size, records);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
