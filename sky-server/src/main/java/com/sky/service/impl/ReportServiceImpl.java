package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.ReportMapper;
import com.sky.properties.ReportExcelProperties;
import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.extractor.ExcelExtractor;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    ReportMapper reportMapper;
    @Autowired
    ReportExcelProperties reportExcelProperties;
    @Autowired
    WorkspaceServiceImpl workspaceService;

    /**
     * 营业额统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public Result<TurnoverReportVO> getTurnoverStatistics(LocalDate begin, LocalDate end) {
        LocalDate p = begin;
        ArrayList<LocalDate> dateTimes = new ArrayList<>();
        while (!p.equals(end)) {
            dateTimes.add(p);
            p = p.plusDays(1);
        }
        ArrayList<Double> count = new ArrayList<>();
        for (LocalDate dateTime : dateTimes) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("status", 5);
            map.put("begin", LocalDateTime.of(dateTime, LocalTime.MIN));
            map.put("end", LocalDateTime.of(dateTime, LocalTime.MAX));
            Double sum = reportMapper.sumByMap(map);
            if (sum == null) {
                sum = 0D;
            }
            count.add(sum);
        }

        return Result.success(TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateTimes, ","))
                .turnoverList(StringUtils.join(count, ","))
                .build());
    }

    /**
     * 用户统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        LocalDate p = begin;
        ArrayList<LocalDate> dateTimes = new ArrayList<>();
        while (!p.equals(end)) {
            dateTimes.add(p);
            p = p.plusDays(1);
        }
        ArrayList<Integer> user = new ArrayList<>();
        ArrayList<Integer> newUser = new ArrayList<>();
        for (LocalDate dateTime : dateTimes) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("begin", LocalDateTime.of(dateTime, LocalTime.MIN));
            map.put("end", LocalDateTime.of(dateTime, LocalTime.MAX));
            Integer sum = reportMapper.sumUserByDay(map);
            Integer sumUser = reportMapper.sumUser(map);
            if (sum == null) {
                sum = 0;
            }
            if (sumUser == null) {
                sumUser = 0;
            }
            newUser.add(sum);
            user.add(sumUser);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateTimes, ","))
                .totalUserList(StringUtils.join(user, ","))
                .newUserList(StringUtils.join(newUser, ","))
                .build();
    }

    /**
     * 订单统计
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        LocalDate p = begin;
        ArrayList<LocalDate> dateTimes = new ArrayList<>();
        while (!p.equals(end)) {
            dateTimes.add(p);
            p = p.plusDays(1);
        }
        ArrayList<Integer> order = new ArrayList<>();
        ArrayList<Integer> newOrder = new ArrayList<>();
        for (LocalDate dateTime : dateTimes) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("begin", LocalDateTime.of(dateTime, LocalTime.MIN));
            map.put("end", LocalDateTime.of(dateTime, LocalTime.MAX));
            map.put("status", Orders.COMPLETED);
            Integer sum = reportMapper.sumNewOrder(map);
            Integer sumOrder = reportMapper.sumOrder(map);
            if (sum == null) {
                sum = 0;
            }
            if (sumOrder == null) {
                sumOrder = 0;
            }
            order.add(sumOrder);
            newOrder.add(sum);
        }
        Double rate;
        if (sumArrayList(order) == 0) {
            rate = 1.0;
        } else {
            rate = sumArrayList(newOrder) / sumArrayList(order) * 1.0;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateTimes, ","))
                .orderCountList(StringUtils.join(newOrder, ","))
                .validOrderCountList(StringUtils.join(order, ","))
                .totalOrderCount(sumArrayList(newOrder))
                .validOrderCount(sumArrayList(order))
                .orderCompletionRate(rate)
                .build();
    }

    /**
     * 销量排行前十
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO salesTop10Report(LocalDate begin, LocalDate end) {
        ArrayList<String> name = new ArrayList<>();
        ArrayList<Integer> number = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<>();
        ArrayList<HashMap<String, Object>> result;
        map.put("begin", LocalDateTime.of(begin, LocalTime.MIN));
        map.put("end", LocalDateTime.of(end, LocalTime.MAX));
        map.put("status", Orders.COMPLETED);
        result = reportMapper.salesTop10Report(map);
        for (HashMap<String, Object> hashMap : result) {
            name.add((String) hashMap.get("name"));
            number.add(((BigDecimal) hashMap.get("number")).intValue());
        }


        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(name, ","))
                .numberList(StringUtils.join(number, ","))
                .build();
    }

    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        //1. 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        //查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        //2. 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            //获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            //填充数据--时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            //获得第4行
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

            //获得第5行
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row.getCell(4).setCellValue(businessDataVO.getUnitPrice());

            //填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                //查询某一天的营业数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                //获得某一行
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());
            }

            //3. 通过输出流将Excel文件下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            //关闭资源
            out.close();
            excel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private Integer sumArrayList(ArrayList<Integer> arrayList) {
        Integer sum = 0;
        for (Integer integer : arrayList) {
            sum += integer;
        }
        return sum;
    }
}
