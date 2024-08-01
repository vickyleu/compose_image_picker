package com.huhx.picker.viewmodel

import kotlin.reflect.KClass

actual fun KClass<*>.isAssignableFromKMP(clazz: KClass<*>): Boolean {
    return this.java.isAssignableFrom(clazz.java)
}