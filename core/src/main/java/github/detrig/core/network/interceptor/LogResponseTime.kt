package github.detrig.core.network.interceptor

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.FUNCTION

/**
 * Запросы RestApi, помеченные аннотацией LogResponseTime, могут быть измерены
 * и отправлены в аналитику как техническая метрика времени ответа.
 *
 * Чтобы аннотация работала, нужно добавить обработку на уровне создания RestApi/Retrofit.
 */
@Target(FUNCTION)
@Retention(RUNTIME)
annotation class LogResponseTime(val eventName: String, val paramType: String)
