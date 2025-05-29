package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> list = new ArrayList<>();
        list.add(begin);
        while (begin.isBefore(end)) {
            begin = begin.plusDays(1);
            list.add(begin);
        }
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : list) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalDateTime.MIN.toLocalTime());
            LocalDateTime endTime = LocalDateTime.of(date, LocalDateTime.MAX.toLocalTime());
            Map map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(list, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }
}
