package emerald.apps.fairychess

import android.app.Activity
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import emerald.apps.fairychess.view.MainActivity
import org.hamcrest.CoreMatchers.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


class Integrationtests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var mainActivity : MainActivity

    @Before
    fun before(){
        mainActivity = getActivity(activityRule)
        //use disableAnimationRule
    }

    @Test
    fun testShowUserStats(){
        onView(withId(R.id.tv_playerstats)).perform(click())
        onView(withId(R.id.aD_playerStats_playerName)).check(matches(isDisplayed()))
        onView(withId(R.id.aD_playerStats_ELO)).check(matches(isDisplayed()))
        onView(withId(R.id.aD_playerStats_gamesLost)).check(matches(isDisplayed()))
        onView(withId(R.id.aD_playerStats_gamesWon)).check(matches(isDisplayed()))
        onView(withId(R.id.aD_playerStats_gamesPlayed)).check(matches(isDisplayed()))
    }

    @Test
    fun testPlayOnlineGameResultAbortSearch(){
        helperStartNormalOnlineChessGame()
        var signal = CountDownLatch(1);
        signal.await(30, TimeUnit.SECONDS)
    }

    private fun helperStartNormalOnlineChessGame(){
        //select local game
        onView(withId(R.id.btn_createGame)).perform(click())
        //select game name parameter
        onView(withId(R.id.spinner_gameName)).perform(click())
        onView(withText("normal chess")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.spinner_gameName)).check(matches(withSpinnerText(containsString("normal chess"))))
        //select time mode parameter
        onView(withId(R.id.spinner_timemode)).perform(click())
        onView(withText("bullet (2 minutes)")).inRoot(isPlatformPopup()).perform(click())
        onView(withId(R.id.spinner_timemode)).check(matches(withSpinnerText(containsString("bullet (2 minutes)"))))
        //leave local game and check for success (btn_local is visible)
        //onView(withId(R.id.btn_create_game)).perform(click())
        onData(withText("create game")).inRoot(isDialog()).perform(click())
    }

    private fun helperMakeMove(sourceSquareString:String,targetSquareString:String){
        val sourceSquareId = mainActivity.resources.getIdentifier(sourceSquareString,"id",mainActivity.packageName)
        val targetSquareId = mainActivity.resources.getIdentifier(targetSquareString,"id",mainActivity.packageName)
        onView(withId(sourceSquareId)).perform(click())
        onView(withId(targetSquareId)).perform(click())
    }

    fun <T : Activity?> getActivity(activityScenarioRule: ActivityScenarioRule<T>): T {
        val activityRef: AtomicReference<T> = AtomicReference()
        activityScenarioRule.scenario.onActivity(activityRef::set)
        return activityRef.get()
    }
}