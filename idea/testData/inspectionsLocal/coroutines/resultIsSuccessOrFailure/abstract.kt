// FIX: Unwrap 'Result' return type (breaks use-sites!)
package kotlin

class Result<T>(val value: T?) {
    fun getOrThrow(): T = value ?: throw AssertionError("")
}

abstract class Abstract {
    abstract fun <caret>foo(): Result<Int>
}
