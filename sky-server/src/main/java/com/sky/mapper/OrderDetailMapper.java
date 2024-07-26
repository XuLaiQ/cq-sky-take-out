package com.sky.mapper;

import com.sky.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author ：xulai
 * @File ：== orderDetailMapper.py ==
 * @Date ：2024/7/26 18:31
 * @Describe:
 **/
@Mapper
public interface OrderDetailMapper {
    /**
     * 插入多条订单明细数据
     * @param orderDetails
     */
    void insertBatch(ArrayList<OrderDetail> orderDetails);

    /**
     * 根据订单id查询订单明细
     * @param id
     * @return
     */
    @Select("select * from order_detail where order_id = #{id}")
    List<OrderDetail> getByOrderId(Long id);
}
