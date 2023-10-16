package git.semver.plugin.changelog

class ChangeLogFormat {
    companion object {
        /**
         * Creates a change log breaking changes at the top and grouping by typ or scope.
         */
        val defaultChangeLog = ChangeLogFormatter {
            appendLine(constants.header).appendLine()

            withType("release") {
                skip()
            }
            withBreakingChanges {
                appendLine(constants.breakingChange)
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
            groupBySorted({ constants.headerTexts[it.scope] ?: constants.headerTexts[it.type] }) {
                appendLine(groupKey)
                with({ constants.headerTexts.containsKey(it.scope) }) {
                    formatChanges {
                        append("- ").append(hash()).append(type()).appendLine(header())
                    }
                }
                formatChanges {
                    append("- ").append(hash()).append(scope()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine(constants.otherChange)
                formatChanges {
                    append("- ").append(hash()).appendLine(fullHeader())
                }
                appendLine()
            }
        }

        /**
         * Creates a simple change log only containing breaking changes, features and fixes
         */
        val simpleChangeLog = ChangeLogFormatter {
            appendLine(constants.header).appendLine()

            withBreakingChanges {
                appendLine(constants.breakingChange)
                formatChanges {
                    append("- ").appendLine(fullHeader())
                }
                appendLine()
            }
            withType("fix", "feat") {
                appendLine(constants.headerTexts[groupKey])
                formatChanges {
                    append("- ").append(scope()).appendLine(header())
                }
                appendLine()
            }
        }

        /**
         * Creates a change log grouping on type and then on scope
         */
        val scopeChangeLog = ChangeLogFormatter {
            appendLine(constants.header).appendLine()

            withType("release") {
                skip()
            }

            withBreakingChanges(formatGroupByScopeDisplayType(constants.breakingChange))
            groupBySorted({ constants.headerTexts[it.type] }, formatGroupBySope())
            otherwise (formatGroupByScopeDisplayType(constants.otherChange))
        }

        private fun formatGroupBySope(): ChangeLogBuilder.() -> Unit = {
            appendLine(groupKey).appendLine()
            groupByScope {
                append("#### ").appendLine(groupKey)
                formatChanges {
                    append("- ").append(hash()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine("#### Missing scope")
                formatChanges {
                    append("- ").append(hash()).appendLine(header())
                }
                appendLine()
            }
            appendLine()
        }

        private fun formatGroupByScopeDisplayType(header: String?): ChangeLogBuilder.() -> Unit = {
            appendLine(header).appendLine()
            groupByScope {
                append("#### ").appendLine(groupKey)
                formatChanges {
                    append("- ").append(hash()).append(type()).appendLine(header())
                }
                appendLine()
            }
            otherwise {
                appendLine("#### Missing scope")
                formatChanges {
                    append("- ").append(hash()).append(type()).appendLine(header())
                }
                appendLine()
            }
            appendLine()
        }
    }
}