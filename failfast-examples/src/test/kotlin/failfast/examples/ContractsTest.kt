package failfast.examples

import failfast.ContextDSL
import failfast.FailFast
import failfast.describe
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.util.*

fun main() {
    FailFast.runTest()
}

// a port of https://github.com/dmcg/minutest/blob/master/core/src/test/kotlin/dev/minutest/examples/ContractsExampleTests.kt
object ContractsTest {
    val context = describe("Contracts") {
        describe("ArrayList") {
            behavesAsMutableCollection(ArrayList<String>())
        }
        describe("LinkedList") {
            behavesAsMutableCollection(LinkedList<String>())
        }
    }

    private suspend fun ContextDSL.behavesAsMutableCollection(fixture: MutableList<String>) {
        context("behaves as MutableCollection") {

            test("is empty when created") {
                expectThat(fixture.isEmpty())
            }

            test("can add") {
                fixture.add("item")
                expectThat(fixture.first()).isEqualTo("item")
            }
        }
    }
}
