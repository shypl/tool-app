package org.shypl.tool.app

import org.shypl.tool.depin.Binder
import kotlin.reflect.KClass

interface ApplicationConfiguration {
	var dir: String
	var run: String
	var env: String?
	
	fun injection(configuration: Binder.() -> Unit)
	
	fun module(clazz: KClass<*>)
	
	fun modules(list: List<KClass<*>>)
	
	fun modules(vararg list: KClass<*>)
	
	fun configLoader(loader: ConfigLoader)
	
	fun configLoaders(list: List<ConfigLoader>)
	
	fun configLoaders(vararg list: ConfigLoader)
}

inline fun <reified T : Any> ApplicationConfiguration.module() = module(T::class)


