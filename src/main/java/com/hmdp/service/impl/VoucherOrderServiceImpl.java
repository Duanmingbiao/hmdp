package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements IVoucherOrderService {
    @Autowired
    private SeckillVoucherServiceImpl seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    private static final String SECKILL_NOT_START = "秒杀尚未开始";
    private static final String SECKILL_ENDED = "秒杀已经结束";
    private static final String STOCK_INSUFFICIENT = "库存不足";
    private static final String ALREADY_PURCHASED = "您已经购买过一次";

    @Override
    public Object seckillVoucher(Long voucherId) {
        // 1. 校验秒杀时间
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher.getBeginTime().after(new Date())) {
            return SECKILL_NOT_START;
        }
        if (voucher.getEndTime().before(new Date())) {
            return SECKILL_ENDED;
        }

        // 2. 库存校验
        if (voucher.getStock() < 1) {
            return STOCK_INSUFFICIENT;
        }

        // 3. 一人一单（锁用户ID）
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) { // 注意：使用 intern() 确保字符串常量池
            // 获取代理对象（必须通过代理才能让 @Transactional 生效）,且此处创建代理必须在接口的基础上，所以转为接口型
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.markOrder(voucherId, voucher, userId);
        }
    }

    @Transactional(rollbackFor = Exception.class)  // 明确指定回滚异常
    public Object markOrder(Long voucherId, SeckillVoucher voucher, Long userId) {
        // 3.1 一人一单校验（双重检查）
        Integer orderNum = lambdaQuery()
                .eq(VoucherOrder::getUserId, userId)
                .eq(VoucherOrder::getVoucherId, voucherId)  // 建议加上券ID校验
                .count();
        if (orderNum > 0) {
            log.warn("用户{}已购买过秒杀券{}", userId, voucherId);
            return ALREADY_PURCHASED;
        }

        // 3.2 扣减库存（乐观锁）
        boolean update = seckillVoucherService.lambdaUpdate()
                .set(SeckillVoucher::getStock, voucher.getStock() - 1)
                .eq(SeckillVoucher::getVoucherId, voucherId)
                .eq(SeckillVoucher::getStock, voucher.getStock() > 0)  // CAS 乐观锁
                .update();
        if (!update) {
            log.warn("库存扣减失败，券ID:{}", voucherId);
            return STOCK_INSUFFICIENT;
        }

        // 3.3 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        log.info("用户{}秒杀成功，订单ID:{}", userId, orderId);
        return orderId;
    }
}