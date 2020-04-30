package com.leyou.auth;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.auth.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.PrivateKey;
import java.security.PublicKey;

@RunWith(SpringRunner.class)
@SpringBootTest
public class JwtTest {

    private static final String pubKeyPath = "F:\\my_java\\leyou\\javacode\\rsa\\rsa.pub";

    private static final String priKeyPath = "F:\\my_java\\leyou\\javacode\\rsa\\rsa.pri";

    private PublicKey publicKey;    //公开秘钥

    private PrivateKey privateKey;  //私有秘钥

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        // 生成token
        String token = JwtUtils.generateToken(new UserInfo(20L, "jack"), privateKey, 5);    //通过私钥生成token令牌
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MjAsInVzZXJuYW1lIjoiamFjayIsImV4cCI6MTU2OTI4NTc1M30.Wz_RsI4a57UKC6HjV5cLGxPiDzZypt9rv5-zqHSXjsqch1gc6gF94NKarypJeGCBTrS3GeKndRxmoVnzmM6NT1NPIZYwjTUi_43tPwu3T-mIDZJqV5X-Tv-ui-dLWtRKn3UL9CQKer0NeOK9T30LUP8kLKfjCTLfQUWLdaeXa6M";

        // 解析token
        UserInfo user = JwtUtils.getInfoFromToken(token, publicKey);    // 通过公钥和用户请求中的令牌来获取token中的用户信息
        System.out.println("id: " + user.getId());
        System.out.println("userName: " + user.getUsername());
    }
}
