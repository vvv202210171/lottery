package com.xxl.mq.admin.service;

import com.xxl.mq.admin.core.model.XxlMqBiz;
import com.xxl.mq.admin.core.result.ReturnT;

import java.util.List;

/**
 * @author xuxueli 2018-11-20
 */
public interface IXxlMqBizService {

    public List<XxlMqBiz> findAll();

    public XxlMqBiz load(int id);

    public ReturnT<String> add(XxlMqBiz xxlMqBiz);

    public ReturnT<String> update(XxlMqBiz xxlMqBiz);

    public ReturnT<String> delete(int id);

}
