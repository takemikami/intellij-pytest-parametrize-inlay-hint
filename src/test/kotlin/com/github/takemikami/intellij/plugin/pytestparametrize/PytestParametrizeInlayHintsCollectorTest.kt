package com.github.takemikami.intellij.plugin.pytestparametrize

import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase

class PytestParametrizeInlayHintsCollectorTest<PytestParametrizeInlayHintsProvider> : InlayHintsProviderTestCase() {

    fun testSimple() {
        val text = """
            import pytest
            @pytest.mark.parametrize(
              "x, y",
              [
                /*<# [0:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/1),
                /*<# [1:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/2),
              ]
            )
            def test_foo(x, y):
              pass
        """.trimIndent()
        testAnnotations(text)
    }

    fun testListLiteral() {
        val text = """
            import pytest
            @pytest.mark.parametrize(
              ["x", "y"],
              [
                /*<# [0:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/1),
                /*<# [1:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/2),
              ]
            )
            def test_foo(x, y):
              pass
        """.trimIndent()
        testAnnotations(text)
    }

    fun testParenthesized() {
        val text = """
            import pytest
            @pytest.mark.parametrize(
              ("x", "y"),
              [
                /*<# [0:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/1),
                /*<# [1:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/2),
              ]
            )
            def test_foo(x, y):
              pass
        """.trimIndent()
        testAnnotations(text)
    }

    fun testIds() {
        val text = """
            import pytest
            @pytest.mark.parametrize(
              "x, y",
              [
                /*<# [case1:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/1),
                /*<# [case2:  ] #>*/(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/2),
              ],
              ids=["case1", "case2"]
            )
            def test_foo(x, y):
              pass
        """.trimIndent()
        testAnnotations(text)
    }

    fun testPytestParam() {
        val text = """
            import pytest
            @pytest.mark.parametrize(
              "x, y",
              [
                /*<# [case1:  ] #>*/pytest.param(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/1, id="case1"),
                /*<# [case2:  ] #>*/pytest.param(/*<# [x:  ] #>*/0, /*<# [y:  ] #>*/2, id="case2"),
              ]
            )
            def test_foo(x, y):
              pass
        """.trimIndent()
        testAnnotations(text)
    }

    @Suppress("UnstableApiUsage")
    private fun testAnnotations(text: String) {
        doTestProvider(
            "test_foo.py",
            text,
            PytestParametrizeInlayHintsProvider()
        )
    }
}
