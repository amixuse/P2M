package com.p2m.core.internal.config

import com.p2m.core.config.P2MConfigManager
import com.p2m.core.log.ILogger

internal class InternalP2MConfigManager : P2MConfigManager {
    override var logger: ILogger? = null
}