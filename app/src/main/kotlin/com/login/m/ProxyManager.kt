package com.login.m

import kotlin.random.Random

object ProxyManager {

    data class ProxyEntity(val host: String, val port: Int)

    // قائمة خوادم البروكسي لدول مختلفة
    private val proxyPool = listOf(
        ProxyEntity("us.proxy.example.com", 8080),
        ProxyEntity("uk.proxy.example.com", 8080),
        ProxyEntity("de.proxy.example.com", 8080),
        ProxyEntity("fr.proxy.example.com", 8080)
    )

    fun getRandomProxy(): ProxyEntity {
        return proxyPool[Random.nextInt(proxyPool.size)]
    }
}
