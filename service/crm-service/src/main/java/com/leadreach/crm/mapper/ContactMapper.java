package com.leadreach.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leadreach.crm.entity.Contact;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ContactMapper extends BaseMapper<Contact> {
}