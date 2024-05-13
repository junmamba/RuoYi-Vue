package com.ruoyi.simc.service.impl;

import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.simc.domain.SimcDistrict;
import com.ruoyi.simc.domain.SimcResidentSocialInsurance;
import com.ruoyi.simc.mapper.SimcResidentSocialInsuranceMapper;
import com.ruoyi.simc.service.ISimcDistrictService;
import com.ruoyi.simc.service.ISimcResidentSocialInsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * @description:
 * @author: junmamba
 * @create: 2024-05-04 11:13
 */
@Service
public class SimcResidentSocialInsuranceServiceImpl implements ISimcResidentSocialInsuranceService {
    @Autowired
    private SimcResidentSocialInsuranceMapper simcResidentSocialInsuranceMapper;

    @Autowired
    private ISimcDistrictService simcDistrictService;

    @Override
    public List<SimcResidentSocialInsurance> selectList(SimcResidentSocialInsurance rsi) throws Exception {
        Map<String, Long> districtParam = this.simcDistrictService.buildQueryDistrictParam(rsi.getDistrictId());
        if (districtParam.containsKey("townshipDistrictId")) {
            rsi.setResidentTownshipDistrictId(districtParam.get("townshipDistrictId"));
        }
        if (districtParam.containsKey("villageDistrictId")) {
            rsi.setResidentVillageDistrictId(districtParam.get("villageDistrictId"));
        }
        if (districtParam.containsKey("groupDistrictId")) {
            rsi.setResidentGroupDistrictId(districtParam.get("groupDistrictId"));
        }
        List<SimcResidentSocialInsurance> list = this.simcResidentSocialInsuranceMapper.selectList(rsi);

        Set<Long> districtIds = new HashSet<>();
        for (int i = 0; null != list && i < list.size(); i++) {
            districtIds.add(list.get(i).getResidentTownshipDistrictId());
            districtIds.add(list.get(i).getResidentVillageDistrictId());
            districtIds.add(list.get(i).getResidentGroupDistrictId());
            list.get(i).setStrSocialInsuranceJointApprovalTime(DateUtils.parseDateToStr("yyyyMMdd", list.get(i).getSocialInsuranceJointApprovalTime()));
        }
        LocalDate currentDate = LocalDate.now();
        List<SimcDistrict> simcDistrictList = this.simcDistrictService.queryByDistrictIdList(new ArrayList<>(districtIds));
        for (int i = 0; null != list && i < list.size(); i++) {
            list.get(i).setDistrictName(simcDistrictService.getDistrictName(list.get(i).getResidentTownshipDistrictId(), list.get(i).getResidentVillageDistrictId(), list.get(i).getResidentGroupDistrictId(), simcDistrictList));
            String birthDate = DateUtils.parseDateToStr(DateUtils.YYYYMMDD, list.get(i).getResidentBirthDate());
            LocalDate birthLocalDate = LocalDate.of(Integer.parseInt(birthDate.substring(0, 4)), Integer.parseInt(birthDate.substring(4, 6)), Integer.parseInt(birthDate.substring(6, 8)));
            int year = Period.between(birthLocalDate, currentDate).getYears();
            int month = Period.between(birthLocalDate, currentDate).getMonths();

            list.get(i).setAge(month > 0 ? year + "+" : String.valueOf(year));

            int retireAge = 60;
            if ("1".equals(list.get(i).getSocialInsuranceType()) && list.get(i).getResidentSex() == 2) {
                // 城镇职工 && 女性 --> 退休年龄年龄为55，其他均为60
                retireAge = 55;
            }
            int months = year * 12 + month;
            if (months >= retireAge * 12) {// 退休
                list.get(i).setRetireState(2);
            } else if (months >= (retireAge - 1) * 12 && months < retireAge * 12) {// 即将退休
                list.get(i).setRetireState(1);
            } else {// 未退休
                list.get(i).setRetireState(0);
            }
        }
        return list;
    }
}
