package org.shypl.tool.app

import org.shypl.tool.depin.Binder
import kotlin.reflect.KClass

internal class ApplicationConfigurationImpl(args: Array<String>) : ApplicationConfiguration {
	override var dir: String = args.getOrNull(0) ?: "."
	override var configEnv: String? = args.getOrNull(1)
	
	val modules = mutableListOf<KClass<*>>()
	val injections = mutableListOf<Binder.() -> Unit>()
	var configLoaders = mutableListOf<ConfigLoader>()
	
	
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