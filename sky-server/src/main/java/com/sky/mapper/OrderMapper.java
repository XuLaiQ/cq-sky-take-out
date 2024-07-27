package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @Author ：xulai
 * @File ：== orderMapper.py ==
 * @Date ：2024/7/26 18:27
 * @Describe:
 **/
@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据订单号查询订单
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    OrderVO details(Long id);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 获取配送超时的订单
     * @param pendingPayment
     * @return
     */
    List<Orders> getByStatusAndOrderTimeLT(@Param("pendingPayment") Integer pendingPayment, @Param("time") LocalDateTime time);

    /**
     * 批量修改派送状态
     * @param ordersList
     */
    void updateBatchStatus(@Param("ordersList") List<Orders> ordersList,@Param("status") Integer status);

    /**
     * 查询总订单数
     * @param map
     * @return
     */
    Integer countByMap(@Param("map") Map map);

    /**
     * 统计营业额
     * @param map
     * @return
     */
    Double sumByMap(@Param("map")Map map);

}
