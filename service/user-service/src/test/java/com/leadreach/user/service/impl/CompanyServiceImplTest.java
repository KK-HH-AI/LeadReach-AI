package com.leadreach.user.service.impl;

import com.leadreach.user.entity.Company;
import com.leadreach.user.entity.User;
import com.leadreach.user.mapper.CompanyMapper;
import com.leadreach.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 创建公司单元测试：角色校验、一人一公司、租户回填
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyMapper companyMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CompanyServiceImpl companyService;

    private User boss(Long tenantId) {
        User u = new User();
        u.setId(1L);
        u.setRole("BOSS");
        u.setTenantId(tenantId);
        return u;
    }

    @Test
    void createCompany_nameBlank_rejected() {
        Map<String, Object> resp = companyService.createCompany(1L, "  ", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("公司名称不能为空", resp.get("msg"));
    }

    @Test
    void createCompany_notBoss_rejected() {
        User employee = new User();
        employee.setId(1L);
        employee.setRole("EMPLOYEE");
        when(userMapper.selectById(1L)).thenReturn(employee);

        Map<String, Object> resp = companyService.createCompany(1L, "测试公司", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("只有老板才能创建公司", resp.get("msg"));
    }

    @Test
    void createCompany_bossAlreadyHasCompany_rejected() {
        when(userMapper.selectById(1L)).thenReturn(boss(99L));

        Map<String, Object> resp = companyService.createCompany(1L, "测试公司", null, null);
        assertEquals(1, resp.get("code"));
        assertEquals("该老板已创建过公司", resp.get("msg"));
    }

    @Test
    void createCompany_success_backfillsTenantAndReturnsProfile() {
        when(userMapper.selectById(1L)).thenReturn(boss(null));
        // 拦截 insert，模拟数据库自增主键回填
        when(companyMapper.insert(any(Company.class))).thenAnswer(inv -> {
            inv.getArgument(0, Company.class).setId(100L);
            return 1;
        });

        Map<String, Object> resp = companyService.createCompany(1L, "  测试公司  ", " 跨境电商 ", "深圳");
        assertEquals(0, resp.get("code"));

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        assertEquals(100L, data.get("companyId"));
        assertEquals("测试公司", data.get("name"));
        assertEquals("跨境电商", data.get("industry"));
        assertEquals("深圳", data.get("region"));

        // 公司 boss_id 落库 + 老板租户回填
        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getBossId());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(100L, userCaptor.getValue().getTenantId());
    }
}
