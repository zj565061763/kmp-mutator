package com.sd.demo.kmp.mutator

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sd.demo.kmp.mutator.navigation.AppRoute
import com.sd.demo.kmp.mutator.navigation.RouteHome
import com.sd.demo.kmp.mutator.navigation.RouteSample

@Composable
fun App() {
  MaterialTheme {
    val navController = rememberNavController()
    NavHost(
      navController = navController,
      startDestination = AppRoute.Home,
    ) {
      composable<AppRoute.Home> {
        RouteHome(
          onClickSample = { navController.navigate(AppRoute.Sample) },
        )
      }
      composable<AppRoute.Sample> {
        RouteSample(
          onClickBack = { navController.popBackStack() }
        )
      }
    }
  }
}

expect fun logMsg(tag: String = "kmp-mutator", block: () -> String)