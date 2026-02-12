package io.github.romanvht.mtgandroid.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.InterruptedIOException

object MtgWrapper {
    private const val TAG = "MtgWrapper"
    private var mtgProcess: Process? = null
    private var stdoutThread: Thread? = null
    private var stderrThread: Thread? = null

    fun generateSecret(context: Context, domain: String): String? {
        return try {
            val mtgBinary = getMtgBinary(context)
            if (mtgBinary == null) {
                Log.e(TAG, "Failed to get MTG binary")
                return null
            }

            Log.d(TAG, "Generating secret with domain: $domain")
            Log.d(TAG, "Binary path: ${mtgBinary.absolutePath}")

            val processBuilder = ProcessBuilder(
                mtgBinary.absolutePath,
                "generate-secret",
                domain
            )

            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            val output = StringBuilder()
            val errorOutput = StringBuilder()

            val stdoutReader = Thread {
                try {
                    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.d(TAG, "MTG stdout: $line")
                            output.append(line).append("\n")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading stdout", e)
                }
            }

            val stderrReader = Thread {
                try {
                    BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.e(TAG, "MTG stderr: $line")
                            errorOutput.append(line).append("\n")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading stderr", e)
                }
            }

            stdoutReader.start()
            stderrReader.start()

            val exitCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)
                process.exitValue()
            } else {
                process.waitFor()
            }

            stdoutReader.join(1000)
            stderrReader.join(1000)

            Log.d(TAG, "Process exit code: $exitCode")

            if (exitCode == 0) {
                val secret = output.toString().trim()
                if (secret.isNotEmpty()) {
                    Log.d(TAG, "Generated secret successfully")
                    secret
                } else {
                    Log.e(TAG, "Secret is empty")
                    null
                }
            } else {
                Log.e(TAG, "generate-secret failed with exit code: $exitCode")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating secret", e)
            null
        }
    }

    fun startProxy(context: Context, bindAddress: String, secret: String): Boolean {
        return try {
            stopProxy()

            val mtgBinary = getMtgBinary(context)
            if (mtgBinary == null) {
                Log.e(TAG, "Failed to get MTG binary")
                return false
            }

            Log.d(TAG, "Starting proxy on $bindAddress")

            val processBuilder = ProcessBuilder(
                mtgBinary.absolutePath,
                "simple-run",
                bindAddress,
                secret
            )

            processBuilder.directory(context.filesDir)
            processBuilder.redirectErrorStream(true)
            mtgProcess = processBuilder.start()

            stdoutThread = Thread {
                try {
                    BufferedReader(InputStreamReader(mtgProcess!!.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.d(TAG, "MTG: $line")
                        }
                    }
                } catch (e: InterruptedIOException) {
                    Log.d(TAG, "MTG output reader interrupted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading MTG output", e)
                }
            }

            stderrThread = Thread {
                try {
                    BufferedReader(InputStreamReader(mtgProcess!!.errorStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            Log.e(TAG, "MTG error: $line")
                        }
                    }
                } catch (e: InterruptedIOException) {
                    Log.d(TAG, "MTG error reader interrupted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading MTG errors", e)
                }
            }

            stdoutThread?.start()
            stderrThread?.start()

            Thread.sleep(500)

            val isAlive = isProcessAlive(mtgProcess)
            if (isAlive) {
                Log.d(TAG, "MTG proxy started successfully on $bindAddress")
            } else {
                Log.e(TAG, "MTG process died immediately")
            }

            isAlive
        } catch (e: Exception) {
            Log.e(TAG, "Error starting proxy", e)
            false
        }
    }

    fun stopProxy() {
        try {
            mtgProcess?.let { process ->
                if (isProcessAlive(process)) {
                    process.destroy()

                    Thread.sleep(500)

                    stdoutThread?.join(1000)
                    stderrThread?.join(1000)

                    if (isProcessAlive(process) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        process.destroyForcibly()
                    }

                    Log.d(TAG, "MTG proxy stopped")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping proxy", e)
        } finally {
            mtgProcess = null
            stdoutThread = null
            stderrThread = null
        }
    }

    private fun isProcessAlive(process: Process?): Boolean {
        if (process == null) return false

        return try {
            process.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
    }

    private fun getMtgBinary(context: Context): File? {
        return try {
            val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
            val nativeLibFile = File(nativeLibDir, "libmtg.so")

            if (nativeLibFile.exists() && nativeLibFile.canExecute()) {
                Log.d(TAG, "Using native library: ${nativeLibFile.absolutePath}")
                return nativeLibFile
            }

            Log.e(TAG, "Native library not found or not executable")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting MTG binary", e)
            null
        }
    }
}
