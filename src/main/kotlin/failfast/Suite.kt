package failfast

import failfast.internal.ContextExecutor
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.lang.management.ManagementFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Suite(
    val rootContexts: Collection<ContextProvider>,
    private val parallelism: Int = cpus()
) {
    companion object {
        fun fromContexts(rootContexts: Collection<RootContext>, parallelism: Int = cpus()) = Suite(
            rootContexts.map { ContextProvider { it } },
            parallelism
        )

        fun fromClasses(classes: List<Class<*>>, parallelism: Int = cpus()) =
            Suite(classes.map { ObjectContextProvider(it) }, parallelism)

    }

    init {
        if (rootContexts.isEmpty()) throw EmptySuiteException()
    }


    constructor(rootContext: RootContext, parallelism: Int = cpus()) : this(
        listOf(ContextProvider { rootContext }),
        parallelism
    )

    constructor(parallelism: Int = cpus(), function: ContextLambda)
            : this(RootContext("root", function), parallelism)


    fun run(): SuiteResult {
        val threadPool =
            if (parallelism > 1) Executors.newWorkStealingPool(parallelism) else Executors.newSingleThreadExecutor()
        return try {
            threadPool.asCoroutineDispatcher().use { dispatcher ->
                runBlocking(dispatcher) {
                    val testResultChannel = Channel<TestResult>(Channel.UNLIMITED)
                    val contextInfos =
                        rootContexts.map {
                            async {
                                val context = it.getContext()
                                try {
                                    withTimeout(20000) {
                                        ContextExecutor(context, testResultChannel, this).execute()
                                    }
                                } catch (e: TimeoutCancellationException) {
                                    throw FailFastException("context ${context.name} timed out")
                                }
                            }
                        }.awaitAll()
                    val totalTests = contextInfos.sumBy { it.tests }
                    val results = (0 until totalTests).map {
                        testResultChannel.receive()
                    }
                    testResultChannel.close()
                    SuiteResult(results, results.filterIsInstance<Failed>(), contextInfos)
                }
            }
        } finally {
            threadPool.awaitTermination(100, TimeUnit.SECONDS)
            threadPool.shutdown()
        }
    }

}

internal fun uptime(): String {
    val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
    val uptime = ManagementFactory.getRuntimeMXBean().uptime
    val cpuTime = operatingSystemMXBean.processCpuTime / 1000000
    val percentage = cpuTime * 100 / uptime
    return "total:${uptime} cpu:${cpuTime}ms, pct:${percentage}%"
}

private fun cpus() = Runtime.getRuntime().availableProcessors() / 2
