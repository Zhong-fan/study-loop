package com.studyloop.common.context;

import com.studyloop.common.exception.BizException;
import com.studyloop.common.result.ErrorCode;

/**
 * 保存"当前请求是谁发的"。
 * <p>
 * 网关校验完 token 后，会把用户 id 放进请求头 X-User-Id；
 * UserInterceptor 把请求头读出来存进这里，业务代码就不必每个方法都传 userId。
 * <p>
 * 用完必须清理（拦截器的 afterCompletion 里做），否则线程池复用时会串数据。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    public static Long get() {
        return USER_ID.get();
    }

    /** 业务代码取当前用户；没取到说明没登录，直接抛 401 */
    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
