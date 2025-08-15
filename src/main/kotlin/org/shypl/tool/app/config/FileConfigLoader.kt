package org.shypl.tool.app.config

import org.shypl.tool.app.ConfigLoader
import java.io.File
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

class FileConfigLoader : ConfigLoader {
	
	override fun imagine(name: String): List<String> {
		return emptyList()
	}
	
	override fun match(file: File, type: KClass<out Any>): Boolean {
		return type.isSubclassOf(Path::class) || type.isSubclassOf(File::class)
	}
	
	override fun <T : Any> load(file: File, type: KClass<out T>): T {
		@Suppress("UNCHECKED_CAST")
		return when {
			type.isSubclassOf(Path::class) -> file.toPath()
			type.isSubclassOf(File::class) -> file
			else -> throw IllegalStateException()
		} as T
	}
}