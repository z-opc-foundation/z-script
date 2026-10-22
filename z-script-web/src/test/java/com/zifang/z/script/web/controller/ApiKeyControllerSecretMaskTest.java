package com.zifang.z.script.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.meta.Result;
import com.zifang.z.script.core.domain.entity.ApiKeyDO;
import com.zifang.z.script.core.domain.service.ApiKeyService;
import com.zifang.z.script.web.dto.ApiKeyManageVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * API Key 管理面「明文不外泄 + 归属/角色收窄」的断言 (TASK-20260925-031)。
 * <p>
 * 这条尺子量三件事：
 * <ol>
 *   <li><b>出口</b>：读端点（page / list-enabled / {id}）的响应体里不得出现 api_key 明文，
 *       也不得出现 api_secret_hash（字段名与值都不许）；</li>
 *   <li><b>归属</b>：拿 A 应用的 Key 去读，只能读到 A 应用的 Key；读别人的按「不存在」处理（404）；</li>
 *   <li><b>角色</b>：scope=READ_ONLY 只读、scope=SPECIFIC 不进管理面、没有 API Key 身份直接 401。</li>
 * </ol>
 * <p>
 * 为什么不用 Mockito / MockMvc：z-script-web 的测试栈只有 JUnit 5（根 pom 直接条目），
 * 加 Spring Boot 测试栈要动依赖图。桩件用 JDK 动态代理现造（ApiKeyService 是接口），
 * 不引新依赖，也就不动发版形状。
 */
class ApiKeyControllerSecretMaskTest {

    /** app-a 的那把 Key 的明文（假值，只用于断言它不出现在响应体里）。 */
    private static final String KEY_A = "zsk_live_abcdef0123456789abcdef01";

    /** app-b 的那把 Key 的明文。 */
    private static final String KEY_B = "zsk_live_1234567890abcdef12345678";

    /** app-a 那把 Key 的 secret 哈希（假值）。 */
    private static final String HASH_A = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    private final ObjectMapper mapper = new ObjectMapper();

    // ============ 用例 ============

    @Test
    @DisplayName("page：响应体不含 api_key 明文与 api_secret_hash")
    void pageResponseCarriesNoPlaintextKey() throws Exception {
        FakeApiKeyService svc = new FakeApiKeyService();
        svc.keys.add(key(1L, "app-a", KEY_A, HASH_A));
        ApiKeyController controller = controllerOf(svc);

        Result<IPage<ApiKeyManageVO>> result = controller.page(1, 20, null, null, requestWith(caller("app-a", "ALL")));

        assertTrue(result.isSuccess(), "带有效身份的读请求应当成功");
        String json = mapper.writeValueAsString(result);
        assertFalse(json.contains(KEY_A), "响应体不得含 api_key 明文，实际：" + json);
        assertFalse(json.contains(HASH_A), "响应体不得含 api_secret_hash 的值，实际：" + json);
        assertFalse(json.toLowerCase().replace("_", "").contains("secrethash"),
                "响应体不得出现 api_secret_hash 字段名，实际：" + json);

        ApiKeyManageVO vo = result.getData().getRecords().get(0);
        assertEquals("zsk_live_abcd…ef01", vo.getApiKey(), "api_key 应为掩码");
        assertEquals("app-a", vo.getAppName(), "appName 字段名要保留（前端/e2e 按它取值）");
        assertEquals(Long.valueOf(1L), vo.getId());
    }

    @Test
    @DisplayName("list-enabled：响应体不含明文，且只出本应用的 Key")
    void listEnabledIsMaskedAndScoped() throws Exception {
        FakeApiKeyService svc = new FakeApiKeyService();
        svc.keys.add(key(1L, "app-a", KEY_A, HASH_A));
        svc.keys.add(key(2L, "app-b", KEY_B, "b-should-not-appear"));
        ApiKeyController controller = controllerOf(svc);

        Result<List<ApiKeyManageVO>> result = controller.listEnabled(requestWith(caller("app-a", "ALL")));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size(), "只应看到本应用的 Key");
        assertEquals("app-a", result.getData().get(0).getAppName());

