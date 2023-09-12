package org.shypl.tool.app.config

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.File

class YamlConfigLoader : JacksonConfigLoader<YAMLMapper>(YAMLMapper()) {
	override fun imagine(name: String): List<String> {
		return listOf(
			"$name.yml",
			"$name.yaml",
		)
	}
	
	override fun match(file: File): Boolean {
		return when (file.extension.lowercase()) {
			"yml", "yaml" -> true
			else          -> false
		}
	}
}
