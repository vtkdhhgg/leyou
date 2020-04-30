package com.leyou.order.utils;

import com.github.wxpay.sdk.WXPay;
import com.github.wxpay.sdk.WXPayConstants;
import com.github.wxpay.sdk.WXPayUtil;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.order.config.PayConfig;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.github.wxpay.sdk.WXPayConstants.SUCCESS;
import static com.github.wxpay.sdk.WXPayConstants.UNIFIEDORDER_URL;


@Slf4j
@Component
public class PayHelper {

    @Autowired
    private WXPay wxPay;

    @Autowired
    private PayConfig payConfig;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    public String createOrder(Long orderId, Long totalPay, String desc){
        try {
            Map<String, String> data = new HashMap<>();
            // 商品描述
            data.put("body", desc);
            // 订单号
            data.put("out_trade_no", orderId.toString());
            //金额，单位是分
            data.put("total_fee", totalPay.toString());
            //调用微信支付的终端IP
            data.put("spbill_create_ip", "127.0.0.1");
            //回调地址
            data.put("notify_url", payConfig.getNotifyUrl());
            // 交易类型为扫码支付
            data.put("trade_type", "NATIVE");

            // 利用wxPay工具,完成下单
            Map<String, String> result = wxPay.unifiedOrder(data);

            // 判断通信和业务标示
            isSuccess(result);

            // 校验签名
            isValidSign(result);

            // 下单成功，获取支付链接
            String url = result.get("code_url");
            return url;
        } catch (Exception e) {
            log.error("[微信下单] 创建预交易订单异常失败", e);
            return null;
        }
    }

    /**
     * 校验数据
     * @param result
     */
    public void isSuccess(Map<String, String> result) {
        // 判断通信标示
        String returnCode = result.get("return_code");
        if("FAIL".equals(returnCode)){
            // 通信失败
            log.error("[微信下单] 微信下单通信失败,失败原因:{}", result.get("return_msg"));
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }

        // 判断业务标示
        String resultCode = result.get("result_code");
        if("FAIL".equals(resultCode)){
            // 通信失败
            log.error("[微信下单] 微信下单业务失败,错误码:{}, 错误原因:{}",
                    result.get("err_code"), result.get("err_code_des"));
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }
    }


    //校验签名
    public void isValidSign(Map<String, String> data) {

        try {
            //重新生成签名
            String sign1 = WXPayUtil.generateSignature(data, this.payConfig.getKey(), WXPayConstants.SignType.HMACSHA256);
            String sign2 = WXPayUtil.generateSignature(data, this.payConfig.getKey(), WXPayConstants.SignType.MD5);

            //将重新生成的签名与传过来的签名比较
            String sign = data.get("sign");
            if (!StringUtils.equals(sign1, sign) && !StringUtils.equals(sign2, sign)) {
                throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
            }

        }catch (Exception e){
            log.error("[微信支付] 校验签名失败， 错误数据:{}", data);
            throw new LyException(ExceptionEnum.WX_PAY_ORDER_FAIL);
        }
    }

    public PayState queryPayState(Long orderId){


        try {
            //准备集合参数
            Map<String, String> data = new HashMap<>();
            //填入订单号
            data.put("out_trade_no", orderId.toString());
            //查询订单状态
            Map<String, String> result = wxPay.orderQuery(data);

            //校验状态
            isSuccess(result);

            //校验签名
            isValidSign(result);

            //校验金额
            String tradeNo = result.get("out_trade_no");
            String totalFeeStr = result.get("total_fee");
            if (StringUtils.isEmpty(totalFeeStr) || StringUtils.isEmpty(tradeNo)) {
                throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
            }
            //获取结果中的金额
            Long totalFee = Long.valueOf(totalFeeStr);
            //获取订单中的金额
            Order order = orderMapper.selectByPrimaryKey(tradeNo);
            if (totalFee != /*order.getActualPay()*/ 1L){
                //金额不符
                throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
            }

            String state = result.get("trade_state");
            if (SUCCESS.equals(state)) {
                //支付成功
                //修改订单状态
                OrderStatus status = new OrderStatus();
                status.setStatus(OrderStatusEnum.PAYED.value());
                status.setOrderId(order.getOrderId());
                status.setPaymentTime(new Date());
                int count = statusMapper.updateByPrimaryKeySelective(status);
                if (count != 1) {
                    throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
                }
                //返回成功
                return PayState.SUCCESS;
            }

            if ("USERPAYING".equals(state) || "NOTPAY".equals(state)) {
                return PayState.NOT_PAY;
            }

            return PayState.FAIL;
        } catch (Exception e) {
            return PayState.NOT_PAY;// 并不知道是否支付，再去发起请求申请一次
        }
    }
}
