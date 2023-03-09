package com.coldsky.vhr.mapper;

import com.coldsky.vhr.model.AdjustSalary;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface AdjustSalaryMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(AdjustSalary record);

    int insertSelective(AdjustSalary record);

    AdjustSalary selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(AdjustSalary record);

    int updateByPrimaryKey(AdjustSalary record);
}