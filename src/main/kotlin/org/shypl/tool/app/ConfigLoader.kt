package org.shypl.tool.app

import java.io.File
import kotlin.reflect.KClass

interface ConfigLoader {
	fun imagine(name: String): List<String>
	
	fun match(file: File): Boolean
	
	fun <T : Any> load(file: File, type: KClass<out T>): T
}
