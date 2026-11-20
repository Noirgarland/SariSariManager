package com.example.sarisari.manager.core.navigation

import com.example.sarisari.manager.R
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** Validates: Requirements 6.6, 7.4, 7.5, 9.7 */
class TopLevelDestinationsTest {

    private val strings: Map<String, String> by lazy {
        val xml = listOf(
            File("src/main/res/values/strings.xml"),
            File("app/src/main/res/values/strings.xml")
        ).first { it.exists() }.readText()
        Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(xml).associate { it.groupValues[1] to it.groupValues[2] }
    }

    private val expectedLabelKeys = listOf(
        "destination_dashboard", "destination_cashier", "destination_products",
        "destination_reports", "destination_more"
    )

    @Test
    fun hasExactlyFiveItems() {
        assertEquals(5, topLevelDestinations.size)
    }

    @Test
    fun labelResourcesAreInRequiredOrder() {
        assertEquals(
            listOf(
                R.string.destination_dashboard, R.string.destination_cashier,
                R.string.destination_products,
                R.string.destination_reports, R.string.destination_more
            ),
            topLevelDestinations.map { it.labelRes }
        )
        val labels = expectedLabelKeys.map { strings.getValue(it) }
        assertEquals(listOf("Home", "Sell", "Products", "Reports", "More"), labels)
    }

    @Test
    fun routesAreInRequiredOrder() {
        assertEquals(
            listOf<Any>(DashboardRoute, CashierRoute, ProductsRoute, ReportsRoute, MoreRoute),
            topLevelDestinations.map { it.route }
        )
    }

    @Test
    fun noDuplicateRoutesOrLabels() {
        val routeTypes = topLevelDestinations.map { it.route::class }
        assertEquals(routeTypes.size, routeTypes.toSet().size)
        val labelRes = topLevelDestinations.map { it.labelRes }
        assertEquals(labelRes.size, labelRes.toSet().size)
        val labels = expectedLabelKeys.map { strings.getValue(it) }
        assertEquals(labels.size, labels.toSet().size)
    }
}
