package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ExceptionEnum {

    BRAND_NOT_FOUND(404,"品牌没查到！"),
    CATEGORY_NOT_FOUND(404,"商品分类信息没查到！"),
    SPEC_GROUP_NOT_FOUND(404,"商品规格组不存在！"),
    SPEC_PARAM_NOT_FOUND(404,"商品规格组参数不存在！"),
    GOODS_NOT_FOUND(404,"商品不存在！"),
    GOODS_DETAIL_NOT_FOUND(404,"商品详情不存在！"),
    GOODS_SKU_NOT_FOUND(404,"商品SKU不存在！"),
    GOODS_STOCK_NOT_FOUND(404,"商品STOCK不存在！"),
    CART_NOT_FOUND(404, "用户购物车不存在！"),
    BRAND_SAVE_ERROR(500,"新增品牌失败！"),
    SPEC_GROUP_SAVE_ERROR(500,"新增商品规格组失败！"),
    SPEC_PARAM_SAVE_ERROR(500,"新增商品规格参数失败！"),
    GOODS_SAVE_ERROR(500,"新增商品失败！"),
    SPEC_GROUP_UPDATE_ERROR(500,"更新商品规格组失败！"),
    SPEC_PARAM_UPDATE_ERROR(500,"更新商品规格参数失败！"),
    GOODS_UPDATE_ERROR(500,"更新商品失败！"),
    SPEC_GROUP_DELETE_ERROR(500,"删除商品规格组失败！"),
    SPEC_PARAM_DELETE_ERROR(500,"删除商品规格参数失败！"),
    UPLOAD_FILE_ERROR(500,"文件上传失败！"),
    USER_REGISTER_ERROR(500,"服务器内部异常，用户注册失败！"),
    INVALID_FILE_TYPE(400,"无效的文件类型！"),
    GOODS_ID_CANNOT_BE_NULL(400,"商品Id不能为空！"),
    INVALID_USER_DATA_TYPE(400,"用户数据类型不匹配！"),
    INVALID_VERIFY_CODE(400,"用户注册验证码有误！"),
    INVALID_USERNAME_PASSWORD(400,"用户名或密码有误！"),
    CREATE_TOKEN_ERROR(500, "用户凭证生成失败！"),
    UNAUTHORIZED(403, "未授权！"),
    CREATE_ORDER_ERROR(500, "创建订单失败！"),
    CREATE_ORDER_DETAIL_ERROR(500, "创建订单详情失败！"),
    CREATE_ORDER_STATUS_ERROR(500, "创建订单状态失败！"),
    STOCK_NOT_ENOUGH(500, "商品库存不足！"),
    ORDER_NOT_FOUND(404, "订单信息不存在！"),
    ORDER_DETAIL_NOT_FOUND(404, "订单详情信息不存在！"),
    ORDER_STATUS_NOT_FOUND(404, "订单状态信息不存在！"),
    WX_PAY_ORDER_FAIL(500, "微信下单失败！"),
    ORDER_STATUS_ERROR(400, "订单状态异常！"),
    INVALID_ORDER_PARAM(400, "订单参数异常！"),
    UPDATE_ORDER_STATUS_ERROR(500, "修改订单状态失败！"),
    ;

    private int code;   //状态码
    private String msg; //异常信息

}
