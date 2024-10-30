package com.xxl.mq.admin.service.impl;

import com.xxl.mq.admin.core.model.XxlMqBiz;
import com.xxl.mq.admin.core.result.ReturnT;
import com.xxl.mq.admin.dao.IXxlMqMessageDao;
import com.xxl.mq.admin.dao.IXxlMqTopicDao;
import com.xxl.mq.admin.service.IXxlMqBizService;
import com.xxl.mq.admin.service.IXxlMqMessageService;
import com.xxl.mq.client.consumer.annotation.MqConsumer;
import com.xxl.mq.client.message.XxlMqMessage;
import com.xxl.mq.client.message.XxlMqMessageStatus;
import com.xxl.mq.client.util.DateUtil;
import com.xxl.mq.client.util.LogHelper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Created by xuxueli on 16/8/28.
 */
@Service
public class XxlMqMessageServiceImpl implements IXxlMqMessageService {


    @Resource
    private IXxlMqMessageDao xxlMqMessageDao;
    @Resource
    private IXxlMqBizService xxlMqBizService;
    @Resource
    private IXxlMqTopicDao xxlMqTopicDao;


    @Override
    public Map<String, Object> pageList(int offset, int pagesize, String topic, String status, Date addTimeStart, Date addTimeEnd) {

        List<XxlMqMessage> list = xxlMqMessageDao.pageList(offset, pagesize, topic, status, addTimeStart, addTimeEnd);
        int total = xxlMqMessageDao.pageListCount(offset, pagesize, topic, status, addTimeStart, addTimeEnd);

        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("data", list);
        maps.put("recordsTotal", total);
        maps.put("recordsFiltered", total);
        return maps;
    }

