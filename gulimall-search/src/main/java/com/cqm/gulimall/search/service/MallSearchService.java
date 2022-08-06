package com.cqm.gulimall.search.service;

import com.cqm.gulimall.search.vo.SearchParam;
import com.cqm.gulimall.search.vo.SearchResult;

public interface MallSearchService {

    /**
     *
     * @param searchParam 检索所有参数
     * @return 返回检索结果
     * 里面包含页面的所有信息
     */

    SearchResult search(SearchParam searchParam);

}
