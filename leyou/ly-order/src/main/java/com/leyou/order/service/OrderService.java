package com.leyou.order.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.IdWorker;
import com.leyou.item.pojo.Sku;
import com.leyou.order.client.AddressClient;
import com.leyou.order.client.GoodsClient;
import com.leyou.order.dto.AddressDTO;
import com.leyou.common.dto.CartDTO;
import com.leyou.order.dto.OrderDTO;
import com.leyou.order.enums.OrderStatusEnum;
import com.leyou.order.enums.PayState;
import com.leyou.order.interceptor.UserInterceptor;
import com.leyou.order.mapper.OrderDetailMapper;
import com.leyou.order.mapper.OrderMapper;
import com.leyou.order.mapper.OrderStatusMapper;
import com.leyou.order.pojo.Order;
import com.leyou.order.pojo.OrderDetail;
import com.leyou.order.pojo.OrderStatus;
import com.leyou.order.utils.PayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    @Autowired
    private OrderStatusMapper statusMapper;

    @Autowired
    private IdWorker idworker;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private PayHelper payHelper;

    /**
     * 创建订单
     * @param orderDTO
     * @return
     */
    @Transactional
    public Long createOrder(OrderDTO orderDTO) {
        Order order = new Order();

        // 1.新增订单
        // 1.1 订单编号，基本信息
        long orderId = idworker.nextId();
        order.setOrderId(orderId);
        order.setCreateTime(new Date());
        order.setPaymentType(orderDTO.getPaymentType());

        // 1.2 用户信息
        UserInfo user = UserInterceptor.getUser();
        order.setUserId(user.getId());
        order.setBuyerNick(user.getUsername());
        order.setBuyerRate(false);

        // 1.3 收货人地址
        // 获取收货人信息
        AddressDTO addr = AddressClient.findById(orderDTO.getAddressId());
        order.setReceiver(addr.getName());
        order.setReceiverAddress(addr.getAddress());
        order.setReceiverCity(addr.getCity());
        order.setReceiverDistrict(addr.getDistrict());
        order.setReceiverMobile(addr.getPhone());
        order.setReceiverState(addr.getState());
        order.setReceiverZip(addr.getZipCode());

        // 1.4 金额
        //将orderDTO装换为一个map，key是skuId,value是商品数量num
        Map<Long, Integer> numMap = orderDTO.getCarts()
                .stream().collect(Collectors.toMap(CartDTO::getSkuId, CartDTO::getNum));

        // 获取所有的skuId
        Set<Long> skuIds = numMap.keySet();
        // 使用skuIds查询所有sku
        List<Sku> skus = goodsClient.querySkuByIds(new ArrayList<>(skuIds));

        //准备orderDetail集合
        List<OrderDetail> details = new ArrayList<>();
        //计算总金额
        long totalPay = 0L;
        for (Sku sku : skus) {
            totalPay += sku.getPrice() * numMap.get(sku.getId());

            //封装的details
            OrderDetail detail = new OrderDetail();
            detail.setImage(StringUtils.substringBefore(sku.getImages(), ","));
            detail.setNum(numMap.get(sku.getId()));
            detail.setOrderId(orderId);
            detail.setOwnSpec(sku.getOwnSpec());
            detail.setSkuId(sku.getId());
            detail.setTitle(sku.getTitle());
            detail.setPrice(sku.getPrice());
            details.add(detail);
        }
        order.setTotalPay(totalPay);
        //实付金额 = 总金额 + 邮费 - 优惠
        order.setActualPay(totalPay + order.getPostFee() - 0);

        // 1.5写入数据库
        int count = orderMapper.insertSelective(order);
        if (count != 1) {
            log.error("[创建订单] 创建订单失败，orderId：{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_ERROR);
        }

        // 2.新增订单详情
        count = detailMapper.insertList(details);
        if (count != details.size()) {
            log.error("[创建订单] 创建订单详情失败，orderId：{}", orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_DETAIL_ERROR);
        }

        // 3.新增订单状态
        OrderStatus orderStatus = new OrderStatus();
        orderStatus.setCreateTime(order.getCreateTime());
        orderStatus.setOrderId(orderId);
        orderStatus.setStatus(OrderStatusEnum.UN_PAY.value());
        count = statusMapper.insertSelective(orderStatus);
        if (count != 1) {
            log.error("[创建订单] 订单状态写入数据库失败，orderId：{}",orderId);
            throw new LyException(ExceptionEnum.CREATE_ORDER_STATUS_ERROR);
        }
        // 4.减少对应商品库存
        List<CartDTO> cartDTOS = orderDTO.getCarts();
        goodsClient.decreaseStock(cartDTOS);

        return orderId;
    }

    public Order queryOrderById(Long id) {
        Order order = orderMapper.selectByPrimaryKey(id);
        if (order == null){
            throw new LyException(ExceptionEnum.ORDER_NOT_FOUND);
        }

        //查询订单详情
        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getOrderId());
        List<OrderDetail> details = detailMapper.select(detail);
        if (CollectionUtils.isEmpty(details)) {
            throw new LyException(ExceptionEnum.ORDER_DETAIL_NOT_FOUND);
        }
        order.setOrderDetails(details);

        //查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(order.getOrderId());
        if (orderStatus == null) {
            throw new LyException(ExceptionEnum.ORDER_STATUS_NOT_FOUND);
        }
        order.setOrderStatus(orderStatus);
        return order;
    }

    public String createPayUrl(Long id) {
        //查询订单
        Order order = queryOrderById(id);
        //判断订单状态是否是未支付状态
        if (order.getOrderStatus().getStatus() != OrderStatusEnum.UN_PAY.value()) {
            throw new LyException(ExceptionEnum.ORDER_STATUS_ERROR);
        }
        //获取实付金额
        Long actualPay = /*order.getActualPay();*/ 1L;  //测试只支付1分钱
        //获取商品描述
        String dsc = order.getOrderDetails().get(0).getTitle();
        return payHelper.createOrder(id, actualPay, dsc);
    }

    public void handleNotify(Map<String, String> result) {

        //校验数据
        payHelper.isSuccess(result);

        //校验签名
        payHelper.isValidSign(result);

        //校验金额
        //获取商品的金额
        String outTradeNoStr = result.get("out_trade_no");
        //获取微信服务端的金额
        String totalFeeStr = result.get("total_fee");
        if (StringUtils.isBlank(totalFeeStr) || StringUtils.isBlank(outTradeNoStr)) {
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }
        //获取结果中的金额
        Long totalFee = Long.valueOf(totalFeeStr);
        //获取订单中的金额
        Long trandeNo = Long.valueOf(outTradeNoStr);
        Order order = orderMapper.selectByPrimaryKey(trandeNo);
        if (totalFee != /*order.getActualPay()*/ 1L){
            //金额不符
            throw new LyException(ExceptionEnum.INVALID_ORDER_PARAM);
        }

        //修改订单状态
        OrderStatus status = new OrderStatus();
        status.setOrderId(order.getOrderId());
        status.setStatus(OrderStatusEnum.PAYED.value());
        status.setPaymentTime(new Date());
        int count = statusMapper.updateByPrimaryKeySelective(status);
        if (count != 1) {
            throw new LyException(ExceptionEnum.UPDATE_ORDER_STATUS_ERROR);
        }

        log.info("[订单回调]，订单支付成功！订单编号：{}", order.getOrderId());

    }

    public PayState queryStateById(Long orderId) {
        //查询订单状态
        OrderStatus orderStatus = statusMapper.selectByPrimaryKey(orderId);
        Integer status = orderStatus.getStatus();
        //判断是否支付
        if (status != OrderStatusEnum.UN_PAY.value()){
            //证明是真的支付了
            return PayState.SUCCESS;
        }

        //如果未支付，可能不一定没有支付，必须去微信查询支付状态
        return payHelper.queryPayState(orderId);
    }
}
