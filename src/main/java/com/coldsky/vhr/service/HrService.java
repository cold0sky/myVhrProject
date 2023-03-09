package com.coldsky.vhr.service;

import com.coldsky.vhr.entity.Role;
import com.coldsky.vhr.mapper.HrMapper;
import com.coldsky.vhr.entity.Hr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class HrService implements UserDetailsService {
    @Autowired
    HrMapper hrMapper;


    @Override
    // Spring security 通过这个方法加载登陆用户的信息
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Hr hr = hrMapper.loadUserByUsername(username);
        if (hr == null) {
            throw new UsernameNotFoundException("用户名不存在");
        }
        hr.setRoles(hrMapper.getHrRolesById(hr.getId()));
//        System.out.println(hr.toString());
//        List<Role> roles = new ArrayList<>();
//        roles.add(new Role(1,"ROLE_manager","部门经理"));
//
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//        String encode = encoder.encode("abc");
//        return new Hr(3, "系统管理员", "18568887789", "029-82881234", "深圳南山", true, "admin", encode, "null", roles, "http://bpic.588ku.com/element_pic/01/40/00/64573ce2edc0728.jpg");
        return hr;
    }
}
