package com.passion.knownow.core.parsing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicIndexTest {

    private val index = """
        ---
        title: Brazil Enterprise Architecture
        locale: en-US
        ---

        # Brazil Enterprise Architecture

        - [Enterprise Architecture](https://raw.githubusercontent.com/ServiceNow/ServiceNowDocs/brazil/markdown/ea/index.md) -- The ServiceNow Enterprise Architecture unites strategic and operational teams, enabling organizations to achieve their business objectives.
          - [Workspace](https://raw.githubusercontent.com/ServiceNow/ServiceNowDocs/brazil/markdown/ea/workspace.md) -- An interactive interface for enterprise architects.
            - [Insights](https://raw.githubusercontent.com/ServiceNow/ServiceNowDocs/brazil/markdown/ea/insights.md) -- Views the insights of a portfolio.
          - [Setup](https://raw.githubusercontent.com/ServiceNow/ServiceNowDocs/brazil/markdown/ea/setup.md) -- Configure the workspace.
    """.trimIndent()

    @Test
    fun readsTitleAndSummary() {
        val preview = TopicIndexParser.parse("ea", index)
        assertEquals("Brazil Enterprise Architecture", preview.title)
        assertTrue(preview.summary!!.startsWith("The ServiceNow Enterprise Architecture"))
        assertEquals(4, preview.pageCount)
    }

    @Test
    fun tracksHierarchy() {
        val preview = TopicIndexParser.parse("ea", index)
        val insights = preview.entries.first { it.title == "Insights" }
        assertEquals(2, insights.depth)
        // closest parent first
        assertEquals(listOf("Workspace", "Enterprise Architecture"), insights.ancestors)
        assertEquals("insights.md", insights.path)
    }

    @Test
    fun resolvesPublishedSiteLinks() {
        assertEquals(
            "application-portfolio-management" to "eaw-app-service-form.md",
            DocNormalizer.parseReference(
                "https://www.servicenow.com/docs/r/application-portfolio-management/eaw-app-service-form.html"
            ),
        )
    }

    @Test
    fun rewritesSiteLinksToLocalScheme() {
        val body = "See [the form](https://www.servicenow.com/docs/r/ea/eaw-app-service-form.html)."
        val out = DocNormalizer.normalize(body, null, "ea", "index.md")
        assertTrue(out.contains("knn://doc/ea/eaw-app-service-form.md"))
        assertFalse(out.contains("servicenow.com"))
    }

    @Test
    fun leavesUnrelatedLinksAlone() {
        val body = "Ask in the [community](https://www.servicenow.com/community/ea/ct-p/ea)."
        val out = DocNormalizer.normalize(body, null, "ea", "index.md")
        assertTrue(out.contains("https://www.servicenow.com/community/ea/ct-p/ea"))
    }

    @Test
    fun separatesGluedBoldLabelFromLink() {
        val body = "**Parent Topic:**[Workspace](https://raw.githubusercontent.com/x/y/brazil/markdown/ea/workspace.md)"
        val out = DocNormalizer.normalize(body, null, "ea", "index.md")
        assertTrue(out.contains("**Parent Topic:** [Workspace]"))
    }
}
