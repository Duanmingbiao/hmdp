package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexPatterns;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Override
    public Result sendCode(String phone, HttpSession session) {
//        1.验证手机号是否有效
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //2.生成验证码
        String code = RandomUtil.randomNumbers(6);
        //3.保存验证码
        session.setAttribute("code",code);
        log.debug("验证码：" + code);
        return Result.ok("发送成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //1.获取验证码
        String code = loginForm.getCode();
        //2.判断验证码和手机号是否符合
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())
                || !code.equals(session.getAttribute("code"))
                || code == null){
            return Result.fail("手机号或验证码错误");
        }
        //3.根据手机号查询用户
//        User user = query().eq("phone", loginForm.getPhone()).one();
        User user = lambdaQuery().eq(User::getPhone, loginForm.getPhone()).one();
        if (user == null){
            user = new User();
            user.setPhone(loginForm.getPhone());
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            save(user);
        }
        session.setAttribute("user",user);
        System.out.println("用户登录信息" + user);
        return Result.ok(user);
    }

}
