package com.hifnawy.pre.build

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.io.IOException

sealed interface Error
typealias RootError = Error

sealed interface Result<out D, out E : RootError> {
    data class Success<out D>(val stdout: D) : Result<D, Nothing>
    data class Error<out E : RootError>(val error: E) : Result<Nothing, E>
}

class ExecutionError(val errorCode: Int, val errorMessage: String) : Error
data class ProcessMetadata(val exitCode: Int, val stdout: String, val stderr: String)

open class PreBuildPluginEx {

    lateinit var generateSampleData: TaskProvider<Task>
    lateinit var generateSurahDrawables: TaskProvider<Task>
}

@Suppress("LoggingSimilarMessage")
class PreBuildPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("preBuildPlugin", PreBuildPluginEx::class.java)

        val helperScriptsDir = File(project.rootDir, "helper-scripts")
        val venvDir = File(helperScriptsDir, ".venv")
        val generateSampleDataScript = File(helperScriptsDir, "generateSampleData.py")
        val generateSurahDrawablesScript = File(helperScriptsDir, "generateSurahDrawables.py")

        val checkUv = project.tasks.register("checkUv") {
            doLast {
                val result = executeCommand(workingDir = helperScriptsDir, command = "uv --version")
                when (result) {
                    is Result.Success -> project.logger.lifecycle(result.stdout)
                    is Result.Error   -> {
                        project.logger.error("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                        throw GradleException("uv is not installed or not found in PATH. Please install uv: https://docs.astral.sh/uv/getting-started/installation/")
                    }
                }
            }
        }

        val createVenv = project.tasks.register("createVenv") {
            dependsOn(checkUv)
            doLast {
                val result = executeCommand(workingDir = helperScriptsDir, command = "uv venv .venv")
                when (result) {
                    is Result.Success -> project.logger.lifecycle(result.stdout)
                    is Result.Error   -> {
                        project.logger.error("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                        throw GradleException("Failed to create venv")
                    }
                }
            }
            onlyIf { !venvDir.exists() }
        }

        val syncVenv = project.tasks.register("syncVenv") {
            dependsOn(createVenv)
            doLast {
                val result = executeCommand(workingDir = helperScriptsDir, command = "uv sync")
                when (result) {
                    is Result.Success -> project.logger.lifecycle(result.stdout)
                    is Result.Error   -> {
                        project.logger.error("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                        throw GradleException("Failed to sync venv")
                    }
                }
            }
        }

        val generateSampleData = project.tasks.register("generateSampleData") {
            dependsOn(syncVenv)
            doLast {
                val result = executeCommand(workingDir = helperScriptsDir, command = "uv run ${generateSampleDataScript.name}")
                when (result) {
                    is Result.Success -> project.logger.lifecycle(result.stdout)
                    is Result.Error   -> {
                        project.logger.error("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                        throw GradleException("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                    }
                }
            }
        }

        val generateSurahDrawables = project.tasks.register("generateSurahDrawables") {
            dependsOn(syncVenv)
            doLast {
                val result = executeCommand(workingDir = helperScriptsDir, command = "uv run ${generateSurahDrawablesScript.name} --headless")
                when (result) {
                    is Result.Success -> project.logger.lifecycle(result.stdout)
                    is Result.Error   -> {
                        project.logger.error("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                        throw GradleException("ERROR ${result.error.errorCode}: ${result.error.errorMessage}")
                    }
                }
            }
        }

        extension.generateSampleData = generateSampleData
        extension.generateSurahDrawables = generateSurahDrawables
    }

    fun executeCommand(workingDir: File? = null, command: String): Result<String, ExecutionError> {
        return try {
            val commandWithArgs = command.split(" ").toTypedArray()
            val (exitCode, stdout, stderr) = processBuilder(workingDir = workingDir, args = commandWithArgs)

            when (exitCode) {
                0    -> Result.Success(stdout = stdout.trim())
                else -> Result.Error(ExecutionError(exitCode, stderr))
            }
        } catch (ex: IOException) {
            Result.Error(ExecutionError(ex.hashCode(), ex.message.toString()))
        }
    }

    fun processBuilder(workingDir: File? = null, vararg args: String): ProcessMetadata {
        val processBuilder = ProcessBuilder(*args)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .apply {
                environment().putAll(System.getenv())
                workingDir?.let { directory(it) }
            }

        val process = processBuilder.start()

        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()

        return ProcessMetadata(exitCode, stdout, stderr)
    }
}