    @Override
    public ReturnT<String> delete(int id) {
        int ret = xxlMqMessageDao.delete(id);
        return ret>0 ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @Override
    public ReturnT<String> update(XxlMqMessage message) {

        // valid id
        if (message.getId() < 1){
            return new ReturnT<String>(500, "参数非法");
        }

        // valid message
        ReturnT<String> validRet = validMessage(message);
        if (validRet != null) {
            return validRet;
        }

        // log
        String appendLog = LogHelper.makeLog("人工修改", message.toString());
        message.setLog(appendLog);

        // update
        int ret = xxlMqMessageDao.update(message);
        return ret>0 ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    private static ReturnT<String> validMessage(XxlMqMessage mqMessage){

        if (mqMessage.getId() < 1) {    // add

            // topic
            if (mqMessage.getTopic()==null || mqMessage.getTopic().trim().length()==0) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-mq, topic empty.");
            }
            if (!(mqMessage.getTopic().length()>=4 && mqMessage.getTopic().length()<=255)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-mq, topic length invalid[4~255].");
            }

            // group
            if (mqMessage.getGroup()==null || mqMessage.getGroup().trim().length()==0) {
                mqMessage.setGroup(MqConsumer.DEFAULT_GROUP);
            }
            if (!(mqMessage.getGroup().length()>=4 && mqMessage.getGroup().length()<=255)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-mq, group length invalid[4~255].");
            }
        }

        // data
        if (mqMessage.getData() == null) {
            mqMessage.setData("");
        }
        if (mqMessage.getData().length() > 20000) {
            throw new IllegalArgumentException("xxl-mq, data length invalid[0~60000].");
        }

        // status
        //mqMessage.setStatus(XxlMqMessageStatus.NEW.name());
        if (XxlMqMessageStatus.valueOf(mqMessage.getStatus()) == null) {
            return new ReturnT<String>(500, "消息状态非法");
        }

        // retryCount
        if (mqMessage.getRetryCount() < 0) {
            mqMessage.setRetryCount(0);
        }

        // shardingId
        if (mqMessage.getShardingId() < 0) {
            mqMessage.setShardingId(0);
        }

        // delayTime
        if (mqMessage.getEffectTime() == null) {
            mqMessage.setEffectTime(new Date());
        }

        // timeout
        if (mqMessage.getTimeout() < 0) {
            mqMessage.setTimeout(0);
        }

        // log

        return null;
    }

    @Override
    public ReturnT<String> add(XxlMqMessage message) {

        // valid message
        ReturnT<String> validRet = validMessage(message);
        if (validRet != null) {
            return validRet;
        }

        // log
        String appendLog = LogHelper.makeLog("人工添加", message.toString());
        message.setLog(appendLog);

        // save
        xxlMqMessageDao.save(Arrays.asList(message));
        return ReturnT.SUCCESS;
    }

    @Override
    public Map<String, Object> dashboardInfo() {

        int bizCount = 0;
        int topicCount = 0;
        int messageCount = 0;

        List<XxlMqBiz> bizList = xxlMqBizService.findAll();
        bizCount = bizList!=null?bizList.size():0;
        topicCount = xxlMqTopicDao.pageListCount(0, 1, -1, null);
        messageCount = xxlMqMessageDao.pageListCount(0, 1, null, null, null, null);

        Map<String, Object> dashboardMap = new HashMap<String, Object>();
        dashboardMap.put("bizCount", bizCount);
        dashboardMap.put("topicCount", topicCount);
        dashboardMap.put("messageCount", messageCount);
        return dashboardMap;
    }

    @Override
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {

        // process
        List<String> messageDay_list = new ArrayList<String>();
        List<Integer> newNum_list = new ArrayList<Integer>();
        List<Integer> ingNum_list = new ArrayList<Integer>();
        List<Integer> successNum_list = new ArrayList<Integer>();
        List<Integer> failNum_list = new ArrayList<Integer>();

        int newNum_total = 0;
        int ingNum_total = 0;
        int successNum_total = 0;
        int failNum_total = 0;


        List<Map<String, Object>> triggerCountMapAll = xxlMqMessageDao.messageCountByDay(startDate, endDate);
        if (triggerCountMapAll!=null && triggerCountMapAll.size()>0) {
            for (Map<String, Object> item: triggerCountMapAll) {

                String messageDay = String.valueOf(item.get("messageDay"));
                int newNum = Integer.valueOf(String.valueOf(item.get("newNum")));
                int ingNum = Integer.valueOf(String.valueOf(item.get("ingNum")));
                int successNum = Integer.valueOf(String.valueOf(item.get("successNum")));
                int failNum = Integer.valueOf(String.valueOf(item.get("failNum")));

                messageDay_list.add(messageDay);
                newNum_list.add(newNum);
                ingNum_list.add(ingNum);
                successNum_list.add(successNum);
                failNum_list.add(failNum);

                newNum_total += newNum;
                ingNum_total += ingNum;
                successNum_total += successNum;
                failNum_total += failNum;
            }
        } else {
            for (int i = 4; i > -1; i--) {
                String messageDay = DateUtil.formatDate(new Date());

                messageDay_list.add(messageDay);
                newNum_list.add(0);
                ingNum_list.add(0);
                successNum_list.add(0);
                failNum_list.add(0);
            }
        }


        Map<String, Object> result = new HashMap<String, Object>();
        result.put("messageDay_list", messageDay_list);
        result.put("newNum_list", newNum_list);
        result.put("ingNum_list", ingNum_list);
        result.put("successNum_list", successNum_list);
        result.put("failNum_list", failNum_list);

        result.put("newNum_total", newNum_total);
        result.put("ingNum_total", ingNum_total);
        result.put("successNum_total", successNum_total);
        result.put("failNum_total", failNum_total);

        return new ReturnT<Map<String, Object>>(result);
    }

    @Override
    public ReturnT<String> clearMessage(String topic, String status, int type) {
        xxlMqMessageDao.clearMessage(topic, status, type);
        return ReturnT.SUCCESS;
    }

}
