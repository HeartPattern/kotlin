// "Add explicit import" "true"
// LANGUAGE_VERSION: 1.2

open class Bar {
    companion object {
        class FromBarCompanion
    }
}

class Foo : Bar() {
    val a: <caret>FromBarCompanion? = null
}