package com.leyou.sms;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SmsTest {

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    public void smsTest() throws InterruptedException {
        HashMap<String, String> msg = new HashMap<>();
        msg.put("phone", "17364714533");
        msg.put("code", "8888");
        amqpTemplate.convertAndSend("ly.sms.exchange", "sms.verify.code",msg);

        Thread.sleep(10000L);
    }

}
