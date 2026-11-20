package com.example.sarisari.manager.domain.model

import java.time.ZoneId

/** The store's time zone. Daily and monthly boundaries are calculated in it. */
object StoreTime {
    val ZONE: ZoneId = ZoneId.of("Asia/Manila")
}
