package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getShopTypeList() {
        String shopTypeBean = stringRedisTemplate.opsForValue().get(RedisConstants.SHOP_TYPE);
        if(StrUtil.isNotBlank(shopTypeBean)){
            List<ShopType> shopType = JSONUtil.toList(shopTypeBean, ShopType.class);
            return Result.ok(shopType);
        }
        List<ShopType> list = lambdaQuery().orderByAsc(ShopType::getSort).list();
        stringRedisTemplate.opsForValue().set(RedisConstants.SHOP_TYPE, JSONUtil.toJsonStr(list));
        stringRedisTemplate.expire(RedisConstants.SHOP_TYPE, 60000L, TimeUnit.MINUTES);
        return Result.ok(list);
    }
}
