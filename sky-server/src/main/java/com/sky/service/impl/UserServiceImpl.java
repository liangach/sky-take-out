package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.security.auth.login.LoginException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    private static final String WX_LOG = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 调用微信接口服务，获取当前微信用户的openid
        String code = userLoginDTO.getCode();  // 获取当前对象的授权码
        String openid = getOpenId(code);
        // 判断openid是否为空，如果为空表示失败，抛出业务异常
        if(openid == null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        // 判断当前用户是否为新用户
        User user = userMapper.getByOpenId(openid);
        // 如果是新用户，自动完成注册
        if(user == null){
            user = new User();
            user.setOpenid(openid);
            user.setCreateTime(LocalDateTime.now());
            userMapper.insert(user);
        }
        // 返回这个用户对象
        return user;
    }

    public String getOpenId(String code){
        // 封装请求参数
        Map map = new HashMap();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        // 调用工具类向微信接口服务发送请求
        String json = HttpClientUtil.doGet(WX_LOG, map);
        log.info("微信登录返回结果：{}", json);

        // 解析字符串
        JSONObject jsonObject = JSON.parseObject(json);
        // 根据key值获取value的值-openid的值
        String openid = jsonObject.getString("openid");
        log.info("微信用户的openid为：{}", openid);
        return openid;
    }
}
