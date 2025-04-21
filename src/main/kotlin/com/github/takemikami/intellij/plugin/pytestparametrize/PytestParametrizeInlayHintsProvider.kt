package com.github.takemikami.intellij.plugin.pytestparametrize

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayGroup
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class PytestParametrizeInlayHintsProvider : InlayHintsProvider<PytestParametrizeInlayHintsProvider.Settings> {
    companion object {
        private val settingsKey: SettingsKey<Settings> = SettingsKey("python.inlay.pytest_parametrize")
    }

    data class Settings(
        var showParametrizeOrderHints: Boolean = true,
        var showParametrizeNameHints: Boolean = true,
    )

    override val key: SettingsKey<Settings> = settingsKey
    override val name: String = "Pytest Parametrize Hint"
    override val previewText: String? = null
    override val group: InlayGroup = InlayGroup.PARAMETERS_GROUP

    override fun createSettings(): Settings = Settings()

    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: Settings,
        sink: InlayHintsSink,
    ): InlayHintsCollector? = PytestParametrizeInlayHintsCollector(editor, settings)

    override fun createConfigurable(settings: Settings): ImmediateConfigurable =
        object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JComponent = panel { }

            override val cases: List<ImmediateConfigurable.Case> =
                listOf(
                    ImmediateConfigurable.Case(
                        "pytest parametrize order hints",
                        "hints.pytest.parametrize.order",
                        settings::showParametrizeOrderHints,
                    ),
                    ImmediateConfigurable.Case(
                        "pytest parametrize name hints",
                        "hints.pytest.parametrize.names",
                        settings::showParametrizeNameHints,
                    ),
                )
        }
}
