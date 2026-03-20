package com.medical.common.pagination;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> {
    private Long currentPage;
    private Long pageSize;
    private Long total;
    private List<T> list;

    public static <T> PageResult<T> fromPage(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.total = page.getTotal();
        result.pageSize = page.getSize();
        result.currentPage = page.getCurrent();
        result.list = page.getRecords();
        return result;
    }
}
