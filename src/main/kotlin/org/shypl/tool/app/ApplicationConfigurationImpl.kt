package org.shypl.tool.app

import org.shypl.tool.depin.Binder
import kotlin.reflect.KClass

internal class ApplicationConfigurationImpl(args: Array<String>) : ApplicationConfiguration {
	override var dir: String = "."
	override var run: String = "run/app.ru"
	override var env: String? = null
	
	val modules = mutableListOf<KClass<*>>()
	val injections = mutableListOf<Binder.() -> Unit>()
	var configLoaders = mutableListOf<ConfigLoader>()
	
	
	init {
		val query = args.getOrNull(0)
		if (query != null) {
			val m = query.split(';').associate { Pair(it.substringBefore('='), it.substringAfter('=')) }
			m["dir"]?.also { dir = it }
			m["run"]?.also { run = it }
			m["env"]?.also { env = it }
		}
	}
	
	override fun injection(configuration: Binder.() -> Unit) {
		injections.add(configuration)
	}
	
	override fun module(clazz: KClass<*>) {
		modules.add(clazz)
	}
	
	override fun modules(list: List<KClass<*>>) {
		modules.addAll(list)
	}
	
	override fun modules(vararg list: KClass<*>) {
		modules.addAll(list)
	}
	
	override fun configLoader(loader: ConfigLoader) {
		configLoaders.add(loader)
	}
	
	override fun configLoaders(list: List<ConfigLoader>) {
		configLoaders.addAll(list)
	}
	
	override fun configLoaders(vararg list: ConfigLoader) {
		configLoaders.addAll(list)
	}
}