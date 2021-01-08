package failfast.internal

import failfast.Failed
import failfast.Ignored
import failfast.Success
import failfast.TestResult

class Junit4Reporter(private val testResults: List<TestResult>) {
    fun stringReport(): List<String> {
        val result = mutableListOf("<testsuite tests=\"${testResults.size}\">")
        testResults.forEach {

            val line = when (it) {
                is Success ->
                    listOf("""<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}"/>""")
                is Failed -> {
                    listOf(
                        """<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}">""",
                        """<failure message="${it.failure.message}">""",
                        ExceptionPrettyPrinter(it.failure).stackTrace.joinToString("\n"),
                        """</failure>""",
                        """</testcase>"""
                    )
                }
                is Ignored -> {
                    listOf(
                        """<testcase classname="${it.test.parentContext.stringPath()}" name="${it.test.testName}">""",
                        """<skipped/></testcase>"""
                    )
                }
            }
            result.addAll(line)
        }
        result.add("</testsuite>")
        return result
    }

}