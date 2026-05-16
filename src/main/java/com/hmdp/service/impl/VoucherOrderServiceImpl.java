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
import org.springframework.beans.factory.annotation.Autowired;
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
@Slf4j
@Service
@Transactional
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Autowired
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;

    @Override
    public Object seckillVoucher(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        System.out.println(voucher);
        if (voucher.getBeginTime().after(new Date())) {
            return "秒杀尚未开始";
        }
        if (voucher.getEndTime().before(new Date())) {
            return "秒杀已经结束";
        }
        if (voucher.getStock() < 1){
            return "库存不足";
        }
        log.info("开始下单");
        boolean update = seckillVoucherService.lambdaUpdate()
                .set(SeckillVoucher::getStock, voucher.getStock() - 1)
                .eq(SeckillVoucher::getVoucherId, voucherId).update();
        if (!update) {
            return "库存不足";
        }
        log.info("下单成功");
        VoucherOrder voucherOrder = new VoucherOrder();
        long order = redisIdWorker.nextId("order");
        voucherOrder.setId(order);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return order;
    }
}
