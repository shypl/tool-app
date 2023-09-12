package org.shypl.tool.app

import ch.qos.logback.classic.ClassicConstants
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE_PROPERTY
import ch.qos.logback.core.util.OptionHelper
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.slf4j.LoggerFactory
import org.shypl.tool.app.config.DurationDeserializer
import org.shypl.tool.app.config.JacksonConfigLoader
import org.shypl.tool.app.config.PropertiesConfigLoader
import org.shypl.tool.app.config.TimeStringToSecondsIntDeserializationProblemHandler
import org.shypl.tool.app.config.YamlConfigLoader
import org.shypl.tool.depin.Binder
import org.shypl.tool.depin.Injection
import org.shypl.tool.depin.addSmartProducerForAnnotatedClass
import org.shypl.tool.depin.addSmartProducerForAnnotatedParameter
import org.shypl.tool.logging.info
import org.shypl.tool.logging.ownLogger
import org.shypl.tool.utils.MaybeStartable
import org.shypl.tool.utils.Stoppable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

class Application(
	private val dir: Path,
	private val configEnv: String?,
	private val configLoaders: List<ConfigLoader>,
	modules: List<KClass<*>>,
	injections: List<Binder.() -> Unit>
) : Stoppable {
	
	internal constructor(configuration: ApplicationConfigurationImpl) : this(
		Paths.get(configuration.dir).toAbsolutePath().normalize(),
		configuration.configEnv,
		configuration.configLoaders,
		configuration.modules,
		configuration.injections
	)
	
	constructor(args: Array<String>, configuration: ApplicationConfiguration.() -> Unit) : this(
		ApplicationConfigurationImpl(args)
			.apply(configuration)
			.apply {
				
				configLoaders(
					YamlConfigLoader(),
					PropertiesConfigLoader()
				)
				
				val module = kotlinModule()
				val handler = TimeStringToSecondsIntDeserializationProblemHandler()
				
				configLoaders.filterIsInstance<JacksonConfigLoader<*>>().forEach {
					module.addDeserializer(Duration::class, DurationDeserializer())
					
					it.mapper.registerModule(module)
					it.mapper.addHandler(handler)
				}
			}
	)
	
	private val logger = ownLogger
	private val running = AtomicBoolean(true)
	private val moduleStoppers: Deque<Pair<String, Stoppable>> = LinkedList()
	
	private val name: String = Thread.currentThread().stackTrace
		.asSequence()
		.map { it.className }
		.find { it != "java.lang.Thread" && !it.startsWith("org.shypl.tool.app.") }
		?.let { it.substring(0, it.lastIndexOf('.') + 1) }
		?: ""
	
	init {
		
		
		if (OptionHelper.getSystemProperty(ClassicConstants.CONFIG_FILE_PROPERTY) == null) {
			val file = resolveConfigFile("logback.xml")
			if (file.exists()) {
				JoranConfigurator().apply {
					context = (LoggerFactory.getILoggerFactory() as LoggerContext).also(LoggerContext::reset)
					doConfigure(file)
				}
			}
		}
		
		try {
			logger.info("Starting in '$dir'")
			
			Runtime.getRuntime().addShutdownHook(Thread(::stop, "ApplicationShutdownHook"))
			
			val currentProducingModule = object {
				var clazz: KClass<*>? = null
			}
			
			val injector = Injection()
				.configure {
					addSmartProducerForAnnotatedClass(::provideApplicationConfig)
					addSmartProducerForAnnotatedParameter(::provideApplicationPath)
					addProduceObserverBefore { actual ->
						val expect = currentProducingModule.clazz
						if (expect != null && modules.any { it == actual } && expect != actual) {
							throw RuntimeException("Broken startup sequence of modules, it is expected ${expect.qualifiedName!!} but ${actual.qualifiedName!!} creates")
						}
					}
				}
				.apply {
					injections.forEach { configure(it) }
				}
				.build()
			
			modules.forEach {
				val moduleName = it.qualifiedName!!.removePrefix(name)
				logger.info { "Start module $moduleName" }
				currentProducingModule.clazz = it
				val module = injector.get(it)
				if (module is MaybeStartable) {
					val stopper = module.start()
					if (stopper != null) {
						moduleStoppers.addFirst(moduleName to stopper)
					}
				}
				else if (module is Stoppable) {
					moduleStoppers.addFirst(moduleName to module)
				}
			}
			currentProducingModule.clazz = null
			
			logger.info("Started")
		}
		catch (e: Throwable) {
			logger.error("Start fail", e)
			stop()
		}
	}
	
	override fun stop() {
		if (running.compareAndSet(true, false)) {
			logger.info("Stopping")
			
			for (stopper in moduleStoppers) {
				logger.info { "Stop module ${stopper.first}" }
				try {
					stopper.second.stop()
				}
				catch (e: Throwable) {
					logger.warn("Stop module ${stopper.first} fails", e)
				}
			}
			
			logger.info("Stopped")
		}
	}
	
	private fun resolvePath(path: String): Path {
		return dir.resolve(path)
	}
	
	private fun resolveConfigFile(path: String): File {
		if (configEnv != null && path.contains('.')) {
			val envPath = "${path.substringBeforeLast('.')}.$configEnv.${path.substringAfterLast('.')}"
			resolvePath("config/$envPath").takeIf { Files.exists(it) }?.also {
				return it.toFile()
			}
		}
		return resolvePath("config/$path").toFile()
	}
	
	private fun findConfigFile(path: String): File? {
		var file = resolveConfigFile(path)
		if (file.exists()) return file
		
		file = resolveConfigFile(path.replace('-', '/'))
		if (file.exists()) return file
		
		return null
	}
	
	private fun provideApplicationConfig(annotation: ApplicationConfig, type: KClass<out Any>): Any {
		var path = annotation.file
		if (path.isEmpty()) {
			path = type.simpleName!!
			path = if (path == "Config") {
				type.qualifiedName!!
					.removePrefix(name)
					.removeSuffix(".Config")
					.removeSuffix(".internal")
					.replace('.', '-')
			}
			else {
				path
					.replace("Config", "")
					.replaceFirstChar { it.lowercase(Locale.ROOT) }
					.replace(Regex("[A-Z]"), "-$0")
					.lowercase()
			}
		}
		
		var file = findConfigFile(path)
		if (file == null) {
			for (loader in configLoaders) {
				for (imaginePath in loader.imagine(path)) {
					file = findConfigFile(imaginePath)
					if (file != null) {
						return loader.load(file, type)
					}
				}
			}
			throw IllegalArgumentException("ApplicationConfig file '$path' not found")
		}
		
		val loader = configLoaders.find { it.match(file) }
			?: throw RuntimeException("ApplicationConfig loader for file '$path' not found")
		
		return loader.load(file, type)
	}
	
	private fun provideApplicationPath(annotation: ApplicationPath, parameter: KParameter): Any {
		val path = resolvePath(annotation.value)
		val type = parameter.type.jvmErasure
		
		return when {
			type.isSubclassOf(Path::class) -> path
			type.isSubclassOf(String::class) -> path.toString()
			type.isSubclassOf(File::class) -> path.toFile()
			else -> throw IllegalArgumentException("ApplicationPath type '$type' not supported")
		}
	}
}
