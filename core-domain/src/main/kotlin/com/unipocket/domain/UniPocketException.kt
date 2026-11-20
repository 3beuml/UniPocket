package com.unipocket.domain

/**
 * 框架统一错误类型。界面只认这四种，据此决定提示文案与操作按钮
 * （重试 / 重新登录 / 反馈）。
 *
 * 适配器在接自家教务时，把各种底层异常（IOException、解析失败、HTTP 401…）
 * 归到最贴近的一类里再抛出。
 */
sealed class UniPocketException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** 网络不可达、超时、服务器 5xx 等，可以重试 */
    class Network(message: String, cause: Throwable? = null) : UniPocketException(message, cause)

    /** 登录态失效（401 / 会话过期），需要重新登录 */
    class AuthExpired(
        message: String = "登录状态已失效，请重新登录",
        cause: Throwable? = null,
    ) : UniPocketException(message, cause)

    /** 接口返回的数据不符合预期（字段缺失、格式变化），通常意味着教务改版 */
    class DataFormat(message: String, cause: Throwable? = null) : UniPocketException(message, cause)

    /** 其它未归类错误，界面按普通失败处理 */
    class Unknown(message: String, cause: Throwable? = null) : UniPocketException(message, cause)
}

/**
 * 把任意异常归一成框架错误类型。
 *
 * 适配器自己抛的 [UniPocketException] 原样保留；其它（IO、解析、空指针……）
 * 归到 [UniPocketException.Unknown]，保证界面永远只需要处理四类。
 */
fun Throwable.toUniPocketException(): UniPocketException =
    this as? UniPocketException
        ?: UniPocketException.Unknown(message ?: this::class.simpleName ?: "未知错误", this)
