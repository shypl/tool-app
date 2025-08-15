package org.shypl.tool.app.config

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import java.io.File
import kotlin.reflect.KClass

class PropertiesConfigLoader : JacksonConfigLoader<JavaPropsMapper>(JavaPropsMapper()) {
	
	override fun imagine(name: String): List<String> {
		return listOf("$name.properties")
	}
	
	override fun match(file: File, type: KClass<out Any>): Boolean {
		return file.extension.lowercase() == "properties"
	}
}

