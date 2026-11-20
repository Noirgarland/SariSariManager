package com.example.sarisari.manager.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.testing.TestInstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/** Test dispatchers shared between the replacement module and the assertions. */
@OptIn(ExperimentalCoroutinesApi::class)
object TestDispatchers {
    val io: CoroutineDispatcher = StandardTestDispatcher()
    val default: CoroutineDispatcher = UnconfinedTestDispatcher()
    val main: CoroutineDispatcher = StandardTestDispatcher()
}

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatcherModule::class],
)
object TestDispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = TestDispatchers.io

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = TestDispatchers.default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = TestDispatchers.main
}

/** Test-only holder that receives the three qualified dispatchers. */
class DispatcherHolder @Inject constructor(
    @IoDispatcher val io: CoroutineDispatcher,
    @DefaultDispatcher val default: CoroutineDispatcher,
    @MainDispatcher val main: CoroutineDispatcher,
)

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DispatcherModuleReplacementTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var holder: DispatcherHolder

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun holderReceivesReplacementDispatchers() {
        assertSame(TestDispatchers.io, holder.io)
        assertSame(TestDispatchers.default, holder.default)
        assertSame(TestDispatchers.main, holder.main)
    }
}
