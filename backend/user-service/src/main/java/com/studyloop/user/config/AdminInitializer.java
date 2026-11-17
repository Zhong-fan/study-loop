package com.studyloop.user.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.studyloop.user.entity.User;
import com.studyloop.user.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时确保管理员存在。
 * <p>
 * 早期做法是"注册名等于 admin 就给管理员"，但那意味着谁先注册谁就是管理员。
 * 现在改成由服务自己创建：已经存在就跳过，不存在就用配置里的密码建一个。
 * 密码通过 ADMIN_PASSWORD 覆盖，默认值只适合本地演示。
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserMapper userMapper;
    private final String adminUsername;
    private final String adminPassword;

    public AdminInitializer(UserMapper userMapper,
                            @Value("${app.admin.username:admin}") String adminUsername,
                            @Value("${app.admin.password:123456}") String adminPassword) {
        this.userMapper = userMapper;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", adminUsername);
        if (userMapper.selectOne(wrapper) != null) {
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(BCrypt.hashpw(adminPassword));
        admin.setNickname("管理员");
        admin.setRole("ADMIN");
        admin.setCreatedAt(LocalDateTime.now());
        admin.setDeleted(0);
        userMapper.insert(admin);

        LOG.warn("已创建默认管理员 {}，请尽快修改密码（ADMIN_PASSWORD）", adminUsername);
    }
}
