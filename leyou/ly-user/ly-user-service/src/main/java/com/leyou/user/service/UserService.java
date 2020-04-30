package com.leyou.user.service;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.dnd.DropTarget;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    //redis缓存前缀
    private static final String KEY_PREFIX = "user:verify:phone:";

    //手机验证码过期时间：单位分钟
    private static final int PHONE_VERIFY_CODE_OVERDUE_IN_MINUTE = 5;

    public Boolean checkData(String data, Integer type) {
        User record = new User();
        //数据类型判断    1.用户名 2.手机
        switch(type){
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnum.INVALID_USER_DATA_TYPE);
        }
        return userMapper.selectCount(record) == 0;
    }

    public void sendCode(String phone) {
        String key = KEY_PREFIX + phone;
        // 生成验证码:6位数，纯数字
        String code = NumberUtils.generateCode(6);
        HashMap<String, String> msg = new HashMap<>();
        msg.put("phone", phone);
        msg.put("code", code);
        // 发送短信验证码
        amqpTemplate.convertAndSend("ly.sms.exchange", "sms.verify.code",msg);

        // 保存验证码
        redisTemplate.opsForValue().set(key, code, PHONE_VERIFY_CODE_OVERDUE_IN_MINUTE, TimeUnit.MINUTES);



    }

    /**
     * 用户注册
     * @param user
     * @param code
     */
    public void register(User user, String code) {

        // 1.校验验证码
        String cacheCode = redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        if (!StringUtils.equals(cacheCode, code)) {
            throw new LyException(ExceptionEnum.INVALID_VERIFY_CODE);
        }

        // 2.生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        // 3.对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));
        user.setCreated(new Date());
        // 4.将用户信息写入数据库，完成注册
        int count = userMapper.insert(user);
    }

    /**
     * 用户登录
     * @param username
     * @param password
     * @return
     */
    public User queryUsernameAndPassword(String username, String password) {
        User record = new User();
        record.setUsername(username);
        //根据用户查询用户
        User user = userMapper.selectOne(record);
        //校验用户名
        if (user == null){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        //校验密码
        if (!StringUtils.equals(user.getPassword(), CodecUtils.md5Hex(password, user.getSalt()))){
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
        return user;
    }
}
