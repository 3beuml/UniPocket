package com.unipocket.ui.common

import com.unipocket.domain.UniPocketException

/**
 * 把任意异常归一成框架错误类型。
 * 适配器自己抛的 [UniPocketException] 原样保留；其它（IO、解析、空指针……）归到 [UniPocketException.Unknown]。
 */
internal fun Throwable.toUniPocketException(): UniPocketException =
    this as? UniPocketException
        ?: UniPocketException.Unknown(message ?: this::class.simpleName ?: "未知错误", this)
