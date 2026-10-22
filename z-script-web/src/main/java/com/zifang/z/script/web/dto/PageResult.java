package com.zifang.z.script.web.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应体（TASK-20260925-035）。
 *
 * <p><b>为什么不用 MyBatis-Plus 的 {@code IPage} 直接序列化</b>：
 * <ul>
 *   <li>MP 的 {@code IPage.total} 是 {@code long}，而本仓 classpath 上的
 *       {@code z-boot-web-starter} 的 {@code GlobalJacksonConfig} 全局把 {@code Long/long}
 *       序列化成**字符串**（防 JS 精度丢失）⇒ 未开 count 时是 {@code "0"}，
 *       开了 count 也是 {@code "131"} 这种字符串。</li>
 *   <li>前端分页控件要的是**真计数**：字符串 "0" 会让「共 N 条」渲染成 0，
 *       而字符串 "131" 又要求消费方每次都记得 {@code Number()}。</li>
 * </ul>
 * ⇒ 这里显式定义响应体：<b>分页相关的标量一律是 JSON 数字</b>
 * （{@code total} 用 {@link BigInteger} 承载，避开 long→String 的全局序列化，同时不溢出）。
 *
 * <p><b>字段口径</b>（与 MP {@code IPage} 对齐，前端可直接替换）：
 * <ul>
 *   <li>{@code records} —— 本页数据行（长度 ≤ {@code size}）；</li>
 *   <li>{@code total} —— **满足当前过滤条件的总行数**（服务端 count 出来的真计数），
 *       不是本页行数，也不是字符串；</li>
 *   <li>{@code current} / {@code size} —— 本次请求**实际生效**的页码与页长
 *       （size 超过上限时会被收敛，响应里回显收敛后的值）；</li>
 *   <li>{@code pages} —— 总页数 = ceil(total / size)。</li>
 * </ul>
 *
 * @param <T> 行类型
 */
public class PageResult<T> {

    /**
     * 单页最大行数。超过它按它收敛 —— 防止有人用 {@code size=100000} 把整表拉走
     * （前端过去拿 {@code size=200} 当「取全量」的暗号，就是这个洞）。
     */
    public static final int MAX_PAGE_SIZE = 500;

    private List<T> records = new ArrayList<>();
    private int current;
    private int size;
    private int pages;
    private BigInteger total = BigInteger.ZERO;

    public PageResult() {
    }

    /**
     * 由 MP 的分页对象装配响应体。
     *
     * @param page    已经过分页拦截器处理的分页对象（records 可能是全表，交给调用方替换）
     * @param records 真正要出参的行
     * @param <T>     行类型
     * @return 响应体
     */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        PageResult<T> out = new PageResult<>();
        out.setRecords(records == null ? new ArrayList<>() : records);
        out.setCurrent((int) page.getCurrent());
        out.setSize((int) page.getSize());
        out.setTotal(BigInteger.valueOf(page.getTotal()));
        out.setPages((int) page.getPages());
        return out;
    }

    /**
     * 收敛分页参数：页码从 1 起，页长落在 [1, {@link #MAX_PAGE_SIZE}]。
     *
     * @param current 请求页码
     * @param size    请求页长
     * @return 生效的页码
     */
    public static int normalizeCurrent(long current) {
        return current < 1 ? 1 : (int) Math.min(current, Integer.MAX_VALUE);
    }

    /**
     * 收敛分页参数：页码从 1 起，页长落在 [1, {@link #MAX_PAGE_SIZE}]。
     *
     * @param size 请求页长
     * @return 生效的页长
     */
    public static int normalizeSize(long size) {
        if (size < 1) {
            return 1;
        }
        return (int) Math.min(size, MAX_PAGE_SIZE);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public BigInteger getTotal() {
        return total;
    }

    public void setTotal(BigInteger total) {
        this.total = total == null ? BigInteger.ZERO : total;
    }
}
