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
        String shopBean = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY + id);
        if(StrUtil.isNotBlank(shopBean)){
            Shop shop = JSONUtil.toBean(shopBean, Shop.class);
            return Result.ok(shop);
        }
        if(shopBean != null ){
            return Result.fail("店铺不存在");
        }
        Shop shop_k = getById(id);
        System.out.println(shop_k);
        if(shop_k == null ){
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, "", 60L, TimeUnit.SECONDS);
           return Result.fail("店铺不存在");
        }
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(shop_k));
        stringRedisTemplate.expire(CACHE_SHOP_KEY + id, 36000L, TimeUnit.MINUTES);
        return Result.ok(shop_k);
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
