package com.mallcai.fulfillment.pe.biz.service.task;

import com.mallcai.fulfillment.pe.biz.service.inner.ProduceOrderPushService;
import com.mallcai.fulfillment.pe.common.constants.Constants;
import com.mallcai.fulfillment.pe.common.utils.DateUtil;
import com.mallcai.fulfillment.pe.common.utils.MdcTraceIdUtil;
import com.mallcai.fulfillment.pe.common.utils.RedisDistributeLockUtil;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * @description: 订单推送task
 * @author: chentao
 * @create: 2019-08-26 21:26:17
 */
@JobHandler(value = "orderPushTask")
@Slf4j
@Component
public class OrderPushTask extends IJobHandler {

    @Autowired
    private RedisDistributeLockUtil redisDistributeLockUtil;

    @Autowired
    private ProduceOrderPushService produceOrderPushService;

    private final long LOCK_EXPIRE_TIME = 10 * 60L;

    @Override
    public ReturnT<String> execute(String cron) {

        MdcTraceIdUtil.addTraceId();

        String requestId = UUID.randomUUID().toString();
        String lockKey = Constants.REDIS_LOCK_PREFIX + Constants.REDIS_LOCK_KEY_ORDER_PUSH;
        boolean lockResult = false;
        try {
             lockResult = redisDistributeLockUtil.tryGetDistributedLock(lockKey, requestId, LOCK_EXPIRE_TIME);
            if (lockResult == false) {
                log.info("获取锁失败,lockKey:{},requestId:{}", lockKey, requestId);
                return FAIL;
            }
            Date todayStart = DateUtil.todayStart();
            log.info("执行推送发货单任务开始,日期:{}", DateUtil.formatDate(todayStart));
            produceOrderPushService.pushProduceOrder(todayStart, DateUtil.addDays(todayStart, 1));
            log.info("执行推送发货单任务结束,日期:{}", DateUtil.formatDate(todayStart));
        } catch (Exception e) {
            log.error("执行推送发货单任务异常", e);
            throw e;
        } finally {
            if (lockResult == true) {
                redisDistributeLockUtil.releaseDistributedLock(lockKey, requestId);
            }
        }
        return SUCCESS;
    }

}
