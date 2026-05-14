package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        Shop shop = queryWithPassThrough(id);
        return Result.ok(shop);
    }
    private Shop queryWithPassThrough(Long id) {
        String shopBean = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if(StrUtil.isNotBlank(shopBean)){
            return JSONUtil.toBean(shopBean, Shop.class);
        }
        if(shopBean != null ){
            return null;
        }
        String lockKey = "lock:shop:" + id;
        try {
            Boolean ishas = tryLock(lockKey);
            if(!ishas){
                Thread.sleep(500);
                return queryWithPassThrough(id);
            }
            Shop shop_k = getById(id);
            System.out.println(shop_k);
            if(shop_k == null ){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", 60L, TimeUnit.SECONDS);
                return null;
            }
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop_k));
            stringRedisTemplate.expire(CACHE_SHOP_KEY + id, 36000L, TimeUnit.MINUTES);
            return shop_k;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
    }
    private Boolean tryLock(String key){
        /*
        setIfAbsent设置成功就为true，已经有或者设置失败就为 false
         */
        return stringRedisTemplate.opsForValue().setIfAbsent("lock"+ key, "1", 10L, TimeUnit.SECONDS);
    }
    private void unLock(String key){
        stringRedisTemplate.delete("lock"+ key);
    }
    @Override
    @Transactional
    public Result updata(Shop shop) {
        if(shop.getId() == null){
            return Result.fail("店铺id不能为空");
        }
        updateById(shop);
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