        String json = mapper.writeValueAsString(result);
        assertFalse(json.contains(KEY_A), "响应体不得含 api_key 明文，实际：" + json);
        assertFalse(json.contains(KEY_B), "别的应用的 Key 不得出现在响应体里，实际：" + json);
        assertFalse(json.contains("b-should-not-appear"), "别的应用的 secret 哈希不得出现，实际：" + json);
    }

    @Test
    @DisplayName("详情：读别人的 Key 按「不存在」处理（404），不回数据")
    void getForeignKeyIsNotFound() throws Exception {
        FakeApiKeyService svc = new FakeApiKeyService();
        svc.keys.add(key(1L, "app-a", KEY_A, HASH_A));
        svc.keys.add(key(2L, "app-b", KEY_B, "b-should-not-appear"));
        ApiKeyController controller = controllerOf(svc);

        Result<ApiKeyManageVO> result = controller.get(2L, requestWith(caller("app-a", "ALL")));

        assertFalse(result.isSuccess());
        assertEquals(404, result.getCode());
        assertNull(result.getData(), "越权读不应回任何 Key 数据");
    }

    @Test
    @DisplayName("角色：READ_ONLY 只读、SPECIFIC 不进管理面、无身份 401")
    void scopeActsAsRole() throws Exception {
        FakeApiKeyService svc = new FakeApiKeyService();
        svc.keys.add(key(1L, "app-a", KEY_A, HASH_A));
        ApiKeyController controller = controllerOf(svc);

        // READ_ONLY：读得到
        Result<IPage<ApiKeyManageVO>> readOnly = controller.page(1, 20, null, null, requestWith(caller("app-a", "READ_ONLY")));
        assertTrue(readOnly.isSuccess(), "READ_ONLY 应当可读");

        // READ_ONLY：改不了
        Result<Boolean> disable = controller.disable(1L, requestWith(caller("app-a", "READ_ONLY")));
        assertFalse(disable.isSuccess());
        assertEquals(403, disable.getCode());

        // SPECIFIC：管理面进不来
        Result<IPage<ApiKeyManageVO>> specific = controller.page(1, 20, null, null, requestWith(caller("app-a", "SPECIFIC")));
        assertFalse(specific.isSuccess());
        assertEquals(403, specific.getCode());

        // 没有 API Key 身份
        Result<IPage<ApiKeyManageVO>> anonymous = controller.page(1, 20, null, null, requestWith(null));
        assertFalse(anonymous.isSuccess());
        assertEquals(401, anonymous.getCode());
    }

    @Test
    @DisplayName("写端点：改别人的 Key 判 404，且不落到 service")
    void writeOnForeignKeyIsRejected() throws Exception {
        FakeApiKeyService svc = new FakeApiKeyService();
        svc.keys.add(key(1L, "app-a", KEY_A, HASH_A));
        svc.keys.add(key(2L, "app-b", KEY_B, "b-should-not-appear"));
        ApiKeyController controller = controllerOf(svc);
        svc.calls.clear();

        Result<Boolean> disable = controller.disable(2L, requestWith(caller("app-a", "ALL")));
        assertFalse(disable.isSuccess());
        assertEquals(404, disable.getCode());
        assertFalse(svc.calls.contains("disable"), "越权写不应落到 service，实际调用：" + svc.calls);
    }

    @Test
    @DisplayName("掩码函数：保留前缀与首尾 4 位，不含完整明文")
    void maskKeepsNoPlaintext() {
        assertEquals("zsk_live_abcd…ef01", ApiKeyManageVO.mask(KEY_A));
        assertFalse(ApiKeyManageVO.mask(KEY_A).contains("abcdef0123456789abcdef01"));
        assertNull(ApiKeyManageVO.mask(null));
        assertNull(ApiKeyManageVO.mask(""));
        assertNotNull(ApiKeyManageVO.mask(KEY_B));
    }

    // ============ 桩件 ============

    /** 调用方 Key（模拟拦截器放进请求上下文的那一把）。 */
    private static ApiKeyDO caller(String appName, String scope) {
        ApiKeyDO caller = new ApiKeyDO();
        caller.setId(100L);
        caller.setAppName(appName);
        caller.setScope(scope);
        caller.setApiKey("zsk_live_caller0000000000000000");
        return caller;
    }

    private static ApiKeyDO key(Long id, String appName, String apiKey, String secretHash) {
        ApiKeyDO k = new ApiKeyDO();
        k.setId(id);
        k.setAppName(appName);
        k.setAppId(null);
        k.setOwnerId("anonymous");
        k.setScope("ALL");
        k.setStatus(1);
        k.setApiKey(apiKey);
        k.setApiSecretHash(secretHash);
        return k;
    }

    /** 假 service：只实现本用例用到的方法，其余一律回 null（代理会拦下 IService 的 default 方法）。 */
    private static final class FakeApiKeyService implements InvocationHandler {

        final List<ApiKeyDO> keys = new ArrayList<>();
        final List<String> calls = new ArrayList<>();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            switch (name) {
                case "page": {
                    Page<ApiKeyDO> page = new Page<>(1, 20, keys.size());
                    page.setRecords(new ArrayList<>(keys));
                    return page;
                }
                case "listEnabled":
                    return new ArrayList<>(keys);
                case "getById": {
                    Long id = (Long) args[0];
                    for (ApiKeyDO k : keys) {
                        if (id.equals(k.getId())) {
                            return k;
                        }
                    }
                    return null;
                }
                case "enable":
                case "disable":
                case "removeById":
                    calls.add(name);
                    return Boolean.TRUE;
                case "resetSecret":
                    calls.add(name);
                    return "fresh-plain-secret";
                case "toString":
                    return "FakeApiKeyService";
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "equals":
                    return proxy == args[0];
                default:
                    return null;
            }
        }
    }

    private static ApiKeyService serviceOf(FakeApiKeyService fake) {
        return (ApiKeyService) Proxy.newProxyInstance(
                ApiKeyService.class.getClassLoader(),
                new Class<?>[]{ApiKeyService.class},
                fake);
    }

    private static ApiKeyController controllerOf(FakeApiKeyService fake) throws Exception {
        ApiKeyController controller = new ApiKeyController();
        Field field = ApiKeyController.class.getDeclaredField("apiKeyService");
        field.setAccessible(true);
        field.set(controller, serviceOf(fake));
        return controller;
    }

    /** 假请求：只实现 getAttribute("z.apiKeyEntity")，其余回 null。 */
    private static HttpServletRequest requestWith(ApiKeyDO caller) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null && args.length == 1) {
                        return "z.apiKeyEntity".equals(args[0]) ? caller : null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeRequest";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    return null;
                });
    }
}
